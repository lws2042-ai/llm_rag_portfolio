package com.portfolio.llmrag.exception;

/**
 * 요청한 문서나 페이지가 없을 때 (404).
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(String message) {
        super(message);
    }
}
