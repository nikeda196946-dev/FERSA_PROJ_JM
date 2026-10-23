package com.fersacare.api.manuais.dto;

import com.fersacare.api.manuais.model.StatusManual;

import java.time.Instant;
import java.util.UUID;

public record ManualDTO(
    UUID id,
    String titulo,
    UUID categoriaId,
    String categoriaNome,       // nulo se o manual está sem categoria
    String nomeArquivo,
    long tamanhoBytes,
    Integer totalPaginas,       // nulo até o PDF ser lido
    int totalChunks,
    StatusManual status,
    String mensagemErro,        // só quando status = ERRO
    boolean ativo,
    String origem,              // upload | sharepoint
    Instant criadoEm,
    Instant atualizadoEm
) {}
