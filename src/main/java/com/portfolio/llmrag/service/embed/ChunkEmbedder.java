package com.portfolio.llmrag.service.embed;

import com.portfolio.llmrag.config.EmbeddingProperties;
import com.portfolio.llmrag.exception.EmbeddingException;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 청크 본문을 벡터로 바꾼다.
 *
 * - 청크를 batchSize 개씩 묶어 임베딩 서버에 보낸다. (청크마다 요청하면 느리다)
 * - 돌아온 벡터의 개수와 차원을 확인한다. 모델을 바꿔 차원이 달라지면 DB 에 넣기 전에 알 수 있다.
 */
@Component
public class ChunkEmbedder {

    private final EmbeddingModel model;
    private final int dimensions;
    private final int batchSize;

    public ChunkEmbedder(EmbeddingModel model, EmbeddingProperties properties) {
        this.model = model;
        this.dimensions = properties.dimensions();
        this.batchSize = properties.batchSize();
    }

    /** @return texts 와 같은 순서의 벡터 목록 */
    public List<float[]> embed(List<String> texts) {
        List<float[]> vectors = new ArrayList<>(texts.size());
        for (int from = 0; from < texts.size(); from += batchSize) {
            List<String> batch = texts.subList(from, Math.min(from + batchSize, texts.size()));
            vectors.addAll(embedBatch(batch));
        }
        return vectors;
    }

    private List<float[]> embedBatch(List<String> batch) {
        List<float[]> result;
        try {
            result = model.embed(batch);
        } catch (RuntimeException e) {
            throw new EmbeddingException("임베딩 서버(Ollama)에서 벡터를 받지 못했습니다: " + e.getMessage(), e);
        }

        if (result.size() != batch.size()) {
            throw new EmbeddingException(
                    "청크 " + batch.size() + "개를 보냈는데 벡터 " + result.size() + "개가 돌아왔습니다.");
        }
        for (float[] vector : result) {
            if (vector.length != dimensions) {
                throw new EmbeddingException("벡터 차원이 " + vector.length + "입니다. DB 컬럼은 " + dimensions
                        + "차원이므로 임베딩 모델 설정(spring.ai.ollama.embedding.model)을 확인하세요.");
            }
        }
        return result;
    }
}
