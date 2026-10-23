package com.fersacare.api.ia;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Confere o JSON que vai para o Gemini e a leitura da resposta, sem rede (MockRestServiceServer). */
class GeminiClientTest {

    private static final String BASE = "https://gemini.teste/v1beta";

    private MockRestServiceServer servidor;
    private GeminiClient cliente;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE).defaultHeader("x-goog-api-key", "chave");
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new GeminiClient(builder.build(), "chave", "gemini-embedding-001", 3,
            "gemini-flash-latest", "gemini-flash-lite-latest", "low");
    }

    @Test
    void embedDocumentosMandaTaskTypeTituloEDimensao() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-embedding-001:batchEmbedContents"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header("x-goog-api-key", "chave"))
            .andExpect(jsonPath("$.requests.length()").value(2))
            .andExpect(jsonPath("$.requests[0].model").value("models/gemini-embedding-001"))
            .andExpect(jsonPath("$.requests[0].taskType").value("RETRIEVAL_DOCUMENT"))
            .andExpect(jsonPath("$.requests[0].title").value("Manual FRS-1001"))
            .andExpect(jsonPath("$.requests[0].outputDimensionality").value(3))
            .andExpect(jsonPath("$.requests[1].content.parts[0].text").value("trecho 2"))
            .andRespond(withSuccess("""
                {"embeddings": [{"values": [0.1, 0.2, 0.3]}, {"values": [0.4, 0.5, 0.6]}]}
                """, MediaType.APPLICATION_JSON));

        List<float[]> vetores = cliente.embedDocumentos("Manual FRS-1001", List.of("trecho 1", "trecho 2"));

        assertThat(vetores).hasSize(2);
        assertThat(vetores.get(1)).containsExactly(0.4f, 0.5f, 0.6f);
        servidor.verify();
    }

    @Test
    void embedDocumentosQuebraEmLotesDe100() {
        List<String> textos = IntStream.range(0, 150).mapToObj(i -> "t" + i).toList();
        servidor.expect(once(), requestTo(BASE + "/models/gemini-embedding-001:batchEmbedContents"))
            .andExpect(jsonPath("$.requests.length()").value(100))
            .andRespond(withSuccess(respostaEmbeddings(100), MediaType.APPLICATION_JSON));
        servidor.expect(once(), requestTo(BASE + "/models/gemini-embedding-001:batchEmbedContents"))
            .andExpect(jsonPath("$.requests.length()").value(50))
            .andRespond(withSuccess(respostaEmbeddings(50), MediaType.APPLICATION_JSON));

        assertThat(cliente.embedDocumentos("M", textos)).hasSize(150);
        servidor.verify();
    }

    @Test
    void embedPerguntaUsaRetrievalQuerySemTitulo() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-embedding-001:batchEmbedContents"))
            .andExpect(jsonPath("$.requests[0].taskType").value("RETRIEVAL_QUERY"))
            .andExpect(jsonPath("$.requests[0].title").doesNotExist())
            .andRespond(withSuccess(respostaEmbeddings(1), MediaType.APPLICATION_JSON));

        assertThat(cliente.embedPergunta("qual o torque?")).hasSize(3);
    }

    @Test
    void dimensaoErradaFalhaComMensagemClara() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-embedding-001:batchEmbedContents"))
            .andRespond(withSuccess("{\"embeddings\": [{\"values\": [0.1, 0.2]}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.embedPergunta("x"))
            .isInstanceOf(IaIndisponivelException.class)
            .hasMessageContaining("dimensão 2");
    }

    @Test
    void gerarMandaInstrucaoDeSistemaELeOTexto() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-latest:generateContent"))
            .andExpect(jsonPath("$.systemInstruction.parts[0].text").value("regras"))
            .andExpect(jsonPath("$.systemInstruction.parts[0].thought").doesNotExist())
            .andExpect(jsonPath("$.contents[0].role").value("user"))
            .andExpect(jsonPath("$.contents[0].parts[0].text").value("pergunta"))
            .andExpect(jsonPath("$.generationConfig.temperature").value(0.2))
            .andExpect(jsonPath("$.generationConfig.thinkingConfig.thinkingLevel").value("low"))
            .andRespond(withSuccess("""
                {"candidates": [{"content": {"role": "model", "parts": [
                    {"text": "pensando...", "thought": true},
                    {"text": "Aplique 450 Nm ", "thoughtSignature": "abc"},
                    {"text": "[1]."}]},
                  "finishReason": "STOP"}],
                 "usageMetadata": {"totalTokenCount": 10}, "modelVersion": "gemini-3.8-flash"}
                """, MediaType.APPLICATION_JSON));

        GeminiClient.Geracao g = cliente.gerar("regras", "pergunta");

        assertThat(g.texto()).isEqualTo("Aplique 450 Nm [1].");
        assertThat(g.modelo()).isEqualTo("gemini-3.8-flash");
    }

    @Test
    void principalSobrecarregadoCaiNoReserva() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-latest:generateContent"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": {\"message\": \"This model is currently experiencing high demand.\"}}"));
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-lite-latest:generateContent"))
            .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"ok\"}]}}]}",
                MediaType.APPLICATION_JSON));

        GeminiClient.Geracao g = cliente.gerar("r", "p");

        assertThat(g.texto()).isEqualTo("ok");
        assertThat(g.modelo()).isEqualTo("gemini-flash-lite-latest"); // sem modelVersion, fica o nome pedido
        servidor.verify();
    }

    @Test
    void timeoutLendoORespostaCaiNoReserva() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-latest:generateContent"))
            .andRespond(request -> {
                // simula o Gemini mandando o status 200 e travando no meio do corpo
                MockClientHttpResponse resposta = new MockClientHttpResponse(new InputStream() {
                    @Override
                    public int read() throws IOException {
                        throw new SocketTimeoutException("Read timed out");
                    }
                }, HttpStatus.OK);
                resposta.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                return resposta;
            });
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-lite-latest:generateContent"))
            .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"ok\"}]}}]}",
                MediaType.APPLICATION_JSON));

        assertThat(cliente.gerar("r", "p").texto()).isEqualTo("ok");
        servidor.verify();
    }

    @Test
    void modeloAposentadoCaiNoReserva() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-latest:generateContent"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": {\"message\": \"no longer available\"}}"));
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-lite-latest:generateContent"))
            .andRespond(withSuccess("{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"ok\"}]}}]}",
                MediaType.APPLICATION_JSON));

        assertThat(cliente.gerar("r", "p").texto()).isEqualTo("ok");
        servidor.verify();
    }

    @Test
    void gerarSemTextoFalha() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-latest:generateContent"))
            .andRespond(withSuccess("{\"candidates\": [{\"finishReason\": \"SAFETY\"}]}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.gerar("r", "p"))
            .isInstanceOf(IaIndisponivelException.class)
            .hasMessageContaining("SAFETY");
    }

    @Test
    void erroDefinitivoNaoTentaDeNovoNemUsaReserva() {
        servidor.expect(once(), requestTo(BASE + "/models/gemini-flash-latest:generateContent"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN)
                .contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\": {\"message\": \"API key not valid\"}}"));

        assertThatThrownBy(() -> cliente.gerar("r", "p"))
            .isInstanceOf(IaIndisponivelException.class)
            .hasMessageContaining("403")
            .hasMessageContaining("API key not valid");
        servidor.verify();
    }

    @Test
    void semChaveNemTentaChamar() {
        GeminiClient semChave = new GeminiClient(RestClient.create(), " ", "m", 3, "c", "", "");
        assertThatThrownBy(() -> semChave.embedPergunta("x"))
            .isInstanceOf(IaIndisponivelException.class)
            .hasMessageContaining("GEMINI_API_KEY");
    }

    private static String respostaEmbeddings(int n) {
        return "{\"embeddings\": [" + String.join(",", Collections.nCopies(n, "{\"values\": [1, 0, 0]}")) + "]}";
    }
}
