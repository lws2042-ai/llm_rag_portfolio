package com.portfolio.llmrag.repository;

import com.portfolio.llmrag.dto.ChunkDto;
import com.portfolio.llmrag.service.chunk.TextChunk;
import oracle.jdbc.OracleType;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.IntStream;

/**
 * document_chunks 테이블 접근 (Oracle).
 */
@Repository
public class DocumentChunkRepository {

    /** 임베딩할 청크 (행 id 와 본문) */
    public record ChunkText(long id, String content) {
    }

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

    public List<ChunkText> findTexts(long documentId) {
        String sql = """
                SELECT id, content
                  FROM document_chunks
                 WHERE document_id = :documentId
                 ORDER BY chunk_index
                """;
        return jdbc.query(sql,
                new MapSqlParameterSource("documentId", documentId),
                (rs, rowNum) -> new ChunkText(rs.getLong("id"), rs.getString("content")));
    }

    /**
     * 청크마다 벡터를 저장한다. ids 와 vectors 는 같은 순서여야 한다.
     * float[] 를 Oracle VECTOR(FLOAT32) 타입으로 그대로 바인딩한다.
     */
    public void saveEmbeddings(List<Long> ids, List<float[]> vectors) {
        if (ids.size() != vectors.size()) {
            throw new IllegalArgumentException("청크 " + ids.size() + "개와 벡터 " + vectors.size() + "개의 수가 다릅니다.");
        }
        int vectorType = OracleType.VECTOR_FLOAT32.getVendorTypeNumber();
        SqlParameterSource[] batch = IntStream.range(0, ids.size())
                .mapToObj(i -> new MapSqlParameterSource()
                        .addValue("id", ids.get(i))
                        .addValue("embedding", new SqlParameterValue(vectorType, vectors.get(i))))
                .toArray(SqlParameterSource[]::new);
        jdbc.batchUpdate("UPDATE document_chunks SET embedding = :embedding WHERE id = :id", batch);
    }

    public List<ChunkDto> findByDocument(long documentId) {
        String sql = """
                SELECT chunk_index, page_number, char_count, content,
                       CASE WHEN embedding IS NULL THEN 0 ELSE 1 END AS embedded
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
                        rs.getInt("embedded") == 1,
                        rs.getString("content")));
    }
}
