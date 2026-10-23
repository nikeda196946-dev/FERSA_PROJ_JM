package com.fersacare.api.rag.dto;

import java.util.UUID;

/** Um trecho de manual usado na resposta. numero = o [n] citado no texto da resposta. */
public record FonteDTO(
    int numero,
    UUID manualId,
    String tituloManual,
    Integer pagina,
    String trecho,
    double score
) {}
