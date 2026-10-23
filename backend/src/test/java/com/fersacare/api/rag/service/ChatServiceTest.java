package com.fersacare.api.rag.service;

import com.fersacare.api.ia.GeminiClient;
import com.fersacare.api.manuais.repository.ManualChunkRepository;
import com.fersacare.api.manuais.repository.ManualChunkRepository.TrechoEncontrado;
import com.fersacare.api.rag.dto.ChatResponse;
import com.fersacare.api.rag.dto.FonteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private static final UUID MANUAL = UUID.randomUUID();

    private GeminiClient gemini;
    private ManualChunkRepository chunks;
    private ChatService chat;

    @BeforeEach
    void setUp() {
        gemini = mock(GeminiClient.class);
        chunks = mock(ManualChunkRepository.class);
        when(gemini.modeloChat()).thenReturn("gemini-flash-latest");
        when(gemini.embedPergunta(anyString())).thenReturn(new float[] {1f, 0f});
        chat = new ChatService(gemini, chunks, 5, 0.0);
    }

    @Test
    void semTrechoNaoChamaOModeloDeTexto() {
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of());

        ChatResponse r = chat.perguntar("qual o torque?");

        assertThat(r.encontrou()).isFalse();
        assertThat(r.resposta()).isEqualTo(ChatService.NAO_ENCONTREI);
        assertThat(r.fontes()).isEmpty();
        verify(gemini, never()).gerar(anyString(), anyString());
    }

    @Test
    void respostaComCitacaoDevolveSoAsFontesCitadas() {
        when(chunks.buscarSimilares(any(), eq(5), eq(0.0))).thenReturn(List.of(
            trecho("Lubrificação com graxa NLGI 2.", 3, 0.81),
            trecho("Torque da porca: 450 Nm.", 2, 0.79)
        ));
        when(gemini.gerar(eq(ChatService.INSTRUCAO_SISTEMA), contains("Pergunta: qual o torque?")))
            .thenReturn(new GeminiClient.Geracao("Aplique 450 Nm [2].", "gemini-3.8-flash"));

        ChatResponse r = chat.perguntar("  qual o torque?  ");

        assertThat(r.encontrou()).isTrue();
        assertThat(r.resposta()).isEqualTo("Aplique 450 Nm [2].");
        assertThat(r.fontes()).extracting(FonteDTO::numero).containsExactly(2);
        assertThat(r.fontes().get(0).pagina()).isEqualTo(2);
        assertThat(r.modelo()).isEqualTo("gemini-3.8-flash");
    }

    @Test
    void modeloDizendoQueNaoEncontrouViraEncontrouFalse() {
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of(trecho("Graxa NLGI 2.", 1, 0.4)));
        when(gemini.gerar(anyString(), anyString()))
            .thenReturn(new GeminiClient.Geracao("\"" + ChatService.NAO_ENCONTREI + "\"", "gemini-3.8-flash"));

        ChatResponse r = chat.perguntar("qual a melhor pizza?");

        assertThat(r.encontrou()).isFalse();
        assertThat(r.fontes()).isEmpty();
    }

    @Test
    void semCitacaoDevolveTodosOsTrechosComoFonte() {
        List<TrechoEncontrado> trechos = List.of(trecho("a".repeat(400), 1, 0.9), trecho("b", 2, 0.8));
        List<FonteDTO> fontes = ChatService.fontesCitadas("Resposta sem colchetes. [9] não existe.", trechos);

        assertThat(fontes).extracting(FonteDTO::numero).containsExactly(1, 2);
        assertThat(fontes.get(0).trecho()).hasSize(301).endsWith("…"); // trecho cortado em 300 + reticências
    }

    @Test
    void mensagemNumeraOsTrechosComManualEPagina() {
        String msg = ChatService.montarMensagem("qual o torque?", List.of(
            trecho("Torque: 450 Nm.", 12, 0.9),
            new TrechoEncontrado(UUID.randomUUID(), MANUAL, "Manual sem página", null, "Texto.", 0.5)
        ));

        assertThat(msg).contains("[1] Manual: Manual FRS-1001 — página 12\nTorque: 450 Nm.");
        assertThat(msg).contains("[2] Manual: Manual sem página\nTexto.");
        assertThat(msg).endsWith("Pergunta: qual o torque?");
    }

    private static TrechoEncontrado trecho(String conteudo, int pagina, double score) {
        return new TrechoEncontrado(UUID.randomUUID(), MANUAL, "Manual FRS-1001", pagina, conteudo, score);
    }
}
