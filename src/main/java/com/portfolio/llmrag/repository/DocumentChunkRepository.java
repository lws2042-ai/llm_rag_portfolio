package com.portfolio.llmrag.repository;

import com.portfolio.llmrag.dto.ChunkDto;
import com.portfolio.llmrag.service.chunk.TextChunk;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * document_chunks 테이블 접근 (Oracle).
 */
@Repository
public class DocumentChunkRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public DocumentChunkRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void deleteByDocument(long documentId) {
        jdbc.update("DELETE FROM document_chunks WHERE document_id = :documentId",
                new MapSqlParameterSource("documentId", documentId));
    }

    public void saveAll(long documentId, List<TextChunk> chunks) {
        String sql = """
                INSERT INTO document_chunks (document_id, chunk_index, page_number, content, char_count)
                VALUES (:documentId, :chunkIndex, :pageNumber, :content, :charCount)
                """;
        SqlParameterSource[] batch = chunks.stream()
                .map(chunk -> new MapSqlParameterSource()
                        .addValue("documentId", documentId)
                        .addValue("chunkIndex", chunk.chunkIndex())
                        .addValue("pageNumber", chunk.pageNumber())
                        .addValue("content", chunk.content())
                        .addValue("charCount", chunk.content().length()))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate(sql, batch);
    }

    public List<ChunkDto> findByDocument(long documentId) {
        String sql = """
                SELECT chunk_index, page_number, char_count, content
                  FROM document_chunks
                 WHERE document_id = :documentId
                 ORDER BY chunk_index
                """;
        return jdbc.query(sql,
                new MapSqlParameterSource("documentId", documentId),
                (rs, rowNum) -> new ChunkDto(
                        rs.getInt("chunk_index"),
                        rs.getInt("page_number"),
                        rs.getInt("char_count"),
                        rs.getString("content")));
    }
}
