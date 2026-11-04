package com.portfolio.llmrag.service.chunk;

/**
 * 검색 단위가 되는 텍스트 조각.
 *
 * @param chunkIndex 문서 안에서의 순서 (0부터)
 * @param pageNumber 청크가 나온 페이지. 청크는 페이지를 넘지 않으므로 답변 근거("3쪽")로 그대로 쓴다.
 * @param content    청크 본문
 */
public record TextChunk(
        int chunkIndex,
        int pageNumber,
        String content
) {
}
