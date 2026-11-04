package com.portfolio.llmrag.service.chunk;

import com.portfolio.llmrag.config.ChunkProperties;
import com.portfolio.llmrag.service.extract.PageText;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 페이지 텍스트를 검색용 청크로 나눈다.
 *
 * 규칙
 * 1. 청크는 페이지를 넘지 않는다. (출처 페이지를 정확히 표시하기 위해)
 * 2. 문장(또는 줄) 단위로 모아 size 글자를 넘기 직전에 끊는다.
 * 3. 다음 청크는 앞 청크의 마지막 문장들을 overlap 글자까지 다시 포함한다.
 * 4. size 보다 긴 문장은 공백 위치를 우선해 강제로 자른다.
 */
@Component
public class TextChunker {

    private final int size;
    private final int overlap;

    public TextChunker(ChunkProperties properties) {
        this.size = properties.size();
        this.overlap = properties.overlap();
    }

    public List<TextChunk> chunk(List<PageText> pages) {
        List<TextChunk> chunks = new ArrayList<>();
        for (PageText page : pages) {
            for (String content : split(page.text())) {
                chunks.add(new TextChunk(chunks.size(), page.pageNumber(), content));
            }
        }
        return chunks;
    }

    /** 한 페이지의 텍스트를 청크 본문 목록으로 나눈다. */
    List<String> split(String text) {
        List<String> result = new ArrayList<>();
        List<String> current = new ArrayList<>();
        int length = 0;

        for (String unit : units(text)) {
            if (length + unit.length() > size && !current.isEmpty()) {
                emit(current, result);

                // 앞 청크 끝부분의 문장들을 overlap 글자까지 다음 청크로 넘긴다.
                List<String> tail = new ArrayList<>();
                int tailLength = 0;
                for (int i = current.size() - 1; i >= 0; i--) {
                    String previous = current.get(i);
                    if (tailLength + previous.length() > overlap) {
                        break;
                    }
                    tail.addFirst(previous);
                    tailLength += previous.length();
                }
                // 겹치는 부분 때문에 새 문장이 들어가지 못하면 앞에서부터 덜어낸다.
                while (!tail.isEmpty() && tailLength + unit.length() > size) {
                    tailLength -= tail.removeFirst().length();
                }
                current = tail;
                length = tailLength;
            }
            current.add(unit);
            length += unit.length();
        }
        emit(current, result);
        return result;
    }

    private static void emit(List<String> units, List<String> result) {
        String content = String.join("", units).strip();
        if (!content.isEmpty()) {
            result.add(content);
        }
    }

    /**
     * 문장 단위로 나눈다. 각 조각은 뒤따르는 공백을 포함하므로 이어 붙이면 원문이 된다.
     * 문장 끝: . ! ? 뒤에 공백이 오거나 텍스트가 끝날 때, 또는 줄바꿈.
     */
    private List<String> units(String text) {
        List<String> sentences = new ArrayList<>();
        int start = 0;
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            boolean sentenceEnd = (c == '.' || c == '!' || c == '?')
                    && (i + 1 == n || Character.isWhitespace(text.charAt(i + 1)));
            i++;
            if (sentenceEnd || c == '\n') {
                while (i < n && Character.isWhitespace(text.charAt(i))) {
                    i++;
                }
                sentences.add(text.substring(start, i));
                start = i;
            }
        }
        if (start < n) {
            sentences.add(text.substring(start));
        }

        List<String> units = new ArrayList<>();
        for (String sentence : sentences) {
            if (sentence.length() <= size) {
                units.add(sentence);
            } else {
                units.addAll(hardSplit(sentence));
            }
        }
        return units;
    }

    /** size 보다 긴 문장을 자른다. 단어가 잘리지 않게 뒤쪽 절반 안의 마지막 공백에서 끊는다. */
    private List<String> hardSplit(String sentence) {
        List<String> pieces = new ArrayList<>();
        int start = 0;
        while (sentence.length() - start > size) {
            int end = start + size;
            int space = lastWhitespace(sentence, start + size / 2, end);
            if (space > start) {
                end = space + 1;
            }
            pieces.add(sentence.substring(start, end));
            start = end;
        }
        pieces.add(sentence.substring(start));
        return pieces;
    }

    private static int lastWhitespace(String s, int from, int to) {
        for (int i = to - 1; i >= from; i--) {
            if (Character.isWhitespace(s.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
