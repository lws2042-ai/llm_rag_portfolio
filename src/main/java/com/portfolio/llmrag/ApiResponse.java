package com.portfolio.llmrag;

public record ApiResponse(
    String status,
    String message,
    String version
) {
}
