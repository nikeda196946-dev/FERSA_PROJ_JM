package com.fersacare.api.ia;

/** O Gemini não respondeu (chave ausente, cota estourada, timeout, erro do serviço). Vira 503 na API. */
public class IaIndisponivelException extends RuntimeException {
    public IaIndisponivelException(String mensagem) {
        super(mensagem);
    }

    public IaIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
