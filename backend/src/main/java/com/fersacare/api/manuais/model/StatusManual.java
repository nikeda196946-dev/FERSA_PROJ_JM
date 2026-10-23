package com.fersacare.api.manuais.model;

/**
 * Ciclo de vida de um manual na ingestão. No banco fica em minúsculo
 * ('processando', 'pronto', 'erro' — ver check em database/manuais.sql);
 * na API sai como o nome do enum.
 */
public enum StatusManual {
    PROCESSANDO,
    PRONTO,
    ERRO;

    public String noBanco() {
        return name().toLowerCase();
    }

    public static StatusManual doBanco(String valor) {
        return valueOf(valor.toUpperCase());
    }
}
