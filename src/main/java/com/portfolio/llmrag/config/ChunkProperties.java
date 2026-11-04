package com.portfolio.llmrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 청크 분할 설정 (application.properties 의 app.chunk.*)
 *
 * @param size    청크 하나의 최대 글자 수
 * @param overlap 앞 청크와 겹치게 둘 최대 글자 수 (문장이 청크 경계에서 끊겨 맥락을 잃지 않도록)
 */
@ConfigurationProperties(prefix = "app.chunk")
public record ChunkProperties(
        @DefaultValue("800") int size,
        @DefaultValue("150") int overlap
) {

    public ChunkProperties {
        if (size <= 0) {
            throw new IllegalArgumentException("app.chunk.size 는 0보다 커야 합니다: " + size);
        }
        if (overlap < 0 || overlap >= size) {
            throw new IllegalArgumentException(
                    "app.chunk.overlap 은 0 이상, size(" + size + ") 미만이어야 합니다: " + overlap);
        }
    }
}
