package com.fersacare.api.rag.service;

import com.fersacare.api.ia.GeminiClient;
import com.fersacare.api.rag.dto.ChatLivreResponse;
import org.springframework.stereotype.Service;

/**
 * Chat LIVRE, sem manuais — só para o admin testar a IA (tela "Testar IA").
 * Responde qualquer coisa com o conhecimento do próprio modelo. É o
 * contraponto do ChatService: comparando os dois na mesma pergunta dá para
 * ver por que o RAG existe (sem ele, o modelo pode inventar um torque).
 * Nunca é usado na área pública.
 */
@Service
public class ChatLivreService {

    static final String INSTRUCAO_LIVRE = """
        Você é um assistente de uso geral. Responda em português do Brasil, de forma
        direta e clara. Respostas curtas, a não ser que peçam detalhes.
        """;

    private final GeminiClient gemini;

    public ChatLivreService(GeminiClient gemini) {
        this.gemini = gemini;
    }

    public ChatLivreResponse perguntar(String pergunta) {
        GeminiClient.Geracao g = gemini.gerar(INSTRUCAO_LIVRE, pergunta.strip());
        return new ChatLivreResponse(g.texto(), g.modelo());
    }
}
