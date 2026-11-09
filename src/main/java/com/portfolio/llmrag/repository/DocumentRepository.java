package com.portfolio.llmrag.repository;

import com.portfolio.llmrag.domain.Document;
import com.portfolio.llmrag.domain.DocumentStatus;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/**
 * documents 테이블 접근 (Oracle).
 */
@Repository
public class DocumentRepository {

    private static final int MAX_FAIL_REASON_LENGTH = 1000;

    private static final String SELECT_COLUMNS = """
            SELECT id, original_name, stored_name, extension, size_bytes,
                   status, page_count, chunk_count, fail_reason, uploaded_at, updated_at
              FROM documents
            """;

    private static final RowMapper<Document> ROW_MAPPER = (rs, rowNum) -> new Document(
            rs.getLong("id"),
            rs.getString("original_name"),
            rs.getString("stored_name"),
            rs.getString("extension"),
            rs.getLong("size_bytes"),
            DocumentStatus.valueOf(rs.getString("status")),
            rs.getObject("page_count") == null ? null : rs.getInt("page_count"),
            rs.getObject("chunk_count") == null ? null : rs.getInt("chunk_count"),
            rs.getString("fail_reason"),
            toLocalDateTime(rs.getTimestamp("uploaded_at")),
            toLocalDateTime(rs.getTimestamp("updated_at"))
    );

    private final NamedParameterJdbcTemplate jdbc;

    public DocumentRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 새 문서를 UPLOADED 상태로 저장하고 생성된 id 를 반환한다.
     */
    public long insert(String originalName, String storedName, String extension, long sizeBytes) {
        String sql = """
                INSERT INTO documents (original_name, stored_name, extension, size_bytes, status)
                VALUES (:originalName, :storedName, :extension, :sizeBytes, :status)
                """;
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("originalName", originalName)
                .addValue("storedName", storedName)
                .addValue("extension", extension)
                .addValue("sizeBytes", sizeBytes)
                .addValue("status", DocumentStatus.UPLOADED.name());

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(sql, params, keyHolder, new String[]{"ID"});

        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("문서 id 를 생성하지 못했습니다.");
        }
        return key.longValue();
    }

    public Optional<Document> findById(long id) {
        List<Document> rows = jdbc.query(
                SELECT_COLUMNS + " WHERE id = :id",
                new MapSqlParameterSource("id", id),
                ROW_MAPPER);
        return rows.stream().findFirst();
    }

    public List<Document> findAll() {
        return jdbc.query(SELECT_COLUMNS + " ORDER BY id DESC", ROW_MAPPER);
    }

    public void markExtracted(long id, int pageCount) {
        String sql = """
                UPDATE documents
                   SET status = :status, page_count = :pageCount, fail_reason = NULL,
                       updated_at = SYSTIMESTAMP
                 WHERE id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("status", DocumentStatus.EXTRACTED.name())
                .addValue("pageCount", pageCount)
                .addValue("id", id));
    }

    public void markChunked(long id, int chunkCount) {
        String sql = """
                UPDATE documents
                   SET status = :status, chunk_count = :chunkCount, fail_reason = NULL,
                       updated_at = SYSTIMESTAMP
                 WHERE id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("status", DocumentStatus.CHUNKED.name())
                .addValue("chunkCount", chunkCount)
                .addValue("id", id));
    }

    public void markIndexed(long id) {
        String sql = """
                UPDATE documents
                   SET status = :status, fail_reason = NULL, updated_at = SYSTIMESTAMP
                 WHERE id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("status", DocumentStatus.INDEXED.name())
                .addValue("id", id));
    }

    /** 상태는 그대로 두고 실패 사유만 기록한다. (임베딩 실패처럼 다시 시도할 수 있는 경우) */
    public void recordFailReason(long id, String reason) {
        String sql = """
                UPDATE documents
                   SET fail_reason = :reason, updated_at = SYSTIMESTAMP
                 WHERE id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("reason", truncate(reason))
                .addValue("id", id));
    }

    public void markFailed(long id, String reason) {
        String sql = """
                UPDATE documents
                   SET status = :status, fail_reason = :reason, updated_at = SYSTIMESTAMP
                 WHERE id = :id
                """;
        jdbc.update(sql, new MapSqlParameterSource()
                .addValue("status", DocumentStatus.FAILED.name())
                .addValue("reason", truncate(reason))
                .addValue("id", id));
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return "알 수 없는 오류";
        }
        return reason.length() <= MAX_FAIL_REASON_LENGTH ? reason : reason.substring(0, MAX_FAIL_REASON_LENGTH);
    }

    private static java.time.LocalDateTime toLocalDateTime(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }
}
