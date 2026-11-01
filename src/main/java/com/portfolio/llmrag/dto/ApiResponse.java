package com.portfolio.llmrag.dto;

/**
 * 모든 API가 공통으로 사용하는 응답 형식.
 *
 * @param status  "success" 또는 "error"
 * @param message 사용자에게 보여줄 메시지
 * @param data    실제 응답 데이터 (에러일 때는 null)
 */
public record ApiResponse<T>(
        String status,
        String message,
        T data
) {

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>("success", message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("error", message, null);
    }
}
