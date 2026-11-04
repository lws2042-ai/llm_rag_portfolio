package com.portfolio.llmrag.repository;

import com.portfolio.llmrag.dto.PageSummaryDto;
import com.portfolio.llmrag.service.extract.PageText;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * document_pages 테이블 접근 (Oracle).
 * 추출한 텍스트를 저장해 두면 청크 분할 방식을 바꿀 때 PDF 를 다시 읽지 않아도 된다.
 */
@Repository
public class DocumentPageRepository {

    private static final int PREVIEW_LENGTH = 100;

    private final NamedParameterJdbcTemplate jdbc;

    public DocumentPageRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void saveAll(long documentId, List<PageText> pages) {
        String sql = """
                INSERT INTO document_pages (document_id, page_number, content, char_count)
                VALUES (:documentId, :pageNumber, :content, :charCount)
                """;
        SqlParameterSource[] batch = pages.stream()
                .map(page -> new MapSqlParameterSource()
                        .addValue("documentId", documentId)
                        .addValue("pageNumber", page.pageNumber())
                        .addValue("content", page.text())
                        .addValue("charCount", page.text().length()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(sql, batch);
    }

    public List<PageSummaryDto> findSummaries(long documentId) {
        String sql = """
                SELECT page_number, char_count,
                       DBMS_LOB.SUBSTR(content, :previewLength, 1) AS preview
                  FROM document_pages
                 WHERE document_id = :documentId
                 ORDER BY page_number
                """;
        return jdbc.query(sql,
                new MapSqlParameterSource()
                        .addValue("documentId", documentId)
                        .addValue("previewLength", PREVIEW_LENGTH),
                (rs, rowNum) -> new PageSummaryDto(
                        rs.getInt("page_number"),
                        rs.getInt("char_count"),
                        rs.getString("preview")));
    }

    /** 청크를 다시 만들 때 쓰는 페이지 전체 텍스트 (페이지 순) */
    public List<PageText> findAll(long documentId) {
        String sql = """
                SELECT page_number, content
                  FROM document_pages
                 WHERE document_id = :documentId
                 ORDER BY page_number
                """;
        return jdbc.query(sql,
                new MapSqlParameterSource("documentId", documentId),
                (rs, rowNum) -> new PageText(rs.getInt("page_number"), rs.getString("content")));
    }

    public Optional<String> findContent(long documentId, int pageNumber) {
        String sql = """
                SELECT content
                  FROM document_pages
                 WHERE document_id = :documentId AND page_number = :pageNumber
                """;
        List<String> rows = jdbc.query(sql,
                new MapSqlParameterSource()
                        .addValue("documentId", documentId)
                        .addValue("pageNumber", pageNumber),
                (rs, rowNum) -> rs.getString("content"));
        return rows.stream().findFirst();
    }
}
