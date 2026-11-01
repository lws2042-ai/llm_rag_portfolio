package com.portfolio.llmrag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 파일 저장 설정 (application.properties 의 app.storage.*)
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        @DefaultValue("uploads") String uploadDir
) {
}
