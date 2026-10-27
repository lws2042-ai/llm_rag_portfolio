package com.portfolio.llmrag.service;

import java.time.Instant;

public record StoredFileInfo(
        String storedName,
        String extension,
        long sizeBytes,
        Instant lastModified
) {
}