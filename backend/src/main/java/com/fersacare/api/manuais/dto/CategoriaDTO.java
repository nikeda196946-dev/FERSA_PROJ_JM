package com.fersacare.api.manuais.dto;

import java.time.Instant;
import java.util.UUID;

public record CategoriaDTO(
    UUID id,
    String nome,
    String descricao,
    boolean ativo,
    int totalManuais,
    Instant criadoEm
) {}
