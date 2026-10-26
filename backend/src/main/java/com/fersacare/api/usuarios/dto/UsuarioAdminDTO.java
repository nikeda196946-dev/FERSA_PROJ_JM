package com.fersacare.api.usuarios.dto;

import com.fersacare.api.usuarios.model.PerfilUsuario;

import java.time.Instant;
import java.util.UUID;

/** Linha da tela Usuários do CMS: conta do Supabase Auth + perfil do FERSA Care. */
public record UsuarioAdminDTO(
    UUID id,
    String nome,
    String email,
    PerfilUsuario perfil,      // null = conta sem linha em profiles (criada antes do trigger de cadastro)
    Instant criadoEm,
    Instant ultimoAcesso,      // null = nunca entrou
    boolean emailConfirmado
) {}
