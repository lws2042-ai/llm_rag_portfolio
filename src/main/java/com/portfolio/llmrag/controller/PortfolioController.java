package com.portfolio.llmrag.controller;

import com.portfolio.llmrag.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PortfolioController {
    
    @GetMapping("/api/hello")
    public String hello() {
        return "LLM RAG Portfolio API is running!";
    }

    @GetMapping("/api/status")
    public ApiResponse status() {
        return new ApiResponse(
            "success",
            "LLM RAG Portfolio API is running!",
            "1.0.0"
        );
    }
}
