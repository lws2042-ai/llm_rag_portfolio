package com.portfolio.llmrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 임베딩 설정 (application.properties 의 app.embedding.*)
 *
 * @param dimensions 벡터 차원. document_chunks.embedding 컬럼(VECTOR(1024, FLOAT32))과 같아야 한다.
 * @param batchSize  임베딩 서버에 한 번에 보낼 청크 수
 */
@ConfigurationProperties(prefix = "app.embedding")
public record EmbeddingProperties(
        @DefaultValue("1024") int dimensions,
        @DefaultValue("16") int batchSize
) {

    public EmbeddingProperties {
        if (dimensions <= 0) {
            throw new IllegalArgumentException("app.embedding.dimensions 는 0보다 커야 합니다: " + dimensions);
        }
        if (batchSize <= 0) {
            throw new IllegalArgumentException("app.embedding.batch-size 는 0보다 커야 합니다: " + batchSize);
        }
    }
}
