package com.fersacare.api.manuais.repository;

import com.fersacare.api.manuais.dto.ManualDTO;
import com.fersacare.api.manuais.model.StatusManual;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Types;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Tabela `manual`. O PDF (arquivo_pdf, bytea) nunca entra no SELECT_BASE —
 * só é lido nos métodos que realmente precisam dele (ingestão e download),
 * para a listagem do admin não trafegar megabytes à toa.
 *
 * "Visível ao público" = ativo, status 'pronto' e categoria ativa (ou sem
 * categoria). A mesma regra está na busca vetorial (ManualChunkRepository).
 */
@Repository
public class ManualRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ManualRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record ArquivoPdf(String nomeArquivo, byte[] conteudo) {}

    /** O que a sincronização precisa saber de um manual que veio do SharePoint. */
    public record VinculoSharePoint(UUID manualId, String itemId, String etag, String titulo, StatusManual status) {}

    private static final String SELECT_BASE = """
        select m.id, m.titulo, m.categoria_id, c.nome as categoria_nome, m.nome_arquivo, m.tamanho_bytes,
               m.total_paginas, m.total_chunks, m.status, m.mensagem_erro, m.ativo, m.origem,
               m.sharepoint_web_url, m.sharepoint_caminho, m.criado_em, m.atualizado_em
        from manual m
        left join categoria c on c.id = m.categoria_id
        """;

    private static final String FILTRO_PUBLICO =
        " m.ativo and m.status = 'pronto' and (m.categoria_id is null or c.ativo) ";

    private static final RowMapper<ManualDTO> MAPPER = (rs, rowNum) -> new ManualDTO(
        UUID.fromString(rs.getString("id")),
        rs.getString("titulo"),
        rs.getString("categoria_id") != null ? UUID.fromString(rs.getString("categoria_id")) : null,
        rs.getString("categoria_nome"),
        rs.getString("nome_arquivo"),
        rs.getLong("tamanho_bytes"),
        (Integer) rs.getObject("total_paginas"),
        rs.getInt("total_chunks"),
        StatusManual.doBanco(rs.getString("status")),
        rs.getString("mensagem_erro"),
        rs.getBoolean("ativo"),
        rs.getString("origem"),
        rs.getString("sharepoint_web_url"),
        rs.getString("sharepoint_caminho"),
        rs.getTimestamp("criado_em").toInstant(),
        rs.getTimestamp("atualizado_em").toInstant()
    );

    public UUID inserir(String titulo, UUID categoriaId, String nomeArquivo, byte[] pdf, String hashSha256) {
        return jdbc.queryForObject("""
            insert into manual (titulo, categoria_id, nome_arquivo, tamanho_bytes, hash_sha256, arquivo_pdf, status)
            values (:titulo, :categoriaId, :nomeArquivo, :tamanho, :hash, :pdf, 'processando')
            returning id
            """,
            new MapSqlParameterSource()
                .addValue("titulo", titulo)
                .addValue("categoriaId", categoriaId, Types.OTHER) // uuid opcional — ver ChamadoRepository
                .addValue("nomeArquivo", nomeArquivo)
                .addValue("tamanho", pdf.length)
                .addValue("hash", hashSha256)
                .addValue("pdf", pdf),
            UUID.class);
    }

    /** Manual novo vindo do SharePoint: guarda o id do arquivo e o eTag para as próximas sincronizações. */
    public UUID inserirDoSharePoint(String titulo, UUID categoriaId, String nomeArquivo, byte[] pdf, String hashSha256,
                                    String itemId, String etag, String webUrl, String caminho) {
        return jdbc.queryForObject("""
            insert into manual (titulo, categoria_id, nome_arquivo, tamanho_bytes, hash_sha256, arquivo_pdf, status,
                                origem, sharepoint_item_id, sharepoint_etag, sharepoint_web_url, sharepoint_caminho)
            values (:titulo, :categoriaId, :nomeArquivo, :tamanho, :hash, :pdf, 'processando',
                    'sharepoint', :itemId, :etag, :webUrl, :caminho)
            returning id
            """,
            new MapSqlParameterSource()
                .addValue("titulo", titulo)
                .addValue("categoriaId", categoriaId, Types.OTHER)
                .addValue("nomeArquivo", nomeArquivo)
                .addValue("tamanho", pdf.length)
                .addValue("hash", hashSha256)
                .addValue("pdf", pdf)
                .addValue("itemId", itemId)
                .addValue("etag", etag)
                .addValue("webUrl", webUrl)
                .addValue("caminho", caminho),
            UUID.class);
    }

    /**
     * Arquivo alterado no SharePoint: troca o PDF e volta para 'processando'.
     * Título e categoria ficam como o admin deixou. Devolve false se o manual
     * já estava processando (aí a sincronização tenta de novo da próxima vez).
     */
    public boolean atualizarDoSharePoint(UUID id, String nomeArquivo, byte[] pdf, String hashSha256,
                                         String etag, String webUrl, String caminho) {
        return jdbc.update("""
            update manual set nome_arquivo = :nomeArquivo, tamanho_bytes = :tamanho, hash_sha256 = :hash,
                   arquivo_pdf = :pdf, sharepoint_etag = :etag, sharepoint_web_url = :webUrl,
                   sharepoint_caminho = :caminho, status = 'processando', mensagem_erro = null, atualizado_em = now()
            where id = :id and status <> 'processando'
            """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("nomeArquivo", nomeArquivo)
                .addValue("tamanho", pdf.length)
                .addValue("hash", hashSha256)
                .addValue("pdf", pdf)
                .addValue("etag", etag)
                .addValue("webUrl", webUrl)
                .addValue("caminho", caminho)) == 1;
    }

    /**
     * Manual que já estava no sistema (enviado à mão) e cujo PDF também está
     * no SharePoint: passa a ser acompanhado pela sincronização, sem reprocessar.
     * Só liga se ele ainda não estiver ligado a outro arquivo.
     */
    public boolean vincularAoSharePoint(UUID id, String itemId, String etag, String webUrl, String caminho) {
        return jdbc.update("""
            update manual set origem = 'sharepoint', sharepoint_item_id = :itemId, sharepoint_etag = :etag,
                   sharepoint_web_url = :webUrl, sharepoint_caminho = :caminho, atualizado_em = now()
            where id = :id and sharepoint_item_id is null
            """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("itemId", itemId)
                .addValue("etag", etag)
                .addValue("webUrl", webUrl)
                .addValue("caminho", caminho)) == 1;
    }

    /** Todos os manuais que vieram do SharePoint, pelo id do arquivo lá. */
    public List<VinculoSharePoint> listarVinculosSharePoint() {
        return jdbc.query("""
            select id, sharepoint_item_id, sharepoint_etag, titulo, status
            from manual where sharepoint_item_id is not null
            """,
            (rs, rowNum) -> new VinculoSharePoint(
                UUID.fromString(rs.getString("id")),
                rs.getString("sharepoint_item_id"),
                rs.getString("sharepoint_etag"),
                rs.getString("titulo"),
                StatusManual.doBanco(rs.getString("status"))));
    }

    public Optional<ManualDTO> buscarPorId(UUID id) {
        return umOuVazio(SELECT_BASE + " where m.id = :id", new MapSqlParameterSource("id", id));
    }

    public Optional<ManualDTO> buscarPorHash(String hashSha256) {
        return umOuVazio(SELECT_BASE + " where m.hash_sha256 = :hash", new MapSqlParameterSource("hash", hashSha256));
    }

    public List<ManualDTO> listarTodos() {
        return jdbc.query(SELECT_BASE + " order by m.criado_em desc", MAPPER);
    }

    /**
     * Busca da área pública: título, nome do arquivo, categoria ou o próprio
     * texto do manual. strpos em vez de LIKE para não precisar escapar % e _
     * digitados pelo usuário.
     */
    public List<ManualDTO> listarPublicos(String termo, UUID categoriaId) {
        String sql = SELECT_BASE + " where " + FILTRO_PUBLICO + """
              and (cast(:categoriaId as uuid) is null or m.categoria_id = cast(:categoriaId as uuid))
              and (:termo = ''
                   or strpos(lower(m.titulo), :termo) > 0
                   or strpos(lower(m.nome_arquivo), :termo) > 0
                   or strpos(lower(coalesce(c.nome, '')), :termo) > 0
                   or exists (select 1 from manual_chunk mc
                              where mc.manual_id = m.id and strpos(lower(mc.conteudo), :termo) > 0))
            order by m.titulo
            """;
        return jdbc.query(sql, new MapSqlParameterSource()
            .addValue("termo", termo == null ? "" : termo.strip().toLowerCase())
            .addValue("categoriaId", categoriaId, Types.OTHER), MAPPER);
    }

    public Optional<ManualDTO> buscarPublicoPorId(UUID id) {
        return umOuVazio(SELECT_BASE + " where m.id = :id and " + FILTRO_PUBLICO, new MapSqlParameterSource("id", id));
    }

    /** O PDF original, para a ingestão (qualquer status). */
    public Optional<ArquivoPdf> buscarArquivo(UUID id) {
        List<ArquivoPdf> r = jdbc.query("select nome_arquivo, arquivo_pdf from manual where id = :id",
            new MapSqlParameterSource("id", id),
            (rs, rowNum) -> new ArquivoPdf(rs.getString("nome_arquivo"), rs.getBytes("arquivo_pdf")));
        return r.stream().findFirst();
    }

    public int atualizar(UUID id, String titulo, UUID categoriaId) {
        return jdbc.update("""
            update manual set titulo = :titulo, categoria_id = :categoriaId, atualizado_em = now()
            where id = :id
            """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("titulo", titulo)
                .addValue("categoriaId", categoriaId, Types.OTHER));
    }

    public int alterarAtivo(UUID id, boolean ativo) {
        return jdbc.update("update manual set ativo = :ativo, atualizado_em = now() where id = :id",
            new MapSqlParameterSource().addValue("id", id).addValue("ativo", ativo));
    }

    /** Os chunks vão junto (on delete cascade). */
    public int excluir(UUID id) {
        return jdbc.update("delete from manual where id = :id", new MapSqlParameterSource("id", id));
    }

    /**
     * Volta o manual para 'processando' — mas só se ele já não estiver, para
     * dois cliques em "reprocessar" não dispararem duas ingestões em paralelo.
     * Devolve false se já estava processando.
     */
    public boolean marcarProcessando(UUID id) {
        return jdbc.update("""
            update manual set status = 'processando', mensagem_erro = null, atualizado_em = now()
            where id = :id and status <> 'processando'
            """, new MapSqlParameterSource("id", id)) == 1;
    }

    public void marcarPronto(UUID id, int totalPaginas, int totalChunks) {
        jdbc.update("""
            update manual set status = 'pronto', mensagem_erro = null,
                   total_paginas = :paginas, total_chunks = :chunks, atualizado_em = now()
            where id = :id
            """,
            new MapSqlParameterSource()
                .addValue("id", id)
                .addValue("paginas", totalPaginas)
                .addValue("chunks", totalChunks));
    }

    public void marcarErro(UUID id, String mensagem) {
        jdbc.update("""
            update manual set status = 'erro', mensagem_erro = :mensagem, atualizado_em = now()
            where id = :id
            """,
            new MapSqlParameterSource().addValue("id", id).addValue("mensagem", mensagem));
    }

    /** Na subida da API: o que ficou 'processando' foi interrompido por um restart. */
    public int marcarInterrompidosComoErro() {
        return jdbc.update("""
            update manual set status = 'erro', atualizado_em = now(),
                   mensagem_erro = 'Processamento interrompido (o backend reiniciou). Clique em reprocessar.'
            where status = 'processando'
            """, new MapSqlParameterSource());
    }

    private Optional<ManualDTO> umOuVazio(String sql, MapSqlParameterSource params) {
        try {
            return Optional.ofNullable(jdbc.queryForObject(sql, params, MAPPER));
        } catch (EmptyResultDataAccessException e) {
            return Optional.empty();
        }
    }
}
