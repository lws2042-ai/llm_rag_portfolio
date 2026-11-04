-- =========================================================
-- LLM RAG Portfolio - Oracle 스키마
-- 실행: SQL Developer 등에서 앱 계정(rag)으로 접속해 실행
-- =========================================================

-- 업로드 문서
CREATE TABLE documents (
    id             NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    original_name  VARCHAR2(500 CHAR)  NOT NULL,              -- 원본 파일명 (근거 표시용)
    stored_name    VARCHAR2(100)       NOT NULL,              -- 디스크 저장 이름 (UUID + 확장자)
    extension      VARCHAR2(10)        NOT NULL,
    size_bytes     NUMBER(19)          NOT NULL,
    status         VARCHAR2(20)        DEFAULT 'UPLOADED' NOT NULL,
    page_count     NUMBER(10),                                -- 텍스트가 추출된 페이지 수
    chunk_count    NUMBER(10),                                -- 분할된 청크 수
    fail_reason    VARCHAR2(1000 CHAR),                       -- 추출 실패 사유
    uploaded_at    TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    updated_at     TIMESTAMP           DEFAULT SYSTIMESTAMP NOT NULL,
    CONSTRAINT uk_documents_stored_name UNIQUE (stored_name),
    CONSTRAINT ck_documents_status CHECK (status IN ('UPLOADED', 'EXTRACTED', 'CHUNKED', 'FAILED'))
);

COMMENT ON TABLE  documents             IS '업로드 문서';
COMMENT ON COLUMN documents.status      IS 'UPLOADED / EXTRACTED / CHUNKED / FAILED (임베딩 단계에서 INDEXED 추가 예정)';
COMMENT ON COLUMN documents.fail_reason IS '텍스트 추출 실패 사유';

-- 페이지별 추출 텍스트
-- 청크 분할 방식을 바꿔도 PDF 를 다시 읽지 않고 이 테이블에서 다시 만들 수 있다.
CREATE TABLE document_pages (
    id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id  NUMBER      NOT NULL,
    page_number  NUMBER(10)  NOT NULL,
    content      CLOB        NOT NULL,
    char_count   NUMBER(10)  NOT NULL,
    CONSTRAINT fk_pages_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_pages_document_page UNIQUE (document_id, page_number)
);

COMMENT ON TABLE document_pages IS '문서 페이지별 추출 텍스트';

-- 검색용 청크
-- 청크는 페이지를 넘지 않으므로 page_number 를 답변 근거로 그대로 쓴다.
-- 임베딩 단계에서 벡터 컬럼을 추가할 예정.
CREATE TABLE document_chunks (
    id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id  NUMBER      NOT NULL,
    chunk_index  NUMBER(10)  NOT NULL,                        -- 문서 안 순서 (0부터)
    page_number  NUMBER(10)  NOT NULL,
    content      CLOB        NOT NULL,
    char_count   NUMBER(10)  NOT NULL,
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_chunks_document_index UNIQUE (document_id, chunk_index)
);

COMMENT ON TABLE document_chunks IS '문서 검색용 청크';
