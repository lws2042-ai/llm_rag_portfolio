package com.portfolio.llmrag.service.extract;

import com.portfolio.llmrag.exception.TextExtractionException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * TXT 텍스트 추출.
 * 공공기관 TXT 는 EUC-KR(CP949) 이 많으므로 UTF-8 로 먼저 읽고, 실패하면 MS949 로 다시 읽는다.
 * TXT 에는 페이지 개념이 없으므로 전체를 1페이지로 저장한다.
 */
@Component
public class TxtTextExtractor implements TextExtractor {

    private static final Charset MS949 = Charset.forName("MS949");

    @Override
    public boolean supports(String extension) {
        return ".txt".equals(extension);
    }

    @Override
    public List<PageText> extract(Path path) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new TextExtractionException("텍스트 파일을 읽을 수 없습니다.", e);
        }

        String text = TextCleaner.clean(decode(bytes));
        if (text.isEmpty()) {
            throw new TextExtractionException("내용이 비어 있는 텍스트 파일입니다.");
        }
        return List.of(new PageText(1, text));
    }

    static String decode(byte[] bytes) {
        CharsetDecoder utf8 = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            return utf8.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            return new String(bytes, MS949);
        }
    }
}
