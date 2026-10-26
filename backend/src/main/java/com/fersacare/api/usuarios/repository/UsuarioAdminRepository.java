package com.fersacare.api.usuarios.repository;

import com.fersacare.api.usuarios.dto.UsuarioAdminDTO;
import com.fersacare.api.usuarios.model.PerfilUsuario;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contas do Supabase Auth (auth.users) com o perfil do FERSA Care. O
 * backend conecta como o usuário do banco, que pode ler auth.users; o
 * front nunca lê essa tabela direto.
 *
 * O left join mostra também quem tem conta mas ainda não tem profile
 * (cadastrado antes de database/cadastro.sql existir) — o admin resolve
 * isso escolhendo um perfil na tela.
 */
@Repository
public class UsuarioAdminRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public UsuarioAdminRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final String SELECT_BASE = """
        select u.id, coalesce(p.nome, nullif(trim(u.raw_user_meta_data ->> 'nome'), ''), split_part(u.email, '@', 1)) as nome,
               u.email, p.perfil, u.created_at, u.last_sign_in_at, u.email_confirmed_at
        from auth.users u
        left join profiles p on p.id = u.id
        """;

    private static final RowMapper<UsuarioAdminDTO> MAPPER = (rs, rowNum) -> new UsuarioAdminDTO(
        UUID.fromString(rs.getString("id")),
        rs.getString("nome"),
        rs.getString("email"),
        rs.getString("perfil") != null ? PerfilUsuario.valueOf(rs.getString("perfil")) : null,
        instante(rs.getTimestamp("created_at")),
        instante(rs.getTimestamp("last_sign_in_at")),
        rs.getTimestamp("email_confirmed_at") != null
    );

    public List<UsuarioAdminDTO> listar() {
        return jdbc.query(SELECT_BASE + " order by u.created_at desc", MAPPER);
    }

    public Optional<UsuarioAdminDTO> buscarPorId(UUID id) {
        return jdbc.query(SELECT_BASE + " where u.id = :id", new MapSqlParameterSource("id", id), MAPPER).stream().findFirst();
    }

    /** Cria o profile se não existir (com o nome do cadastro) ou só troca o perfil. */
    public void definirPerfil(UUID id, PerfilUsuario perfil) {
        jdbc.update("""
            insert into profiles (id, nome, perfil)
            select u.id, coalesce(nullif(trim(u.raw_user_meta_data ->> 'nome'), ''), split_part(u.email, '@', 1)),
                   cast(:perfil as perfil_usuario)
            from auth.users u where u.id = :id
            on conflict (id) do update set perfil = excluded.perfil
            """,
            new MapSqlParameterSource().addValue("id", id).addValue("perfil", perfil.name()));
    }

    private static java.time.Instant instante(Timestamp t) {
        return t == null ? null : t.toInstant();
    }
}
