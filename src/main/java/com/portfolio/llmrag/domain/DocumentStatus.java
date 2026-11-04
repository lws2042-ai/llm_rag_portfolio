package com.portfolio.llmrag.domain;

/**
 * 문서 처리 상태.
 *
 * UPLOADED  : 파일 저장 완료, 텍스트 추출 전
 * EXTRACTED : 페이지별 텍스트 추출 완료, 청크 분할 전
 * CHUNKED   : 검색용 청크 분할 완료 (다음 단계: 임베딩)
 * FAILED    : 텍스트 추출 또는 청크 저장 실패 (fail_reason 에 사유 기록)
 *
 * 임베딩 단계가 추가되면 INDEXED 상태를 추가한다.
 */
public enum DocumentStatus {
    UPLOADED,
    EXTRACTED,
    CHUNKED,
    FAILED
}
