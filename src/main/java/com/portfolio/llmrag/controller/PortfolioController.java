package com.portfolio.llmrag.controller;

import com.portfolio.llmrag.ApiResponse;
import com.portfolio.llmrag.DocumentDto;

import java.util.List;

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

    @GetMapping("/api/documents")
    public List<DocumentDto> getDocuments() {
        return List.of(
            new DocumentDto(1L, "RAG 개념 정리", "PDF"),
            new DocumentDto(2L, "Spring Boot 개발 가이드", "PDF"),
            new DocumentDto(3L, "LLM 기초 학습 노트", "TXT")

        );
    }
}
