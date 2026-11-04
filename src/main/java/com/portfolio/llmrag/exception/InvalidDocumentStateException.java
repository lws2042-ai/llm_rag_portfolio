package com.portfolio.llmrag.exception;

/**
 * 문서 상태 때문에 요청한 작업을 할 수 없을 때 (409).
 * 예: 텍스트 추출에 실패한 문서의 청크를 다시 만들려고 할 때
 */
public class InvalidDocumentStateException extends RuntimeException {

    public InvalidDocumentStateException(String message) {
        super(message);
    }
}
