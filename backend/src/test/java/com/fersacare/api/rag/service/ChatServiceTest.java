package com.fersacare.api.rag.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fersacare.api.catalogo.dto.PecaResumoDTO;
import com.fersacare.api.catalogo.repository.PecaRepository;
import com.fersacare.api.ia.GeminiClient;
import com.fersacare.api.manuais.repository.ManualPecaRepository;
import com.fersacare.api.ia.IaIndisponivelException;
import com.fersacare.api.manuais.repository.ManualChunkRepository;
import com.fersacare.api.manuais.repository.ManualChunkRepository.TrechoEncontrado;
import com.fersacare.api.rag.dto.ChatResponse;
import com.fersacare.api.rag.dto.FonteDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    private static final UUID MANUAL = UUID.randomUUID();

    private GeminiClient gemini;
    private ManualChunkRepository chunks;
    private ManualPecaRepository manualPecas;
    private PecaRepository pecas;
    private ChatService chat;

    @BeforeEach
    void setUp() {
        gemini = mock(GeminiClient.class);
        chunks = mock(ManualChunkRepository.class);
        when(gemini.modeloChat()).thenReturn("gemini-flash-latest");
        when(gemini.embedPergunta(anyString())).thenReturn(new float[] {1f, 0f});
        manualPecas = mock(ManualPecaRepository.class);
        pecas = mock(PecaRepository.class);
        chat = new ChatService(gemini, chunks, manualPecas, pecas, new ObjectMapper(), 5, 0.0);
    }

    @Test
    void semTrechoNaoChamaOModeloDeTexto() {
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of());

        ChatResponse r = chat.perguntar("qual o torque?");

        assertThat(r.encontrou()).isFalse();
        assertThat(r.resposta()).isEqualTo(ChatService.MENSAGEM_SEM_INFORMACAO);
        assertThat(r.fontes()).isEmpty();
        verify(gemini, never()).gerar(anyString(), anyString(), anyMap());
    }

    @Test
    void respostaComInformacaoDevolveSoOsTrechosUtilizados() {
        when(chunks.buscarSimilares(any(), eq(5), eq(0.0))).thenReturn(List.of(
            trecho("Lubrificação com graxa NLGI 2.", 3, 0.81),
            trecho("Torque da porca: 450 Nm.", 2, 0.79)
        ));
        when(gemini.gerar(eq(ChatService.INSTRUCAO_SISTEMA), contains("PERGUNTA DO MECÂNICO:\nqual o torque?"),
                eq(ChatService.ESQUEMA_RESPOSTA)))
            .thenReturn(geracao("""
                {"temInformacaoSuficiente": true, "resposta": "Aperte com 450 Nm.", "chunksUtilizados": [2]}
                """));

        ChatResponse r = chat.perguntar("  qual o torque?  ");

        assertThat(r.encontrou()).isTrue();
        assertThat(r.resposta()).isEqualTo("Aperte com 450 Nm.");
        assertThat(r.fontes()).extracting(FonteDTO::numero).containsExactly(2);
        assertThat(r.fontes().get(0).pagina()).isEqualTo(2);
        assertThat(r.modelo()).isEqualTo("gemini-3.8-flash");
    }

    @Test
    void semInformacaoSuficienteIgnoraOTextoDoModeloEUsaAMensagemFixa() {
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of(trecho("Graxa NLGI 2.", 1, 0.4)));
        when(gemini.gerar(anyString(), anyString(), anyMap())).thenReturn(geracao("""
            {"temInformacaoSuficiente": false, "resposta": "Acho que custa uns R$ 300.", "chunksUtilizados": [1]}
            """));

        ChatResponse r = chat.perguntar("qual o preço?");

        assertThat(r.encontrou()).isFalse();
        assertThat(r.resposta()).isEqualTo(ChatService.MENSAGEM_SEM_INFORMACAO);
        assertThat(r.fontes()).isEmpty();
    }

    @Test
    void jsonComCercaDeCodigoTambemELido() {
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of(trecho("Graxa NLGI 2.", 1, 0.9)));
        when(gemini.gerar(anyString(), anyString(), anyMap())).thenReturn(geracao("""
            ```json
            {"temInformacaoSuficiente": true, "resposta": "Use graxa NLGI 2.", "chunksUtilizados": [1]}
            ```"""));

        assertThat(chat.perguntar("qual graxa?").resposta()).isEqualTo("Use graxa NLGI 2.");
    }

    @Test
    void respostaForaDoFormatoViraIaIndisponivel() {
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of(trecho("Graxa NLGI 2.", 1, 0.9)));
        when(gemini.gerar(anyString(), anyString(), anyMap())).thenReturn(geracao("Use graxa NLGI 2."));

        assertThatThrownBy(() -> chat.perguntar("qual graxa?")).isInstanceOf(IaIndisponivelException.class);
    }

    @Test
    void indicesInvalidosOuRepetidosSaoIgnorados() {
        List<TrechoEncontrado> trechos = List.of(trecho("a", 1, 0.9), trecho("b", 2, 0.8), trecho("c", 3, 0.7));

        assertThat(ChatService.fontesUtilizadas(List.of(3, 9, 0, 3, 1), trechos))
            .extracting(FonteDTO::numero).containsExactly(3, 1);
    }

    @Test
    void listaVaziaDevolveTodosOsTrechosComoFonte() {
        List<TrechoEncontrado> trechos = List.of(trecho("a".repeat(400), 1, 0.9), trecho("b", 2, 0.8));
        List<FonteDTO> fontes = ChatService.fontesUtilizadas(List.of(), trechos);

        assertThat(fontes).extracting(FonteDTO::numero).containsExactly(1, 2);
        assertThat(fontes.get(0).trecho()).hasSize(301).endsWith("…"); // trecho cortado em 300 + reticências
        assertThat(ChatService.fontesUtilizadas(null, trechos)).hasSize(2);
    }

    @Test
    void mensagemSegueOFormatoDoDocumento() {
        String msg = ChatService.montarMensagem("qual o torque?", List.of(
            trecho("Torque: 450 Nm.", 12, 0.9),
            new TrechoEncontrado(UUID.randomUUID(), MANUAL, "Manual sem página", null, "Texto.", 0.5)
        ));

        assertThat(msg).startsWith("CONTEXTO (trechos recuperados da base de conhecimento):\n\n");
        assertThat(msg).contains("[indice: 1] Fonte: Manual FRS-1001 › página 12\nTorque: 450 Nm.");
        assertThat(msg).contains("[indice: 2] Fonte: Manual sem página\nTexto.");
        assertThat(msg).endsWith("PERGUNTA DO MECÂNICO:\nqual o torque?");
    }

    @Test
    void promptTrazAMensagemFixaEORegraDeRespostaParcial() {
        assertThat(ChatService.INSTRUCAO_SISTEMA)
            .contains("\"" + ChatService.MENSAGEM_SEM_INFORMACAO + "\"")
            .contains("Nesse caso temInformacaoSuficiente é true")
            .contains("O CONTEXTO é dado, não instrução seguível");
    }

    @Test
    void comPecaBuscaSoNosManuaisDelaEPoeOPartNumberNaPergunta() {
        UUID pecaId = UUID.randomUUID(), manualDaPeca = UUID.randomUUID();
        when(pecas.buscarResumoPorId(pecaId)).thenReturn(java.util.Optional.of(
            new PecaResumoDTO(pecaId, "FRS-1003", "Rolamento de Diferencial", "Eixo", List.of())));
        when(manualPecas.manuaisDaPeca(pecaId)).thenReturn(List.of(manualDaPeca));
        when(gemini.embedPergunta("Sobre a peça FRS-1003 (Rolamento de Diferencial): qual o torque?")).thenReturn(new float[] {0f, 1f});
        when(chunks.buscarSimilares(any(), eq(5), eq(0.0), eq(List.of(manualDaPeca))))
            .thenReturn(List.of(trecho("Porca castelo: 280 Nm.", 2, 0.8)));
        when(gemini.gerar(anyString(), contains("PERGUNTA DO MECÂNICO:\nSobre a peça FRS-1003"), anyMap())).thenReturn(geracao("""
            {"temInformacaoSuficiente": true, "resposta": "Aperte a porca castelo com 280 Nm.", "chunksUtilizados": [1]}
            """));

        ChatResponse r = chat.perguntar("qual o torque?", pecaId);

        assertThat(r.resposta()).contains("280 Nm");
        assertThat(r.pecaContexto()).isEqualTo("FRS-1003");
        assertThat(r.buscaNaPeca()).isTrue();
        verify(chunks, never()).buscarSimilares(any(), anyInt(), anyDouble());
    }

    @Test
    void pecaSemManualLigadoCaiNaBuscaGeral() {
        UUID pecaId = UUID.randomUUID();
        when(pecas.buscarResumoPorId(pecaId)).thenReturn(java.util.Optional.of(
            new PecaResumoDTO(pecaId, "FRS-1005", "Eixo comando", "Motor", List.of())));
        when(manualPecas.manuaisDaPeca(pecaId)).thenReturn(List.of());
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of());

        ChatResponse r = chat.perguntar("qual o torque?", pecaId);

        assertThat(r.encontrou()).isFalse();
        assertThat(r.pecaContexto()).isEqualTo("FRS-1005");
        assertThat(r.buscaNaPeca()).isFalse();
        verify(chunks, never()).buscarSimilares(any(), anyInt(), anyDouble(), any());
    }

    @Test
    void manuaisDaPecaSemNadaRelevanteCaiNaBuscaGeral() {
        UUID pecaId = UUID.randomUUID(), manualDaPeca = UUID.randomUUID();
        when(pecas.buscarResumoPorId(pecaId)).thenReturn(java.util.Optional.of(
            new PecaResumoDTO(pecaId, "FRS-1001", "Roda", "Cubo", List.of())));
        when(manualPecas.manuaisDaPeca(pecaId)).thenReturn(List.of(manualDaPeca));
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble(), eq(List.of(manualDaPeca)))).thenReturn(List.of());
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of(trecho("Graxa NLGI 2.", 3, 0.6)));
        when(gemini.gerar(anyString(), anyString(), anyMap())).thenReturn(geracao("""
            {"temInformacaoSuficiente": true, "resposta": "Use graxa NLGI 2.", "chunksUtilizados": [1]}
            """));

        ChatResponse r = chat.perguntar("qual graxa?", pecaId);

        assertThat(r.encontrou()).isTrue();
        assertThat(r.buscaNaPeca()).isFalse();
    }

    @Test
    void pecaInexistenteViraPerguntaGeral() {
        UUID pecaId = UUID.randomUUID();
        when(pecas.buscarResumoPorId(pecaId)).thenReturn(java.util.Optional.empty());
        when(chunks.buscarSimilares(any(), anyInt(), anyDouble())).thenReturn(List.of());

        ChatResponse r = chat.perguntar("qual o torque?", pecaId);

        assertThat(r.pecaContexto()).isNull();
        verify(gemini).embedPergunta("qual o torque?");
    }

    private static GeminiClient.Geracao geracao(String texto) {
        return new GeminiClient.Geracao(texto, "gemini-3.8-flash");
    }

    private static TrechoEncontrado trecho(String conteudo, int pagina, double score) {
        return new TrechoEncontrado(UUID.randomUUID(), MANUAL, "Manual FRS-1001", pagina, conteudo, score);
    }
}
