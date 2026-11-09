package com.portfolio.llmrag.dto;

/**
 * 청크 조회 응답.
 */
public record ChunkDto(
        int chunkIndex,
        int pageNumber,    // 출처 페이지
        int charCount,
        boolean embedded,  // 임베딩 벡터가 저장됐는지
        String content
) {
}
