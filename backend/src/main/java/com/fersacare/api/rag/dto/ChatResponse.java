package com.fersacare.api.rag.dto;

import java.util.List;

public record ChatResponse(
    String resposta,
    boolean encontrou,          // false = "não encontrei nos manuais"
    List<FonteDTO> fontes,      // vazio quando encontrou = false
    String modelo,
    String pecaContexto,        // Part Number da peça escolhida (null = pergunta geral)
    boolean buscaNaPeca         // true = trechos vieram só dos manuais ligados à peça
) {}
