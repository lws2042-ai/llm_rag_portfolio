package com.portfolio.llmrag.controller;

import com.portfolio.llmrag.domain.DocumentStatus;
import com.portfolio.llmrag.dto.ApiResponse;
import com.portfolio.llmrag.dto.ChunkDto;
import com.portfolio.llmrag.dto.DocumentDto;
import com.portfolio.llmrag.dto.PageDetailDto;
import com.portfolio.llmrag.dto.PageSummaryDto;
import com.portfolio.llmrag.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 문서 업로드·조회 API.
 * 예외는 GlobalExceptionHandler 에서 공통 처리한다.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** 업로드 + 페이지별 텍스트 추출 */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DocumentDto>> upload(@RequestParam("file") MultipartFile file)
            throws IOException {
        DocumentDto document = documentService.upload(file);

        String message = document.status() == DocumentStatus.CHUNKED
                ? "업로드, 텍스트 추출, 청크 분할이 완료되었습니다. ("
                        + document.pageCount() + "쪽, 청크 " + document.chunkCount() + "개)"
                : "업로드는 되었지만 처리에 실패했습니다: " + document.failReason();

        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(message, document));
    }

    /** 문서 목록 (최근 업로드 순) */
    @GetMapping
    public ApiResponse<List<DocumentDto>> list() {
        List<DocumentDto> documents = documentService.listDocuments();
        return ApiResponse.ok("문서 " + documents.size() + "건", documents);
    }

    /** 문서 한 건 */
    @GetMapping("/{id}")
    public ApiResponse<DocumentDto> get(@PathVariable("id") long id) {
        return ApiResponse.ok("조회 성공", documentService.getDocument(id));
    }

    /** 페이지 목록 (앞부분 미리보기) */
    @GetMapping("/{id}/pages")
    public ApiResponse<List<PageSummaryDto>> pages(@PathVariable("id") long id) {
        List<PageSummaryDto> pages = documentService.listPages(id);
        return ApiResponse.ok(pages.size() + "쪽", pages);
    }

    /** 한 페이지의 추출 텍스트 전체 */
    @GetMapping("/{id}/pages/{pageNumber}")
    public ApiResponse<PageDetailDto> page(@PathVariable("id") long id,
                                           @PathVariable("pageNumber") int pageNumber) {
        return ApiResponse.ok("조회 성공", documentService.getPage(id, pageNumber));
    }

    /** 청크 목록 (본문 포함) */
    @GetMapping("/{id}/chunks")
    public ApiResponse<List<ChunkDto>> chunks(@PathVariable("id") long id) {
        List<ChunkDto> chunks = documentService.listChunks(id);
        return ApiResponse.ok("청크 " + chunks.size() + "개", chunks);
    }

    /** 저장된 페이지 텍스트로 청크를 다시 만든다 */
    @PostMapping("/{id}/chunks")
    public ApiResponse<DocumentDto> rechunk(@PathVariable("id") long id) {
        DocumentDto document = documentService.rechunk(id);
        return ApiResponse.ok("청크를 다시 만들었습니다. (청크 " + document.chunkCount() + "개)", document);
    }
}
