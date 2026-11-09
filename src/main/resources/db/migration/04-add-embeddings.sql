-- =========================================================
-- 4단계(임베딩) 마이그레이션
-- 3단계 스키마로 이미 테이블을 만든 DB 에 rag 계정으로 한 번 실행한다.
-- 새로 설치할 때는 schema-oracle.sql 만 실행하면 된다.
-- VECTOR 타입은 Oracle 23ai 이상에서 지원한다.
-- =========================================================

ALTER TABLE document_chunks ADD (embedding VECTOR(1024, FLOAT32));

COMMENT ON COLUMN document_chunks.embedding IS 'bge-m3 임베딩 (임베딩 전에는 NULL)';

ALTER TABLE documents DROP CONSTRAINT ck_documents_status;
ALTER TABLE documents ADD CONSTRAINT ck_documents_status
    CHECK (status IN ('UPLOADED', 'EXTRACTED', 'CHUNKED', 'INDEXED', 'FAILED'));

COMMENT ON COLUMN documents.status IS 'UPLOADED / EXTRACTED / CHUNKED / INDEXED / FAILED';
