package com.portfolio.llmrag.service.extract;

import com.portfolio.llmrag.exception.TextExtractionException;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.List;

/**
 * 확장자에 맞는 TextExtractor 를 골라 실행한다.
 */
@Service
public class TextExtractionService {

    private final List<TextExtractor> extractors;

    public TextExtractionService(List<TextExtractor> extractors) {
        this.extractors = extractors;
    }

    public List<PageText> extract(Path path, String extension) {
        return extractors.stream()
                .filter(extractor -> extractor.supports(extension))
                .findFirst()
                .orElseThrow(() -> new TextExtractionException("지원하지 않는 파일 형식입니다: " + extension))
                .extract(path);
    }
}
