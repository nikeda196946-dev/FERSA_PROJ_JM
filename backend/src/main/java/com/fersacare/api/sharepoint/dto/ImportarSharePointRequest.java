package com.fersacare.api.sharepoint.dto;

import java.util.List;
import java.util.UUID;

public record ImportarSharePointRequest(
    List<String> itemIds,           // vazio/null = todos os NOVOS e ALTERADOS
    UUID categoriaId,               // categoria dos manuais novos (opcional)
    Boolean categoriaPelaPasta      // true = usa o nome da subpasta como categoria (cria se não existir)
) {}
