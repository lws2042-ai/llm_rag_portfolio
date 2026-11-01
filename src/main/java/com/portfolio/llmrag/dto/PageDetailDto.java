package com.portfolio.llmrag.dto;

/**
 * 한 페이지의 추출 텍스트 전체.
 */
public record PageDetailDto(
        long documentId,
        int pageNumber,
        String content
) {
}
