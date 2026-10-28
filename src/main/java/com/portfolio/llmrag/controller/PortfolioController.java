package com.portfolio.llmrag.controller;

import com.portfolio.llmrag.ApiResponse;
import com.portfolio.llmrag.DocumentDto;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portfolio.llmrag.service.DocumentStorageService;
import com.portfolio.llmrag.service.StoredFileInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.util.List;

@RestController
public class PortfolioController {
    private final DocumentStorageService storageService;

    public PortfolioController(DocumentStorageService storageService) {
        this.storageService = storageService;
    }

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
    public ResponseEntity<List<StoredFileInfo>> getDocuments()
            throws IOException {

        List<StoredFileInfo> documents =
                storageService.listStoredFiles();

        return ResponseEntity.ok(documents);
    }
}
