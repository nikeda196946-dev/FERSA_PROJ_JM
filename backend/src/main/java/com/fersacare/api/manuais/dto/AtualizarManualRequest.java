package com.fersacare.api.manuais.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AtualizarManualRequest(
    @NotBlank(message = "o título é obrigatório") @Size(max = 200) String titulo,
    UUID categoriaId              // null = sem categoria
) {}
