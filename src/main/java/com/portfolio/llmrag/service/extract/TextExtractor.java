package com.portfolio.llmrag.service.extract;

import com.portfolio.llmrag.exception.TextExtractionException;

import java.nio.file.Path;
import java.util.List;

/**
 * 파일 형식별 텍스트 추출기.
 * 새 형식(HWP, DOCX 등)은 이 인터페이스를 구현한 @Component 를 추가하면 자동으로 등록된다.
 */
public interface TextExtractor {

    /**
     * @param extension 소문자 확장자 (예: ".pdf")
     */
    boolean supports(String extension);

    /**
     * 페이지별 텍스트를 추출한다. 빈 페이지는 결과에서 제외한다.
     *
     * @throws TextExtractionException 추출할 텍스트가 없거나 파일을 읽을 수 없을 때
     */
    List<PageText> extract(Path path);
}
