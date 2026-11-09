package com.portfolio.llmrag.service.embed;

import com.portfolio.llmrag.config.EmbeddingProperties;
import com.portfolio.llmrag.exception.EmbeddingException;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 임베딩 서버 없이 묶음 처리와 검증 규칙만 확인한다.
 */
class ChunkEmbedderTest {

    private static final int DIMENSIONS = 4;

    private final EmbeddingModel model = mock(EmbeddingModel.class);

    private ChunkEmbedder embedder(int batchSize) {
        return new ChunkEmbedder(model, new EmbeddingProperties(DIMENSIONS, batchSize));
    }

    /** 받은 문장 수만큼, 문장 순서가 드러나는 벡터를 돌려주는 가짜 모델 */
    private void answerWithVectors(int dimensions) {
        when(model.embed(anyList())).thenAnswer(invocation -> {
            List<String> texts = invocation.getArgument(0);
            return texts.stream()
                    .map(text -> {
                        float[] v = new float[dimensions];
                        v[0] = Float.parseFloat(text);
                        return v;
                    })
                    .toList();
        });
    }

    private static List<String> numbers(int count) {
        return IntStream.range(0, count).mapToObj(String::valueOf).toList();
    }

    @Test
    void 청크를_batchSize_개씩_나눠_보내고_순서를_지킨다() {
        answerWithVectors(DIMENSIONS);

        List<float[]> vectors = embedder(16).embed(numbers(40));

        verify(model, times(3)).embed(anyList()); // 16 + 16 + 8
        assertEquals(40, vectors.size());
        for (int i = 0; i < 40; i++) {
            assertEquals(i, vectors.get(i)[0]);
        }
    }

    @Test
    void 빈_목록이면_서버를_부르지_않는다() {
        assertTrue(embedder(16).embed(List.of()).isEmpty());
        verify(model, times(0)).embed(anyList());
    }

    @Test
    void 차원이_다르면_DB에_넣기_전에_막는다() {
        answerWithVectors(DIMENSIONS + 1);

        EmbeddingException e = assertThrows(EmbeddingException.class, () -> embedder(16).embed(numbers(3)));
        assertTrue(e.getMessage().contains("차원"));
    }

    @Test
    void 서버_오류는_EmbeddingException_으로_바꾼다() {
        when(model.embed(anyList())).thenThrow(new RuntimeException("Connection refused"));

        EmbeddingException e = assertThrows(EmbeddingException.class, () -> embedder(16).embed(numbers(3)));
        assertTrue(e.getMessage().contains("Connection refused"));
    }

    @Test
    void 벡터_수가_모자라면_실패한다() {
        when(model.embed(anyList())).thenReturn(List.of(new float[DIMENSIONS]));

        assertThrows(EmbeddingException.class, () -> embedder(16).embed(numbers(3)));
    }
}
