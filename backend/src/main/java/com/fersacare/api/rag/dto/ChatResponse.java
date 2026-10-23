package com.fersacare.api.rag.dto;

import java.util.List;

public record ChatResponse(
    String resposta,
    boolean encontrou,          // false = "não encontrei nos manuais"
    List<FonteDTO> fontes,      // vazio quando encontrou = false
    String modelo
) {}
