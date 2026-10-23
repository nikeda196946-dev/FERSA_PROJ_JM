package com.fersacare.api.ia;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Cliente HTTP do Gemini. Fica num bean separado do GeminiClient para que
 * os testes possam trocar o RestClient por um falso (MockRestServiceServer)
 * sem mexer na lógica de montar/ler as requisições.
 *
 * A chave vai no header x-goog-api-key (e não na URL ?key=), para não
 * aparecer em log de acesso nem em mensagem de erro.
 */
@Configuration
public class GeminiConfig {

    @Bean
    public RestClient geminiRestClient(
        RestClient.Builder builder,
        @Value("${fersa.gemini.base-url}") String baseUrl,
        @Value("${fersa.gemini.api-key}") String apiKey
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(90)); // geração de resposta pode levar alguns segundos

        return builder
            .baseUrl(baseUrl)
            .defaultHeader("x-goog-api-key", apiKey)
            // Buffering: manda o corpo com Content-Length em vez de "chunked"
            .requestFactory(new BufferingClientHttpRequestFactory(requestFactory))
            .build();
    }
}
