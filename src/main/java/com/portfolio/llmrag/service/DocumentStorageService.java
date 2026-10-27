package com.portfolio.llmrag.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class DocumentStorageService {

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(uploadDir);

        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(
                originalName.lastIndexOf(".")
        ).toLowerCase();

        String storedName = UUID.randomUUID() + extension;
        Path target = uploadDir.resolve(storedName).normalize();

        if (!target.getParent().equals(uploadDir)) {
            throw new IOException("잘못된 파일 저장 경로입니다.");
        }

        Files.copy(
                file.getInputStream(),
                target,
                StandardCopyOption.REPLACE_EXISTING
        );

        return storedName;
    }
}