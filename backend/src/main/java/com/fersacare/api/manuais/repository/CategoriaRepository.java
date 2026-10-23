package com.fersacare.api.manuais.repository;

import com.fersacare.api.manuais.dto.CategoriaDTO;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** CRUD de `categoria`. Nome duplicado estoura DuplicateKeyException (unique no banco) — o Service traduz. */
@Repository
public class CategoriaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public CategoriaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_BASE = """
        select c.id, c.nome, c.descricao, c.ativo, c.criado_em,
               (select count(*) from manual m where m.categoria_id = c.id) as total_manuais
        from categoria c
        """;

    private static final RowMapper<CategoriaDTO> MAPPER = (rs, rowNum) -> new CategoriaDTO(
        UUID.fromString(rs.getString("id")),
        rs.getString("nome"),
        rs.getString("descricao"),
        rs.getBoolean("ativo"),
        rs.getInt("total_manuais"),
        rs.getTimestamp("criado_em").toInstant()
    );

    public List<CategoriaDTO> listarTodas() {
        return jdbc.query(SELECT_BASE + " order by c.nome", MAPPER);
    }

    public List<CategoriaDTO> listarAtivas() {
        return jdbc.query(SELECT_BASE + " where c.ativo order by c.nome", MAPPER);
    }

    public Optional<CategoriaDTO> buscarPorId(UUID id) {
        try {
            return Optional.ofNullable(
                jdbc.queryForObject(SELECT_BASE + " where c.id = :id", new MapSqlParameterSource("id", id), MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    /** Sem diferenciar maiúsculas/minúsculas nem espaços nas pontas (usado ao criar categoria pela subpasta). */
    public Optional<CategoriaDTO> buscarPorNome(String nome) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(SELECT_BASE + " where lower(c.nome) = lower(trim(:nome))",
                new MapSqlParameterSource("nome", nome), MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }

    public UUID inserir(String nome, String descricao, boolean ativo) {
        return jdbc.queryForObject(
            "insert into categoria (nome, descricao, ativo) values (:nome, :descricao, :ativo) returning id",
            new MapSqlParameterSource()
                .addValue("nome", nome)
                .addValue("descricao", descricao)
                .addValue("ativo", ativo),
            UUID.class);
    }

    public int atualizar(UUID id, String nome, String descricao, boolean ativo) {
        return jdbc.update("""
            update categoria set nome = :nome, descricao = :descricao, ativo = :ativo, atualizado_em = now()
            where id = :id
            """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("nome", nome)
                .addValue("descricao", descricao)
                .addValue("ativo", ativo));
    }

    /** Os manuais da categoria não somem: ficam sem categoria (on delete set null). */
    public int excluir(UUID id) {
        return jdbc.update("delete from categoria where id = :id", new MapSqlParameterSource("id", id));
    }
}
