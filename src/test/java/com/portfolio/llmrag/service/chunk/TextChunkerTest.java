package com.portfolio.llmrag.service.chunk;

import com.portfolio.llmrag.config.ChunkProperties;
import com.portfolio.llmrag.service.extract.PageText;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    private static TextChunker chunker(int size, int overlap) {
        return new TextChunker(new ChunkProperties(size, overlap));
    }

    @Test
    void 짧은_페이지는_청크_하나가_된다() {
        List<String> chunks = chunker(100, 20).split("제1조 목적. 이 규정은 출장비 지급 기준을 정한다.");

        assertEquals(List.of("제1조 목적. 이 규정은 출장비 지급 기준을 정한다."), chunks);
    }

    @Test
    void 청크는_size를_넘지_않고_문장_단위로_끊긴다() {
        String text = "가나다라마바사. ".repeat(20); // 문장 하나 10자
        List<String> chunks = chunker(50, 0).split(text.strip());

        assertTrue(chunks.size() > 1);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 50, "청크 길이 초과: " + chunk.length());
            assertTrue(chunk.endsWith("."), "문장 중간에서 끊김: " + chunk);
        }
    }

    @Test
    void 다음_청크는_앞_청크의_마지막_문장으로_시작한다() {
        String text = "첫째 문장이다. 둘째 문장이다. 셋째 문장이다. 넷째 문장이다.";
        // 문장 하나가 공백 포함 9자 → size 20 이면 청크당 두 문장, overlap 10 이면 한 문장이 겹친다.
        List<String> chunks = chunker(20, 10).split(text);

        assertEquals(List.of(
                "첫째 문장이다. 둘째 문장이다.",
                "둘째 문장이다. 셋째 문장이다.",
                "셋째 문장이다. 넷째 문장이다."), chunks);
    }

    @Test
    void overlap이_0이면_겹치지_않고_원문을_그대로_나눈다() {
        String text = "첫째 문장이다. 둘째 문장이다. 셋째 문장이다. 넷째 문장이다.";
        List<String> chunks = chunker(30, 0).split(text);

        assertEquals(text.replace(" ", ""), String.join("", chunks).replace(" ", ""));
    }

    @Test
    void size보다_긴_문장은_공백에서_강제로_자른다() {
        String text = "단어 ".repeat(50).strip(); // 마침표 없는 149자
        List<String> chunks = chunker(40, 0).split(text);

        assertTrue(chunks.size() >= 4);
        for (String chunk : chunks) {
            assertTrue(chunk.length() <= 40);
            assertTrue(chunk.matches("(단어 ?)+"), "단어가 잘림: " + chunk);
        }
    }

    @Test
    void 줄바꿈도_문장_경계로_본다() {
        List<String> chunks = chunker(12, 0).split("제1조 목적\n제2조 정의\n제3조 범위");

        assertEquals(List.of("제1조 목적", "제2조 정의", "제3조 범위"), chunks);
    }

    @Test
    void 청크는_페이지를_넘지_않고_번호가_이어진다() {
        List<TextChunk> chunks = chunker(100, 20).chunk(List.of(
                new PageText(1, "1쪽 내용이다."),
                new PageText(2, "2쪽 내용이다."),
                new PageText(3, "3쪽 내용이다.")));

        assertEquals(3, chunks.size());
        for (int i = 0; i < 3; i++) {
            assertEquals(i, chunks.get(i).chunkIndex());
            assertEquals(i + 1, chunks.get(i).pageNumber());
            assertEquals((i + 1) + "쪽 내용이다.", chunks.get(i).content());
        }
    }

    @Test
    void 잘못된_설정은_거부한다() {
        assertThrows(IllegalArgumentException.class, () -> new ChunkProperties(0, 0));
        assertThrows(IllegalArgumentException.class, () -> new ChunkProperties(100, 100));
        assertThrows(IllegalArgumentException.class, () -> new ChunkProperties(100, -1));
    }
}
