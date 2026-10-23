package com.fersacare.api.ia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Fala com a API REST do Gemini (generativelanguage.googleapis.com) para
 * duas coisas:
 *
 * 1) Embeddings ({@link #embedDocumentos} na ingestão, {@link #embedPergunta}
 *    no chat). O taskType muda entre os dois — RETRIEVAL_DOCUMENT para os
 *    trechos dos manuais e RETRIEVAL_QUERY para a pergunta — porque o modelo
 *    gera vetores otimizados para "pergunta casa com trecho que responde",
 *    o que melhora a busca em relação a usar o mesmo tipo dos dois lados.
 * 2) Geração de texto ({@link #gerar}) com o modelo Flash.
 *
 * Na geração há um modelo reserva: os modelos Flash mais novos às vezes
 * respondem 503 ("high demand"), demoram demais ou são aposentados (o
 * gemini-2.5-flash passou a dar 404 para contas novas). Nesses casos o
 * chat tenta o reserva em vez de deixar o usuário esperando ou sem resposta.
 *
 * Chamada direta por HTTP em vez de SDK: são dois endpoints, o JSON é
 * pequeno, e assim fica visível exatamente o que vai para o Google.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    /** Limite da API: no máximo 100 textos por chamada de batchEmbedContents. */
    static final int LOTE_EMBEDDING = 100;
    /** Embeddings rodam em segundo plano na ingestão: dá para insistir mais. */
    private static final int TENTATIVAS_EMBEDDING = 4;

    /** Texto gerado + o modelo que efetivamente respondeu (ex: "gemini-3.8-flash"). */
    public record Geracao(String texto, String modelo) {}

    private final RestClient http;
    private final boolean chaveConfigurada;
    private final String modeloEmbedding;
    private final int dimensoes;
    private final String modeloChat;
    private final String modeloChatReserva;
    private final String nivelRaciocinio;

    public GeminiClient(
        @Qualifier("geminiRestClient") RestClient http,
        @Value("${fersa.gemini.api-key}") String apiKey,
        @Value("${fersa.gemini.modelo-embedding}") String modeloEmbedding,
        @Value("${fersa.gemini.embedding-dimensoes}") int dimensoes,
        @Value("${fersa.gemini.modelo-chat}") String modeloChat,
        @Value("${fersa.gemini.modelo-chat-reserva:}") String modeloChatReserva,
        @Value("${fersa.gemini.nivel-raciocinio:}") String nivelRaciocinio
    ) {
        this.http = http;
        this.chaveConfigurada = apiKey != null && !apiKey.isBlank();
        this.modeloEmbedding = modeloEmbedding;
        this.dimensoes = dimensoes;
        this.modeloChat = modeloChat;
        this.modeloChatReserva = modeloChatReserva == null || modeloChatReserva.isBlank() ? null : modeloChatReserva.strip();
        this.nivelRaciocinio = nivelRaciocinio == null || nivelRaciocinio.isBlank() ? null : nivelRaciocinio.strip();
    }

    public String modeloChat() {
        return modeloChat;
    }

    /** Embeddings dos trechos de um manual, na mesma ordem de {@code textos}. */
    public List<float[]> embedDocumentos(String tituloManual, List<String> textos) {
        List<float[]> resultado = new ArrayList<>(textos.size());
        for (int inicio = 0; inicio < textos.size(); inicio += LOTE_EMBEDDING) {
            List<String> lote = textos.subList(inicio, Math.min(inicio + LOTE_EMBEDDING, textos.size()));
            resultado.addAll(embedLote(lote, "RETRIEVAL_DOCUMENT", tituloManual));
        }
        return resultado;
    }

    public float[] embedPergunta(String pergunta) {
        return embedLote(List.of(pergunta), "RETRIEVAL_QUERY", null).get(0);
    }

    /**
     * Gera a resposta do chat. O modelo principal tem uma tentativa; se ele
     * estiver sobrecarregado, lento ou indisponível, o reserva tem duas.
     * Erro que outro modelo não resolveria (chave inválida, pedido mal
     * formado) falha na hora.
     */
    public Geracao gerar(String instrucaoSistema, String mensagemUsuario) {
        GenerateRequest corpo = new GenerateRequest(
            new Content(null, List.of(new Part(instrucaoSistema, null))),
            List.of(new Content("user", List.of(new Part(mensagemUsuario, null)))),
            new GenerationConfig(0.2, nivelRaciocinio == null ? null : new ThinkingConfig(nivelRaciocinio))
        );

        try {
            return gerarCom(modeloChat, corpo, modeloChatReserva == null ? 2 : 1);
        } catch (IaIndisponivelException e) {
            if (modeloChatReserva == null || modeloChatReserva.equals(modeloChat) || !e.tentarOutroModelo()) {
                throw e;
            }
            log.warn("Modelo {} indisponível ({}); usando o reserva {}", modeloChat, e.getMessage(), modeloChatReserva);
            return gerarCom(modeloChatReserva, corpo, 2);
        }
    }

    private Geracao gerarCom(String modelo, GenerateRequest corpo, int tentativas) {
        GenerateResponse resposta = chamar(() -> http.post()
            .uri("/models/{modelo}:generateContent", modelo)
            .contentType(MediaType.APPLICATION_JSON)
            .body(corpo)
            .retrieve()
            .body(GenerateResponse.class), tentativas);

        if (resposta == null || resposta.candidates() == null || resposta.candidates().isEmpty()) {
            throw new IaIndisponivelException("O Gemini não devolveu nenhuma resposta.");
        }
        Candidate candidato = resposta.candidates().get(0);
        if (candidato.content() == null || candidato.content().parts() == null) {
            throw new IaIndisponivelException(
                "O Gemini não devolveu texto (finishReason=" + candidato.finishReason() + ").");
        }
        StringBuilder texto = new StringBuilder();
        for (Part parte : candidato.content().parts()) {
            // partes marcadas como "thought" são o raciocínio interno do modelo, não a resposta
            if (parte.text() != null && !Boolean.TRUE.equals(parte.thought())) {
                texto.append(parte.text());
            }
        }
        String modeloQueRespondeu = resposta.modelVersion() != null ? resposta.modelVersion() : modelo;
        return new Geracao(texto.toString().strip(), modeloQueRespondeu);
    }

    private List<float[]> embedLote(List<String> textos, String taskType, String titulo) {
        String modelo = "models/" + modeloEmbedding;
        List<EmbedContentRequest> requests = textos.stream()
            .map(t -> new EmbedContentRequest(modelo, new Content(null, List.of(new Part(t, null))), taskType, titulo, dimensoes))
            .toList();

        BatchEmbedResponse resposta = chamar(() -> http.post()
            .uri("/models/{modelo}:batchEmbedContents", modeloEmbedding)
            .contentType(MediaType.APPLICATION_JSON)
            .body(new BatchEmbedRequest(requests))
            .retrieve()
            .body(BatchEmbedResponse.class), TENTATIVAS_EMBEDDING);

        if (resposta == null || resposta.embeddings() == null || resposta.embeddings().size() != textos.size()) {
            throw new IaIndisponivelException("O Gemini devolveu uma quantidade inesperada de embeddings.");
        }
        List<float[]> vetores = resposta.embeddings().stream().map(Embedding::values).toList();
        for (float[] v : vetores) {
            // Se alguém trocar o modelo/dimensão sem ajustar o vector(768) do banco,
            // é melhor falhar aqui com uma mensagem clara do que no INSERT.
            if (v == null || v.length != dimensoes) {
                throw new IaIndisponivelException("Embedding com dimensão " + (v == null ? 0 : v.length)
                    + ", esperado " + dimensoes + ". Confira fersa.gemini.embedding-dimensoes e database/manuais.sql.");
            }
        }
        return vetores;
    }

    /**
     * Executa a chamada, repetindo em erros passageiros (429 = cota por
     * minuto do plano gratuito, 5xx, timeout) até {@code tentativas} vezes.
     * No 429 a espera é maior (15 s, 30 s, 45 s), porque a cota só libera
     * quando o minuto vira. Erros como 400/403 (chave inválida, pedido mal
     * formado) falham na hora.
     */
    private <T> T chamar(Supplier<T> chamada, int tentativas) {
        if (!chaveConfigurada) {
            throw new IaIndisponivelException("GEMINI_API_KEY não configurada no backend.");
        }
        for (int tentativa = 1; ; tentativa++) {
            long esperaMs = 2000L * tentativa;
            try {
                return chamada.get();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                boolean passageiro = status == 429 || status >= 500;
                if (!passageiro || tentativa >= tentativas) {
                    // 404 = modelo inexistente/aposentado: outro modelo pode resolver
                    throw new IaIndisponivelException("Gemini respondeu HTTP " + status + ": "
                        + resumir(e.getResponseBodyAsString()), e, passageiro || status == 404);
                }
                if (status == 429) {
                    esperaMs = 15_000L * tentativa;
                }
                log.warn("Gemini respondeu HTTP {} (tentativa {}/{}), tentando de novo em {} s",
                    status, tentativa, tentativas, esperaMs / 1000);
            } catch (RestClientException e) {
                // Timeout/conexão caída. Se acontece enquanto o corpo da resposta
                // está chegando, o Spring não lança ResourceAccessException e sim
                // uma RestClientException genérica com o IOException como causa —
                // por isso olhamos a causa em vez do tipo.
                boolean falhaDeRede = e instanceof ResourceAccessException || causadoPorIo(e);
                if (!falhaDeRede) {
                    // resposta que não deu para ler (JSON inesperado)
                    throw new IaIndisponivelException("Resposta inválida do Gemini: " + e.getMessage(), e);
                }
                if (tentativa >= tentativas) {
                    throw new IaIndisponivelException("Gemini não respondeu a tempo ou a conexão caiu: "
                        + e.getMostSpecificCause(), e, true);
                }
                log.warn("Falha de rede com o Gemini ({}), tentativa {}/{}, tentando de novo",
                    e.getMostSpecificCause().getClass().getSimpleName(), tentativa, tentativas);
            }
            esperar(esperaMs);
        }
    }

    private static boolean causadoPorIo(Throwable e) {
        for (Throwable c = e.getCause(); c != null; c = c.getCause()) {
            if (c instanceof IOException) return true;
        }
        return false;
    }

    private static void esperar(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IaIndisponivelException("Chamada ao Gemini interrompida.", e);
        }
    }

    private static String resumir(String corpo) {
        if (corpo == null) return "";
        String limpo = corpo.replaceAll("\\s+", " ").strip();
        return limpo.length() > 300 ? limpo.substring(0, 300) + "…" : limpo;
    }

    // ----- JSON da API do Gemini (só os campos que usamos) -----

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text, Boolean thought) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(String role, List<Part> parts) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record EmbedContentRequest(String model, Content content, String taskType, String title, Integer outputDimensionality) {}

    record BatchEmbedRequest(List<EmbedContentRequest> requests) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Embedding(float[] values) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record BatchEmbedResponse(List<Embedding> embeddings) {}

    record ThinkingConfig(String thinkingLevel) {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record GenerationConfig(Double temperature, ThinkingConfig thinkingConfig) {}

    record GenerateRequest(Content systemInstruction, List<Content> contents, GenerationConfig generationConfig) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content, String finishReason) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GenerateResponse(List<Candidate> candidates, String modelVersion) {}
}
