package com.fersacare.api.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatLivreRequest(
    @NotBlank(message = "a pergunta é obrigatória")
    @Size(max = 2000, message = "a pergunta pode ter no máximo 2000 caracteres")
    String pergunta
) {}
