package com.portfolio.llmrag.service;

import com.portfolio.llmrag.config.StorageProperties;
import com.portfolio.llmrag.exception.InvalidFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 업로드 파일 검증과 디스크 저장을 담당한다.
 * 파일 정보(원본 파일명 등)는 DB 에, 파일 자체는 디스크에 UUID 이름으로 저장한다.
 */
@Service
public class DocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(DocumentStorageService.class);

    /** 허용 확장자. HWP 등을 추가할 때는 여기와 TextExtractor 구현만 늘리면 된다. */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".pdf", ".txt");
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final Path uploadDir;

    public DocumentStorageService(StorageProperties properties) {
        this.uploadDir = Paths.get(properties.uploadDir()).toAbsolutePath().normalize();
    }

    /** 검증을 통과한 파일의 원본 이름과 확장자 */
    public record ValidatedFile(String originalName, String extension) {
    }

    /**
     * 빈 파일, 파일명, 확장자, PDF 실제 형식을 검사한다.
     */
    public ValidatedFile validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("빈 파일은 업로드할 수 없습니다.");
        }

        String originalName = cleanOriginalName(file.getOriginalFilename());
        String extension = extractExtension(originalName);

        // 확장자만 .pdf 로 바꾼 파일을 거르기 위해 파일 앞부분(%PDF-)을 확인한다.
        if (".pdf".equals(extension) && !hasPdfSignature(file)) {
            throw new InvalidFileException("PDF 형식이 아닌 파일입니다. 확장자만 바뀐 파일인지 확인해 주세요.");
        }
        return new ValidatedFile(originalName, extension);
    }

    /**
     * 파일을 UUID 이름으로 저장하고 저장된 이름을 반환한다.
     */
    public String store(MultipartFile file, String extension) throws IOException {
        Files.createDirectories(uploadDir);

        String storedName = UUID.randomUUID() + extension;
        Path target = resolve(storedName);

        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target);
        }
        return storedName;
    }

    /**
     * 저장된 파일 이름을 실제 경로로 바꾼다. 업로드 폴더 밖을 가리키면 거부한다.
     */
    public Path resolve(String storedName) {
        Path target = uploadDir.resolve(storedName).normalize();
        if (!uploadDir.equals(target.getParent())) {
            throw new IllegalStateException("잘못된 파일 저장 경로입니다.");
        }
        return target;
    }

    /**
     * DB 저장 실패 등으로 남은 파일을 정리한다. 실패해도 예외를 던지지 않는다.
     */
    public void deleteQuietly(String storedName) {
        try {
            Files.deleteIfExists(resolve(storedName));
        } catch (IOException | RuntimeException e) {
            log.warn("업로드 파일 정리 실패: {}", storedName, e);
        }
    }

    private String cleanOriginalName(String rawName) {
        // 일부 브라우저는 "C:\Users\...\a.pdf" 처럼 전체 경로를 보내므로 파일명만 남긴다.
        String name = StringUtils.getFilename(StringUtils.cleanPath(rawName == null ? "" : rawName));
        if (!StringUtils.hasText(name)) {
            throw new InvalidFileException("파일 이름이 없습니다.");
        }
        name = name.strip();
        if (name.length() > MAX_FILE_NAME_LENGTH) {
            throw new InvalidFileException("파일 이름은 " + MAX_FILE_NAME_LENGTH + "자 이하여야 합니다.");
        }
        return name;
    }

    private String extractExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            throw new InvalidFileException("확장자가 없는 파일은 업로드할 수 없습니다.");
        }
        String extension = fileName.substring(dot).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidFileException("PDF 또는 TXT 파일만 업로드할 수 있습니다.");
        }
        return extension;
    }

    private boolean hasPdfSignature(MultipartFile file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(PDF_SIGNATURE.length);
            return Arrays.equals(head, PDF_SIGNATURE);
        }
    }
}
