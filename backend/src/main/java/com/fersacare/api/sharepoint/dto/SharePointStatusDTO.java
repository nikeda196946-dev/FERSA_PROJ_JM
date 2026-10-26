package com.fersacare.api.sharepoint.dto;

import java.time.Instant;

/** O que o CMS mostra sobre a conexão. Nunca inclui o secret. */
public record SharePointStatusDTO(
    boolean configurado,
    String siteUrl,
    String pasta,
    int sincronizacaoMinutos,          // 0 = sincronização automática desligada
    Instant ultimaSincronizacao,       // null = ainda não rodou desde que a API subiu
    String resumoUltimaSincronizacao,
    String faltando                    // o que falta configurar, quando configurado = false
) {}
