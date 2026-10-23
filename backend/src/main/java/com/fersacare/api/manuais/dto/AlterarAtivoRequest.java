package com.fersacare.api.manuais.dto;

import jakarta.validation.constraints.NotNull;

public record AlterarAtivoRequest(
    @NotNull(message = "informe ativo = true ou false") Boolean ativo
) {}
