package com.portfolio.llmrag.dto;

import com.portfolio.llmrag.domain.Document;
import com.portfolio.llmrag.domain.DocumentStatus;

import java.time.LocalDateTime;

/**
 * 문서 정보 API 응답.
 */
public record DocumentDto(
        Long id,
        String originalName,   // 원본 파일명 (답변 근거 표시용)
        String extension,      // .pdf, .txt
        long sizeBytes,
        DocumentStatus status, // UPLOADED, EXTRACTED, CHUNKED, INDEXED, FAILED
        Integer pageCount,     // 텍스트가 추출된 페이지 수
        Integer chunkCount,    // 분할된 청크 수
        String failReason,     // 실패 사유 (FAILED, 또는 임베딩에 실패한 CHUNKED)
        LocalDateTime uploadedAt
) {

    public static DocumentDto from(Document d) {
        return new DocumentDto(
                d.id(),
                d.originalName(),
                d.extension(),
                d.sizeBytes(),
                d.status(),
                d.pageCount(),
                d.chunkCount(),
                d.failReason(),
                d.uploadedAt()
        );
    }
}
