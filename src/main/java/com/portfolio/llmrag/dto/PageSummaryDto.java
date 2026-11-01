package com.portfolio.llmrag.dto;

/**
 * 페이지 목록 응답 (본문 전체 대신 앞부분 미리보기만).
 */
public record PageSummaryDto(
        int pageNumber,
        int charCount,
        String preview
) {
}
