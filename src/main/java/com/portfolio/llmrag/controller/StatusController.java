package com.portfolio.llmrag.controller;

import com.portfolio.llmrag.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 서버 동작 확인용 API.
 */
@RestController
public class StatusController {

    private static final String VERSION = "1.1.0";

    @GetMapping("/api/hello")
    public String hello() {
        return "LLM RAG Portfolio API is running!";
    }

    @GetMapping("/api/status")
    public ApiResponse<String> status() {
        return ApiResponse.ok("LLM RAG Portfolio API is running!", VERSION);
    }
}
