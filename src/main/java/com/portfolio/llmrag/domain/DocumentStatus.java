package com.portfolio.llmrag.domain;

/**
 * 문서 처리 상태.
 *
 * UPLOADED  : 파일 저장 완료, 텍스트 추출 전
 * EXTRACTED : 페이지별 텍스트 추출 완료, 청크 분할 전
 * CHUNKED   : 검색용 청크 분할 완료, 임베딩 전
 *             (임베딩에 실패하면 이 상태로 남고 fail_reason 에 사유를 기록한다. 다시 시도할 수 있다.)
 * INDEXED   : 청크마다 임베딩 벡터 저장 완료 → 검색 가능
 * FAILED    : 텍스트 추출 또는 청크 저장 실패 (fail_reason 에 사유 기록)
 */
public enum DocumentStatus {
    UPLOADED,
    EXTRACTED,
    CHUNKED,
    INDEXED,
    FAILED
}
