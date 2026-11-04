package com.portfolio.llmrag.domain;

import java.time.LocalDateTime;

/**
 * documents 테이블의 한 행.
 * storedName 처럼 외부에 노출하지 않을 값도 포함하므로 API 응답에는 DocumentDto 를 사용한다.
 */
public record Document(
        Long id,
        String originalName,
        String storedName,
        String extension,
        long sizeBytes,
        DocumentStatus status,
        Integer pageCount,
        Integer chunkCount,
        String failReason,
        LocalDateTime uploadedAt,
        LocalDateTime updatedAt
) {
}
