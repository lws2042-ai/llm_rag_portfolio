package com.portfolio.llmrag.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;



import com.portfolio.llmrag.service.DocumentStorageService;

import java.io.IOException;
import java.util.Map;

@RestController 
public class DocumentUploadController {
    private final DocumentStorageService storageService;


    public DocumentUploadController(DocumentStorageService storageService) {
            this.storageService = storageService;
        }

    @PostMapping("/api/documents/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "빈 파일은 업로드할 수 없습니다."
                ));
            }

            String originalName = file.getOriginalFilename();

            if (originalName == null || !(originalName.toLowerCase().endsWith("pdf") 
                || originalName.toLowerCase().endsWith(".txt"))) {
                return ResponseEntity.badRequest().body(Map.of(
                    
                    "status", "error",
                    "message", "PDF 또는 파일만 업로드할 수 있습니다."
                ));
            }
            
            // return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of(
            //         "status", "pending",
            //         "message", "파일 검증 성공. 다음 단계에서 자장 기능을 구현합니다."

            // ));
            
            try {
                String storedName = storageService.store(file);

                return ResponseEntity.ok(Map.of(
                        "status", "success",
                        "message", "파일 업로드 성공",
                        "fileName", storedName
                ));
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(Map.of(
                        "status", "error",
                        "message", "파일 저장에 실패했습니다."
                ));
            }
        }
}

    

