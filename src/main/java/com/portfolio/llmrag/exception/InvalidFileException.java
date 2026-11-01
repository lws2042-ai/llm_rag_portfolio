package com.portfolio.llmrag.exception;

/**
 * 업로드 파일이 허용 조건에 맞지 않을 때 (400).
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}
