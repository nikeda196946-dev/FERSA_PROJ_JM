package com.fersacare.api.sharepoint.dto;

import com.fersacare.api.manuais.model.StatusManual;

import java.time.Instant;
import java.util.UUID;

public record ArquivoSharePointDTO(
    String itemId,
    String nome,
    String caminho,                 // subpasta relativa à pasta sincronizada ("" = a própria pasta)
    long tamanhoBytes,
    Instant modificadoEm,
    String webUrl,
    SituacaoArquivo situacao,
    UUID manualId,                  // quando já importado
    String manualTitulo,
    StatusManual manualStatus
) {
    public enum SituacaoArquivo { NOVO, IMPORTADO, ALTERADO, GRANDE_DEMAIS }
}
