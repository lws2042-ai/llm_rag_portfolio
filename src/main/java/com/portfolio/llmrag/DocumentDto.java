package com.portfolio.llmrag;

public record DocumentDto(
    Long id,        //문서 식별 번호
    String title,   //문서 제목
    String type     //문서 형식 PDF, HWP, EXCEL 등등
) {
}
