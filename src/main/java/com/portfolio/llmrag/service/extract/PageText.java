package com.portfolio.llmrag.service.extract;

/**
 * 추출된 한 페이지의 텍스트.
 * 페이지 번호는 1부터 시작하며, 나중에 청크와 답변 근거("3쪽")에 그대로 이어진다.
 */
public record PageText(
        int pageNumber,
        String text
) {
}
