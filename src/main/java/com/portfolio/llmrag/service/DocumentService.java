package com.portfolio.llmrag.service;

import com.portfolio.llmrag.domain.Document;
import com.portfolio.llmrag.domain.DocumentStatus;
import com.portfolio.llmrag.dto.ChunkDto;
import com.portfolio.llmrag.dto.DocumentDto;
import com.portfolio.llmrag.dto.PageDetailDto;
import com.portfolio.llmrag.dto.PageSummaryDto;
import com.portfolio.llmrag.exception.DocumentNotFoundException;
import com.portfolio.llmrag.exception.EmbeddingException;
import com.portfolio.llmrag.exception.InvalidDocumentStateException;
import com.portfolio.llmrag.exception.TextExtractionException;
import com.portfolio.llmrag.repository.DocumentChunkRepository;
import com.portfolio.llmrag.repository.DocumentChunkRepository.ChunkText;
import com.portfolio.llmrag.repository.DocumentPageRepository;
import com.portfolio.llmrag.repository.DocumentRepository;
import com.portfolio.llmrag.service.DocumentStorageService.ValidatedFile;
import com.portfolio.llmrag.service.chunk.TextChunk;
import com.portfolio.llmrag.service.chunk.TextChunker;
import com.portfolio.llmrag.service.embed.ChunkEmbedder;
import com.portfolio.llmrag.service.extract.PageText;
import com.portfolio.llmrag.service.extract.TextExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.EnumSet;
import java.util.List;

/**
 * 문서 처리 흐름:
 * 검증 → 파일 저장 → DB 등록(UPLOADED) → 텍스트 추출(EXTRACTED) → 청크 분할(CHUNKED) → 임베딩(INDEXED)
 *
 * 추출에 실패해도 문서 행은 남기고 FAILED 와 사유를 기록한다.
 * 그래야 사용자가 "왜 안 됐는지" 확인할 수 있다.
 *
 * 임베딩 실패(예: Ollama 가 꺼져 있음)는 문서 문제가 아니므로 CHUNKED 로 남기고 사유만 기록한다.
 * 임베딩 서버를 켠 뒤 POST /api/documents/{id}/embeddings 로 다시 시도할 수 있다.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentStorageService storage;
    private final TextExtractionService extraction;
    private final DocumentRepository documents;
    private final DocumentPageRepository pages;
    private final DocumentChunkRepository chunks;
    private final TextChunker chunker;
    private final ChunkEmbedder embedder;
    private final TransactionTemplate transaction;

    public DocumentService(DocumentStorageService storage,
                           TextExtractionService extraction,
                           DocumentRepository documents,
                           DocumentPageRepository pages,
                           DocumentChunkRepository chunks,
                           TextChunker chunker,
                           ChunkEmbedder embedder,
                           TransactionTemplate transaction) {
        this.storage = storage;
        this.extraction = extraction;
        this.documents = documents;
        this.pages = pages;
        this.chunks = chunks;
        this.chunker = chunker;
        this.embedder = embedder;
        this.transaction = transaction;
    }

    public DocumentDto upload(MultipartFile file) throws IOException {
        ValidatedFile validated = storage.validate(file);
        String storedName = storage.store(file, validated.extension());

        long documentId;
        try {
            documentId = documents.insert(
                    validated.originalName(), storedName, validated.extension(), file.getSize());
        } catch (RuntimeException e) {
            storage.deleteQuietly(storedName); // DB 저장 실패 시 고아 파일이 남지 않게 정리
            throw e;
        }
        log.info("문서 등록: id={}, name={}", documentId, validated.originalName());

        if (extractPages(documentId, storedName, validated.extension()) && chunkPagesSafely(documentId)) {
            embedChunks(documentId);
        }
        return getDocument(documentId);
    }

    /**
     * 저장된 페이지 텍스트로 청크를 다시 만들고 임베딩까지 한다. (PDF 를 다시 읽지 않는다)
     * 청크 크기 설정을 바꿨거나, 이전 단계에서 올린 문서를 다시 처리할 때 쓴다.
     */
    public DocumentDto rechunk(long id) {
        Document document = findDocument(id);
        if (!EnumSet.of(DocumentStatus.EXTRACTED, DocumentStatus.CHUNKED, DocumentStatus.INDEXED)
                .contains(document.status())) {
            throw new InvalidDocumentStateException(
                    id + "번 문서는 텍스트가 추출되지 않아 청크를 만들 수 없습니다. (상태: " + document.status() + ")");
        }
        chunkPages(id);
        embedChunks(id);
        return getDocument(id);
    }

    /**
     * 기존 청크로 임베딩만 다시 만든다.
     * 업로드 때 임베딩 서버가 꺼져 있었거나, 임베딩 모델을 바꿨을 때 쓴다.
     *
     * @throws EmbeddingException 이번에도 임베딩에 실패한 경우 (503)
     */
    public DocumentDto reembed(long id) {
        Document document = findDocument(id);
        if (document.status() != DocumentStatus.CHUNKED && document.status() != DocumentStatus.INDEXED) {
            throw new InvalidDocumentStateException(
                    id + "번 문서는 청크가 없어 임베딩할 수 없습니다. (상태: " + document.status() + ")");
        }
        embedChunks(id);
        Document result = findDocument(id);
        if (result.status() != DocumentStatus.INDEXED) {
            throw new EmbeddingException(result.failReason());
        }
        return DocumentDto.from(result);
    }

    public List<ChunkDto> listChunks(long documentId) {
        findDocument(documentId);
        return chunks.findByDocument(documentId);
    }

    public List<DocumentDto> listDocuments() {
        return documents.findAll().stream()
                .map(DocumentDto::from)
                .toList();
    }

    public DocumentDto getDocument(long id) {
        return DocumentDto.from(findDocument(id));
    }

    public List<PageSummaryDto> listPages(long documentId) {
        findDocument(documentId);
        return pages.findSummaries(documentId);
    }

    public PageDetailDto getPage(long documentId, int pageNumber) {
        findDocument(documentId);
        String content = pages.findContent(documentId, pageNumber)
                .orElseThrow(() -> new DocumentNotFoundException(
                        documentId + "번 문서의 " + pageNumber + "쪽에 추출된 텍스트가 없습니다."));
        return new PageDetailDto(documentId, pageNumber, content);
    }

    /** @return 추출과 저장에 성공했으면 true */
    private boolean extractPages(long documentId, String storedName, String extension) {
        try {
            List<PageText> extracted = extraction.extract(storage.resolve(storedName), extension);

            // 페이지 저장과 상태 변경은 함께 성공하거나 함께 실패해야 한다.
            transaction.executeWithoutResult(status -> {
                pages.saveAll(documentId, extracted);
                documents.markExtracted(documentId, extracted.size());
            });
            log.info("텍스트 추출 완료: id={}, pages={}", documentId, extracted.size());
            return true;

        } catch (TextExtractionException e) {
            log.warn("텍스트 추출 실패: id={}, reason={}", documentId, e.getMessage());
            documents.markFailed(documentId, e.getMessage());

        } catch (DataAccessException e) {
            log.error("추출 텍스트 저장 실패: id={}", documentId, e);
            documents.markFailed(documentId, "추출한 텍스트를 저장하지 못했습니다.");
        }
        return false;
    }

    private void chunkPages(long documentId) {
        List<TextChunk> created = chunker.chunk(pages.findAll(documentId));

        // 기존 청크 교체와 상태 변경은 함께 성공하거나 함께 실패해야 한다.
        transaction.executeWithoutResult(status -> {
            chunks.deleteByDocument(documentId);
            chunks.saveAll(documentId, created);
            documents.markChunked(documentId, created.size());
        });
        log.info("청크 분할 완료: id={}, chunks={}", documentId, created.size());
    }

    /** @return 청크 저장에 성공했으면 true */
    private boolean chunkPagesSafely(long documentId) {
        try {
            chunkPages(documentId);
            return true;
        } catch (DataAccessException e) {
            log.error("청크 저장 실패: id={}", documentId, e);
            documents.markFailed(documentId, "청크를 저장하지 못했습니다.");
            return false;
        }
    }

    /**
     * 청크마다 벡터를 만들어 저장하고 INDEXED 로 바꾼다.
     * 실패하면 CHUNKED 로 두고 사유만 기록한다.
     * 임베딩은 네트워크 호출이라 오래 걸릴 수 있으므로 트랜잭션 밖에서 하고, 저장만 트랜잭션으로 묶는다.
     */
    private void embedChunks(long documentId) {
        List<ChunkText> targets = chunks.findTexts(documentId);
        try {
            List<float[]> vectors = embedder.embed(targets.stream().map(ChunkText::content).toList());
            List<Long> ids = targets.stream().map(ChunkText::id).toList();

            transaction.executeWithoutResult(status -> {
                chunks.saveEmbeddings(ids, vectors);
                documents.markIndexed(documentId);
            });
            log.info("임베딩 완료: id={}, chunks={}", documentId, targets.size());

        } catch (EmbeddingException e) {
            log.warn("임베딩 실패: id={}, reason={}", documentId, e.getMessage());
            documents.recordFailReason(documentId, e.getMessage());

        } catch (DataAccessException e) {
            log.error("임베딩 벡터 저장 실패: id={}", documentId, e);
            documents.recordFailReason(documentId, "임베딩 벡터를 저장하지 못했습니다.");
        }
    }

    private Document findDocument(long id) {
        return documents.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id + "번 문서를 찾을 수 없습니다."));
    }
}
