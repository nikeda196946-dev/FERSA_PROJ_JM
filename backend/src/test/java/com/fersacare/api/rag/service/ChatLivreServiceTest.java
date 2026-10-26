package com.fersacare.api.rag.service;

import com.fersacare.api.ia.GeminiClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ChatLivreServiceTest {

    @Test
    void perguntaDiretoAoModeloSemManuaisNemEsquema() {
        GeminiClient gemini = mock(GeminiClient.class);
        when(gemini.gerar(ChatLivreService.INSTRUCAO_LIVRE, "Qual a capital da França?"))
            .thenReturn(new GeminiClient.Geracao("Paris.", "gemini-3.5-flash-lite"));

        var r = new ChatLivreService(gemini).perguntar("  Qual a capital da França?  ");

        assertThat(r.resposta()).isEqualTo("Paris.");
        assertThat(r.modelo()).isEqualTo("gemini-3.5-flash-lite");
        verify(gemini, never()).embedPergunta(anyString());
    }
}
