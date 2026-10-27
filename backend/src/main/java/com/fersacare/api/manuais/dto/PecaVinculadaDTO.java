package com.fersacare.api.manuais.dto;

import java.util.UUID;

/** Peça ligada a um manual. origem: automatico (achado no texto) | manual (o admin ligou). */
public record PecaVinculadaDTO(
    UUID id,
    String partNumber,
    String nome,
    String origem,
    int ocorrencias      // quantas vezes o Part Number aparece no manual (0 quando ligado à mão)
) {}
