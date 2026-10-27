package com.fersacare.api.rag.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChatRequest(
    @NotBlank(message = "a pergunta é obrigatória")
    @Size(max = 1000, message = "a pergunta pode ter no máximo 1000 caracteres")
    String pergunta,
    UUID pecaId          // opcional — prioriza os manuais ligados a esta peça
) {}
