package com.fersacare.api.sharepoint.dto;

import java.util.UUID;

public record ResultadoImportacaoDTO(
    String itemId,
    String nome,
    Resultado resultado,
    String mensagem,
    UUID manualId
) {
    public enum Resultado { IMPORTADO, ATUALIZADO, VINCULADO, SEM_ALTERACAO, IGNORADO, ERRO }
}
