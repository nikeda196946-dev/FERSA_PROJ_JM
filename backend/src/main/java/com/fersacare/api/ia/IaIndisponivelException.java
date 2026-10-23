package com.fersacare.api.ia;

/** O Gemini não respondeu (chave ausente, cota estourada, timeout, erro do serviço). Vira 503 na API. */
public class IaIndisponivelException extends RuntimeException {

    /** true = vale tentar outro modelo (sobrecarga, cota, timeout, modelo aposentado). */
    private final boolean tentarOutroModelo;

    public IaIndisponivelException(String mensagem) {
        this(mensagem, null, false);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        this(mensagem, causa, false);
    }

    public IaIndisponivelException(String mensagem, Throwable causa, boolean tentarOutroModelo) {
        super(mensagem, causa);
        this.tentarOutroModelo = tentarOutroModelo;
    }

    public boolean tentarOutroModelo() {
        return tentarOutroModelo;
    }
}
