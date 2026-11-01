package com.portfolio.llmrag.service.extract;

import java.util.regex.Pattern;

/**
 * 추출한 텍스트의 공통 정리 규칙.
 */
public final class TextCleaner {

    private static final Pattern TRAILING_SPACES = Pattern.compile("[ \\t]+\\n");
    private static final Pattern MANY_BLANK_LINES = Pattern.compile("\\n{3,}");

    private TextCleaner() {
    }

    public static String clean(String raw) {
        if (raw == null) {
            return "";
        }
        String text = raw
                .replace("﻿", "")      // BOM
                .replace("\u0000", "")      // NULL 문자 (DB 저장 시 문제)
                .replace("\r\n", "\n")
                .replace('\r', '\n');
        text = TRAILING_SPACES.matcher(text).replaceAll("\n");
        text = MANY_BLANK_LINES.matcher(text).replaceAll("\n\n");
        return text.strip();
    }
}
