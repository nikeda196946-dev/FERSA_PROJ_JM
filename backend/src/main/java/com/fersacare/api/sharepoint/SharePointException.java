package com.fersacare.api.sharepoint;

/**
 * Falha ao falar com o Entra ID / Microsoft Graph. A mensagem já vem em
 * português e diz o que conferir (nunca inclui o secret) — vira 502 na API.
 */
public class SharePointException extends RuntimeException {
    public SharePointException(String mensagem) {
        super(mensagem);
    }

    public SharePointException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
