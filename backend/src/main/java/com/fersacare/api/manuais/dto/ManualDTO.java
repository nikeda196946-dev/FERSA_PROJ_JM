package com.fersacare.api.manuais.dto;

import com.fersacare.api.manuais.model.StatusManual;

import java.time.Instant;
import java.util.List;
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
    String sharepointUrl,       // link para abrir o arquivo no SharePoint (só origem = sharepoint)
    String sharepointCaminho,   // subpasta de onde veio, relativa à pasta sincronizada
    Instant criadoEm,
    Instant atualizadoEm,
    List<PecaVinculadaDTO> pecas  // peças (Part Numbers) de que o manual trata
) {
    public ManualDTO comPecas(List<PecaVinculadaDTO> novas) {
        return new ManualDTO(id, titulo, categoriaId, categoriaNome, nomeArquivo, tamanhoBytes, totalPaginas, totalChunks,
            status, mensagemErro, ativo, origem, sharepointUrl, sharepointCaminho, criadoEm, atualizadoEm,
            novas == null ? List.of() : novas);
    }
}
