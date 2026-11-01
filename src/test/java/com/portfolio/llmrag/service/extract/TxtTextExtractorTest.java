package com.portfolio.llmrag.service.extract;

import com.portfolio.llmrag.exception.TextExtractionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TxtTextExtractorTest {

    private static final String KOREAN = "출장비 지급 규정\r\n제1조 목적";

    private final TxtTextExtractor extractor = new TxtTextExtractor();

    @TempDir
    Path tempDir;

    @Test
    void UTF8_파일을_읽는다() throws IOException {
        Path file = tempDir.resolve("utf8.txt");
        Files.write(file, KOREAN.getBytes(StandardCharsets.UTF_8));

        List<PageText> pages = extractor.extract(file);

        assertEquals(1, pages.size());
        assertEquals("출장비 지급 규정\n제1조 목적", pages.get(0).text());
    }

    @Test
    void EUC_KR_파일도_한글이_깨지지_않는다() throws IOException {
        Path file = tempDir.resolve("euckr.txt");
        Files.write(file, KOREAN.getBytes(Charset.forName("MS949")));

        List<PageText> pages = extractor.extract(file);

        assertEquals("출장비 지급 규정\n제1조 목적", pages.get(0).text());
    }

    @Test
    void BOM은_제거한다() throws IOException {
        Path file = tempDir.resolve("bom.txt");
        Files.write(file, ("﻿" + KOREAN).getBytes(StandardCharsets.UTF_8));

        assertEquals("출장비 지급 규정\n제1조 목적", extractor.extract(file).get(0).text());
    }

    @Test
    void 빈_파일은_실패한다() throws IOException {
        Path file = tempDir.resolve("empty.txt");
        Files.writeString(file, "  \n \n");

        assertThrows(TextExtractionException.class, () -> extractor.extract(file));
    }
}
