package com.portfolio.llmrag.controller;

import com.portfolio.llmrag.dto.ApiResponse;
import com.portfolio.llmrag.exception.DocumentNotFoundException;
import com.portfolio.llmrag.exception.InvalidDocumentStateException;
import com.portfolio.llmrag.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.io.IOException;

/**
 * 모든 API 예외를 ApiResponse 형식으로 바꿔 응답한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ApiResponse<Void>> invalidFile(InvalidFileException e) {
        return respond(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> notFound(DocumentNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(InvalidDocumentStateException.class)
    public ResponseEntity<ApiResponse<Void>> invalidState(InvalidDocumentStateException e) {
        return respond(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Void>> missingFile(MissingServletRequestPartException e) {
        return respond(HttpStatus.BAD_REQUEST, "업로드할 파일(file)이 없습니다.");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge(MaxUploadSizeExceededException e) {
        return respond(HttpStatus.CONTENT_TOO_LARGE, "파일은 20MB까지 업로드할 수 있습니다.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> typeMismatch(MethodArgumentTypeMismatchException e) {
        return respond(HttpStatus.BAD_REQUEST, "잘못된 요청 값입니다: " + e.getName());
    }

    @ExceptionHandler(IOException.class)
    public ResponseEntity<ApiResponse<Void>> ioError(IOException e) {
        log.error("파일 처리 오류", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "파일 처리에 실패했습니다.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception e) {
        // 없는 주소(404), 잘못된 메서드(405) 등 Spring 기본 예외는 원래 상태 코드를 유지한다.
        if (e instanceof ErrorResponse errorResponse) {
            HttpStatusCode status = errorResponse.getStatusCode();
            return ResponseEntity.status(status).body(ApiResponse.error(e.getMessage()));
        }
        log.error("처리되지 않은 오류", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다.");
    }

    private ResponseEntity<ApiResponse<Void>> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(message));
    }
}
