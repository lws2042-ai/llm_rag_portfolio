package com.portfolio.llmrag;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class LlmRagPortfolioApplication {

    public static void main(String[] args) {
        SpringApplication.run(LlmRagPortfolioApplication.class, args);
    }
}
