package com.fersacare.api.manuais.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
    @NotBlank(message = "o nome é obrigatório") @Size(max = 100) String nome,
    @Size(max = 500) String descricao,
    Boolean ativo                 // opcional — padrão true
) {}
