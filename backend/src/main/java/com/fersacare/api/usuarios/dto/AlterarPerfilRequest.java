package com.fersacare.api.usuarios.dto;

import com.fersacare.api.usuarios.model.PerfilUsuario;
import jakarta.validation.constraints.NotNull;

public record AlterarPerfilRequest(
    @NotNull(message = "informe o perfil: MECANICO, TECNICO, ADMIN ou CLIENTE") PerfilUsuario perfil
) {}
