package com.portfolio.llmrag.service;

import com.portfolio.llmrag.domain.Document;
import com.portfolio.llmrag.domain.DocumentStatus;
import com.portfolio.llmrag.dto.ChunkDto;
import com.portfolio.llmrag.dto.DocumentDto;
import com.portfolio.llmrag.dto.PageDetailDto;
import com.portfolio.llmrag.dto.PageSummaryDto;
import com.portfolio.llmrag.exception.DocumentNotFoundException;
import com.portfolio.llmrag.exception.InvalidDocumentStateException;
import com.portfolio.llmrag.exception.TextExtractionException;
import com.portfolio.llmrag.repository.DocumentChunkRepository;
import com.portfolio.llmrag.repository.DocumentPageRepository;
import com.portfolio.llmrag.repository.DocumentRepository;
import com.portfolio.llmrag.service.DocumentStorageService.ValidatedFile;
import com.portfolio.llmrag.service.chunk.TextChunk;
import com.portfolio.llmrag.service.chunk.TextChunker;
import com.portfolio.llmrag.service.extract.PageText;
import com.portfolio.llmrag.service.extract.TextExtractionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 문서 처리 흐름:
 * 검증 → 파일 저장 → DB 등록(UPLOADED) → 텍스트 추출(EXTRACTED) → 청크 분할(CHUNKED)
 *
 * 추출에 실패해도 문서 행은 남기고 FAILED 와 사유를 기록한다.
 * 그래야 사용자가 "왜 안 됐는지" 확인할 수 있다.
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
    private final TransactionTemplate transaction;

    public DocumentService(DocumentStorageService storage,
                           TextExtractionService extraction,
                           DocumentRepository documents,
                           DocumentPageRepository pages,
                           DocumentChunkRepository chunks,
                           TextChunker chunker,
                           TransactionTemplate transaction) {
        this.storage = storage;
        this.extraction = extraction;
        this.documents = documents;
        this.pages = pages;
        this.chunks = chunks;
        this.chunker = chunker;
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

        if (extractPages(documentId, storedName, validated.extension())) {
            try {
                chunkPages(documentId);
            } catch (DataAccessException e) {
                log.error("청크 저장 실패: id={}", documentId, e);
                documents.markFailed(documentId, "청크를 저장하지 못했습니다.");
            }
        }
        return getDocument(documentId);
    }

    /**
     * 저장된 페이지 텍스트로 청크를 다시 만든다. (PDF 를 다시 읽지 않는다)
     * 청크 크기 설정을 바꿨거나, 3단계 이전에 올린 문서를 청크로 나눌 때 쓴다.
     */
    public DocumentDto rechunk(long id) {
        Document document = findDocument(id);
        if (document.status() != DocumentStatus.EXTRACTED && document.status() != DocumentStatus.CHUNKED) {
            throw new InvalidDocumentStateException(
                    id + "번 문서는 텍스트가 추출되지 않아 청크를 만들 수 없습니다. (상태: " + document.status() + ")");
        }
        chunkPages(id);
        return getDocument(id);
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

    private Document findDocument(long id) {
        return documents.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id + "번 문서를 찾을 수 없습니다."));
    }
}
