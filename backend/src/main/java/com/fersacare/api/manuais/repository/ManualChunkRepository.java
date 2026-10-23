package com.fersacare.api.manuais.repository;

import com.fersacare.api.manuais.dto.ManualChunkDTO;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Tabela `manual_chunk` + a busca vetorial do RAG.
 *
 * O vetor vai para o Postgres como texto no formato do pgvector
 * ('[0.1,0.2,...]') com cast(... as vector). Assim não precisamos de
 * nenhuma biblioteca extra (com-pgvector) só para o tipo — o driver JDBC
 * manda uma string e o Postgres converte.
 */
@Repository
public class ManualChunkRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ManualChunkRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record NovoChunk(int ordem, Integer pagina, String conteudo, float[] embedding) {}

    /** Um trecho encontrado pela busca vetorial. score = similaridade de cosseno (1 = idêntico). */
    public record TrechoEncontrado(
        UUID chunkId, UUID manualId, String tituloManual, Integer pagina, String conteudo, double score) {}

    /** Troca todos os chunks do manual (usado na ingestão e no reprocessamento). Chamar dentro de transação. */
    public void substituir(UUID manualId, List<NovoChunk> chunks) {
        jdbc.update("delete from manual_chunk where manual_id = :id", new MapSqlParameterSource("id", manualId));

        MapSqlParameterSource[] lote = chunks.stream()
            .map(c -> new MapSqlParameterSource()
                .addValue("manualId", manualId)
                .addValue("ordem", c.ordem())
                .addValue("pagina", c.pagina())
                .addValue("conteudo", c.conteudo())
                .addValue("embedding", paraVetor(c.embedding())))
            .toArray(MapSqlParameterSource[]::new);

        jdbc.batchUpdate("""
            insert into manual_chunk (manual_id, ordem, pagina, conteudo, embedding)
            values (:manualId, :ordem, :pagina, :conteudo, cast(:embedding as vector))
            """, lote);
    }

    public List<ManualChunkDTO> listarPorManual(UUID manualId) {
        return jdbc.query("""
            select id, ordem, pagina, conteudo, vector_dims(embedding) as dims
            from manual_chunk where manual_id = :id order by ordem
            """,
            new MapSqlParameterSource("id", manualId),
            (rs, rowNum) -> new ManualChunkDTO(
                UUID.fromString(rs.getString("id")),
                rs.getInt("ordem"),
                (Integer) rs.getObject("pagina"),
                rs.getString("conteudo"),
                rs.getInt("dims")));
    }

    /**
     * Top-K por similaridade de cosseno, só entre manuais visíveis ao público
     * (ativo, status 'pronto', categoria ativa ou sem categoria).
     * O "order by embedding <=> vetor" é o que deixa o Postgres usar o índice HNSW.
     */
    public List<TrechoEncontrado> buscarSimilares(float[] vetorPergunta, int topK, double scoreMinimo) {
        return jdbc.query("""
            select id, manual_id, titulo, pagina, conteudo, 1 - distancia as score
            from (
              select mc.id, mc.manual_id, m.titulo, mc.pagina, mc.conteudo,
                     mc.embedding <=> cast(:vetor as vector) as distancia
              from manual_chunk mc
              join manual m on m.id = mc.manual_id
              left join categoria c on c.id = m.categoria_id
              where m.ativo and m.status = 'pronto' and (m.categoria_id is null or c.ativo)
              order by mc.embedding <=> cast(:vetor as vector)
              limit :topK
            ) t
            where 1 - distancia >= :scoreMinimo
            order by distancia
            """,
            new MapSqlParameterSource()
                .addValue("vetor", paraVetor(vetorPergunta))
                .addValue("topK", topK)
                .addValue("scoreMinimo", scoreMinimo),
            (rs, rowNum) -> new TrechoEncontrado(
                UUID.fromString(rs.getString("id")),
                UUID.fromString(rs.getString("manual_id")),
                rs.getString("titulo"),
                (Integer) rs.getObject("pagina"),
                rs.getString("conteudo"),
                rs.getDouble("score")));
    }

    static String paraVetor(float[] v) {
        StringBuilder sb = new StringBuilder(v.length * 12).append('[');
        for (int i = 0; i < v.length; i++) {
            if (i > 0) sb.append(',');
            sb.append(v[i]);
        }
        return sb.append(']').toString();
    }
}
