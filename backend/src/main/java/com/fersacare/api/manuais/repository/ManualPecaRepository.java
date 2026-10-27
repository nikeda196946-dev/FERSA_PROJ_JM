package com.fersacare.api.manuais.repository;

import com.fersacare.api.manuais.dto.PecaVinculadaDTO;
import com.fersacare.api.manuais.service.PartNumberDetector;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tabela `manual_peca`. Regras de origem (ver database/manuais.sql):
 *   - a detecção só mexe nos 'automatico' e nunca religa um 'removido';
 *   - o admin decide o conjunto final: marcado = ligado ('manual', ou
 *     mantém 'automatico'), desmarcado = 'removido'.
 * 'removido' nunca aparece nas consultas de vínculo.
 */
@Repository
public class ManualPecaRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public ManualPecaRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Texto de um manual já processado, para refazer a detecção sem reprocessar o PDF. */
    public record TextoManual(UUID manualId, String texto) {}

    public List<PartNumberDetector.Peca> listarPecas() {
        return jdbc.query("select id, part_number from pecas", new MapSqlParameterSource(),
            (rs, rowNum) -> new PartNumberDetector.Peca(UUID.fromString(rs.getString("id")), rs.getString("part_number")));
    }

    /** Troca os vínculos automáticos do manual pelos detectados agora. Chamar dentro de transação. */
    public void substituirAutomaticos(UUID manualId, Map<UUID, Integer> detectados) {
        jdbc.update("delete from manual_peca where manual_id = :id and origem = 'automatico'",
            new MapSqlParameterSource("id", manualId));
        // manual/removido já existentes ficam como estão; num 'manual' só atualiza a contagem
        detectados.forEach((pecaId, n) -> jdbc.update("""
            insert into manual_peca (manual_id, peca_id, origem, ocorrencias)
            values (:manualId, :pecaId, 'automatico', :n)
            on conflict (manual_id, peca_id) do update set ocorrencias = excluded.ocorrencias
            """,
            new MapSqlParameterSource().addValue("manualId", manualId).addValue("pecaId", pecaId).addValue("n", n)));
    }

    /** O admin escolheu exatamente estas peças para o manual. */
    public void definirVinculos(UUID manualId, Set<UUID> pecaIds) {
        MapSqlParameterSource p = new MapSqlParameterSource("manualId", manualId);
        jdbc.update("update manual_peca set origem = 'removido' where manual_id = :manualId"
            + (pecaIds.isEmpty() ? "" : " and peca_id not in (:ids)"),
            p.addValue("ids", pecaIds.isEmpty() ? null : pecaIds));
        for (UUID pecaId : pecaIds) {
            jdbc.update("""
                insert into manual_peca (manual_id, peca_id, origem) values (:manualId, :pecaId, 'manual')
                on conflict (manual_id, peca_id) do update
                  set origem = case when manual_peca.origem = 'removido' then 'manual' else manual_peca.origem end
                """,
                new MapSqlParameterSource().addValue("manualId", manualId).addValue("pecaId", pecaId));
        }
    }

    /** Peças ligadas a cada manual, numa consulta só. */
    public Map<UUID, List<PecaVinculadaDTO>> listarPorManuais(Collection<UUID> manualIds) {
        Map<UUID, List<PecaVinculadaDTO>> porManual = new HashMap<>();
        if (manualIds.isEmpty()) return porManual;
        jdbc.query("""
            select mp.manual_id, p.id, p.part_number, p.nome, mp.origem, mp.ocorrencias
            from manual_peca mp join pecas p on p.id = mp.peca_id
            where mp.manual_id in (:ids) and mp.origem <> 'removido'
            order by p.part_number
            """,
            new MapSqlParameterSource("ids", manualIds),
            rs -> {
                porManual.computeIfAbsent(UUID.fromString(rs.getString("manual_id")), k -> new java.util.ArrayList<>())
                    .add(new PecaVinculadaDTO(UUID.fromString(rs.getString("id")), rs.getString("part_number"),
                        rs.getString("nome"), rs.getString("origem"), rs.getInt("ocorrencias")));
            });
        return porManual;
    }

    /** Manuais visíveis ao público (ativo, pronto, categoria ativa) ligados à peça. */
    public List<UUID> manuaisDaPeca(UUID pecaId) {
        return jdbc.query("""
            select m.id from manual_peca mp
            join manual m on m.id = mp.manual_id
            left join categoria c on c.id = m.categoria_id
            where mp.peca_id = :pecaId and mp.origem <> 'removido'
              and m.ativo and m.status = 'pronto' and (m.categoria_id is null or c.ativo)
            """,
            new MapSqlParameterSource("pecaId", pecaId),
            (rs, rowNum) -> UUID.fromString(rs.getString("id")));
    }

    /** Título + nome do arquivo + texto dos trechos de cada manual pronto. */
    public List<TextoManual> textosDosManuaisProntos() {
        Map<UUID, String> textos = new LinkedHashMap<>();
        jdbc.query("""
            select m.id, m.titulo || ' ' || m.nome_arquivo || ' ' || coalesce(string_agg(mc.conteudo, ' ' order by mc.ordem), '') as texto
            from manual m left join manual_chunk mc on mc.manual_id = m.id
            where m.status = 'pronto'
            group by m.id, m.titulo, m.nome_arquivo
            """,
            new MapSqlParameterSource(),
            rs -> { textos.put(UUID.fromString(rs.getString("id")), rs.getString("texto")); });
        return textos.entrySet().stream().map(e -> new TextoManual(e.getKey(), e.getValue())).toList();
    }
}
