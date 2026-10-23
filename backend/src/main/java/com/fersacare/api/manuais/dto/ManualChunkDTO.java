package com.fersacare.api.manuais.dto;

import java.util.UUID;

/** Para o admin conferir a ingestão: o texto de cada chunk e o tamanho do vetor gravado. */
public record ManualChunkDTO(
    UUID id,
    int ordem,
    Integer pagina,
    String conteudo,
    int dimensoesEmbedding
) {}
