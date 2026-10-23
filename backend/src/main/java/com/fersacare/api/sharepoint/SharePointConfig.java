package com.fersacare.api.sharepoint;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Cliente HTTP da Microsoft Graph. Sem header de autorização fixo: o
 * GraphClient põe o token em cada chamada — e NÃO põe no download, que usa
 * uma URL pré-autenticada do SharePoint (mandar o token lá dá erro).
 *
 * Usa o HttpClient do Java (e não o HttpURLConnection) porque este perde o
 * corpo das respostas 401 — e é nele que o Entra ID explica o motivo
 * ("AADSTS7000215: Invalid client secret..."). O proxy do sistema é
 * respeitado, para funcionar em rede de empresa.
 */
@Configuration
public class SharePointConfig {

    @Bean
    public RestClient graphRestClient(RestClient.Builder builder) {
        HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .proxy(ProxySelector.getDefault())
            .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(60)); // download de PDF grande
        return builder.requestFactory(requestFactory).build();
    }
}
