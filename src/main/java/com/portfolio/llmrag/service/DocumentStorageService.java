package com.portfolio.llmrag.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.portfolio.llmrag.service.StoredFileInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import java.nio.file.LinkOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@Service
public class DocumentStorageService {

    private final Path uploadDir = Paths.get("uploads").toAbsolutePath().normalize();

    public String store(MultipartFile file) throws IOException {
        Files.createDirectories(uploadDir);

        String originalName = file.getOriginalFilename();
        String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();

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