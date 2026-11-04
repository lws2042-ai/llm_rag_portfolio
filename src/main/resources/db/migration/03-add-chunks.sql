-- =========================================================
-- 3단계(청크 분할) 마이그레이션
-- 2단계 스키마로 이미 테이블을 만든 DB 에 rag 계정으로 한 번 실행한다.
-- 새로 설치할 때는 schema-oracle.sql 만 실행하면 된다.
-- =========================================================

ALTER TABLE documents ADD (chunk_count NUMBER(10));

ALTER TABLE documents DROP CONSTRAINT ck_documents_status;
ALTER TABLE documents ADD CONSTRAINT ck_documents_status
    CHECK (status IN ('UPLOADED', 'EXTRACTED', 'CHUNKED', 'FAILED'));

COMMENT ON COLUMN documents.status IS 'UPLOADED / EXTRACTED / CHUNKED / FAILED (임베딩 단계에서 INDEXED 추가 예정)';

CREATE TABLE document_chunks (
    id           NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    document_id  NUMBER      NOT NULL,
    chunk_index  NUMBER(10)  NOT NULL,
    page_number  NUMBER(10)  NOT NULL,
    content      CLOB        NOT NULL,
    char_count   NUMBER(10)  NOT NULL,
    CONSTRAINT fk_chunks_document FOREIGN KEY (document_id)
        REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT uk_chunks_document_index UNIQUE (document_id, chunk_index)
);

COMMENT ON TABLE document_chunks IS '문서 검색용 청크';
