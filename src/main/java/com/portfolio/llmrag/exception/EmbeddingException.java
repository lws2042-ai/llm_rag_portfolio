package com.portfolio.llmrag.exception;

/**
 * 임베딩을 만들지 못했을 때 (임베딩 서버 연결 실패, 차원 불일치 등).
 * API 로 다시 시도한 요청이 실패하면 503 으로 응답한다.
 */
public class EmbeddingException extends RuntimeException {

    public EmbeddingException(String message) {
        super(message);
    }

    public EmbeddingException(String message, Throwable cause) {
        super(message, cause);
    }
}
