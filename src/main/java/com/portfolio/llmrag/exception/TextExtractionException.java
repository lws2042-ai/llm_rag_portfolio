package com.portfolio.llmrag.exception;

/**
 * 텍스트 추출 실패 (스캔본, 암호 PDF, 손상 파일, 빈 파일 등).
 * 메시지는 documents.fail_reason 에 그대로 저장되므로 사용자가 이해할 수 있게 작성한다.
 */
public class TextExtractionException extends RuntimeException {

    public TextExtractionException(String message) {
        super(message);
    }

    public TextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
