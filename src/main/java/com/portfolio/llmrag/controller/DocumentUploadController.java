package com.portfolio.llmrag.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;



import com.portfolio.llmrag.service.DocumentStorageService;
import com.portfolio.llmrag.service.StoredFileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@RestController 
public class DocumentUploadController {
    private final DocumentStorageService storageService;

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

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



public List<StoredFileInfo> listStoredFiles() throws IOException {

        List<StoredFileInfo> result = new ArrayList<>();

        // 업로드 디렉터리가 없으면 빈 목록 반환
        if (!Files.isDirectory(uploadDir)) {
            return result;
        }

        // 업로드 디렉터리 바로 아래의 파일만 조회
        try (Stream<Path> paths = Files.list(uploadDir)) {

            paths.filter(path ->
                        Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)
                    )
                    .filter(path -> {
                        String name = path.getFileName()
                                        .toString()
                                        .toLowerCase(Locale.ROOT);

                        return name.endsWith(".pdf")
                                || name.endsWith(".txt");
                    })
                    .forEach(path -> {
                        try {
                            String storedName = path.getFileName().toString();

                            String extension = storedName.substring(
                                    storedName.lastIndexOf(".")
                            ).toLowerCase(Locale.ROOT);

                            long sizeBytes = Files.size(path);

                            FileTime modifiedTime = Files.getLastModifiedTime(
                                    path,
                                    LinkOption.NOFOLLOW_LINKS
                            );

                            Instant lastModified = modifiedTime.toInstant();

                            result.add(new StoredFileInfo(
                                    storedName,
                                    extension,
                                    sizeBytes,
                                    lastModified
                            ));

                        } catch (IOException e) {
                            throw new java.io.UncheckedIOException(e);
                        }
                    });
        } catch (java.io.UncheckedIOException e) {
            throw e.getCause();
        }

        // 최근 수정된 파일을 먼저 표시
        result.sort(
                Comparator.comparing(StoredFileInfo::lastModified).reversed()
        );

        return result;
    }



}

    

