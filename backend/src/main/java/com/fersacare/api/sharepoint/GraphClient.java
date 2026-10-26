package com.fersacare.api.sharepoint;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Fala com o Entra ID (login do app) e com a Microsoft Graph (SharePoint).
 *
 * Login: OAuth2 "client credentials" — o backend entra como o próprio app
 * registrado no Entra ID (tenant id + client id + secret), sem usuário. A
 * permissão de aplicativo Sites.Read.All (com consentimento do admin) deixa
 * o app só LER os sites; nada é alterado no SharePoint.
 *
 * Caminho até os PDFs:
 *   URL do site → GET /sites/{host}:/{caminho-do-site}      → id do site
 *              → GET /sites/{id}/drive                      → biblioteca "Documentos"
 *              → GET /drives/{drive}/root:/{pasta}          → a pasta configurada
 *              → GET /drives/{drive}/items/{id}/children    → arquivos (e subpastas)
 */
@Component
public class GraphClient {

    private static final Logger log = LoggerFactory.getLogger(GraphClient.class);
    /** Até onde descer em subpastas (evita varrer uma biblioteca inteira sem querer). */
    static final int PROFUNDIDADE_MAXIMA = 5;

    /** Um PDF encontrado no SharePoint. caminho = subpasta relativa à pasta sincronizada ("" = a própria pasta). */
    public record ArquivoRemoto(String itemId, String nome, long tamanho, String etag, String webUrl,
                                Instant modificadoEm, String caminho) {}

    /** Site + biblioteca + pasta já resolvidos (ids da Graph). */
    public record Local(String siteId, String driveId, String pastaId, String siteWebUrl) {}

    private record Token(String valor, Instant expiraEm) {}

    private final RestClient http;
    private final String tenantId;
    private final String clientId;
    private final String clientSecret;
    private final String loginUrl;
    private final String graphUrl;
    private Token token;

    public GraphClient(
        @Qualifier("graphRestClient") RestClient http,
        @Value("${fersa.sharepoint.tenant-id:}") String tenantId,
        @Value("${fersa.sharepoint.client-id:}") String clientId,
        @Value("${fersa.sharepoint.client-secret:}") String clientSecret,
        @Value("${fersa.sharepoint.login-url:https://login.microsoftonline.com}") String loginUrl,
        @Value("${fersa.sharepoint.graph-url:https://graph.microsoft.com/v1.0}") String graphUrl
    ) {
        this.http = http;
        this.tenantId = tenantId == null ? "" : tenantId.strip();
        this.clientId = clientId == null ? "" : clientId.strip();
        this.clientSecret = clientSecret == null ? "" : clientSecret.strip();
        this.loginUrl = semBarraFinal(loginUrl);
        this.graphUrl = semBarraFinal(graphUrl);
    }

    public boolean credenciaisConfiguradas() {
        return !tenantId.isEmpty() && !clientId.isEmpty() && !clientSecret.isEmpty();
    }

    /** Resolve site, biblioteca padrão e pasta. pasta vazia = raiz da biblioteca. */
    public Local resolver(String siteUrl, String pasta) {
        SiteUrl site = SiteUrl.de(siteUrl);
        String caminhoSite = site.caminho().isEmpty() ? "" : ":" + UriUtils.encodePath(site.caminho(), StandardCharsets.UTF_8);
        JsonNode s = getJson(graphUrl + "/sites/" + site.host() + caminhoSite + "?$select=id,webUrl,displayName",
            "Site não encontrado: " + siteUrl + ". Confira SHAREPOINT_SITE_URL (ex.: https://empresa.sharepoint.com/sites/Manuais).");
        String siteId = s.path("id").asText();
        JsonNode drive = getJson(graphUrl + "/sites/" + siteId + "/drive?$select=id,name",
            "O site não tem biblioteca de documentos acessível.");
        String driveId = drive.path("id").asText();

        String p = pasta == null ? "" : pasta.strip().replaceAll("^/+|/+$", "");
        String urlPasta = p.isEmpty()
            ? graphUrl + "/drives/" + driveId + "/root?$select=id,name,folder"
            : graphUrl + "/drives/" + driveId + "/root:/" + UriUtils.encodePath(p, StandardCharsets.UTF_8) + "?$select=id,name,folder";
        JsonNode pastaNode = getJson(urlPasta,
            "Pasta \"" + p + "\" não encontrada na biblioteca \"" + drive.path("name").asText("Documentos")
                + "\" do site. Confira SHAREPOINT_PASTA (caminho dentro da biblioteca, ex.: Manuais/Rolamentos).");
        if (pastaNode.path("folder").isMissingNode()) {
            throw new SharePointException("SHAREPOINT_PASTA aponta para um arquivo, não para uma pasta: " + p);
        }
        return new Local(siteId, driveId, pastaNode.path("id").asText(), s.path("webUrl").asText(null));
    }

    /** Todos os PDFs da pasta e das subpastas (até PROFUNDIDADE_MAXIMA níveis), seguindo a paginação da Graph. */
    public List<ArquivoRemoto> listarPdfs(Local local) {
        record Pendente(String id, String caminho, int nivel) {}
        List<ArquivoRemoto> pdfs = new ArrayList<>();
        Deque<Pendente> fila = new ArrayDeque<>();
        fila.add(new Pendente(local.pastaId(), "", 0));
        while (!fila.isEmpty()) {
            Pendente atual = fila.poll();
            String url = graphUrl + "/drives/" + local.driveId() + "/items/" + atual.id()
                + "/children?$top=200&$select=id,name,size,eTag,webUrl,lastModifiedDateTime,file,folder";
            while (url != null) {
                JsonNode pagina = getJson(url, "Não foi possível listar a pasta do SharePoint.");
                for (JsonNode item : pagina.path("value")) {
                    String nome = item.path("name").asText();
                    if (!item.path("folder").isMissingNode()) {
                        if (atual.nivel() + 1 <= PROFUNDIDADE_MAXIMA) {
                            fila.add(new Pendente(item.path("id").asText(),
                                atual.caminho().isEmpty() ? nome : atual.caminho() + "/" + nome, atual.nivel() + 1));
                        }
                    } else if (!item.path("file").isMissingNode() && ehPdf(item)) {
                        pdfs.add(new ArquivoRemoto(
                            item.path("id").asText(), nome, item.path("size").asLong(), item.path("eTag").asText(null),
                            item.path("webUrl").asText(null), instante(item.path("lastModifiedDateTime").asText(null)),
                            atual.caminho()));
                    }
                }
                url = pagina.hasNonNull("@odata.nextLink") ? pagina.get("@odata.nextLink").asText() : null;
            }
        }
        pdfs.sort((a, b) -> (a.caminho() + "/" + a.nome()).compareToIgnoreCase(b.caminho() + "/" + b.nome()));
        return pdfs;
    }

    /**
     * Baixa o conteúdo do arquivo. Pega primeiro a URL de download
     * pré-autenticada (@microsoft.graph.downloadUrl, vale ~1 h) e baixa dela
     * sem o token — é o jeito recomendado pela Graph.
     */
    public byte[] baixar(Local local, String itemId) {
        JsonNode item = getJson(graphUrl + "/drives/" + local.driveId() + "/items/" + itemId,
            "Arquivo não encontrado no SharePoint (pode ter sido apagado ou movido).");
        String urlDownload = item.path("@microsoft.graph.downloadUrl").asText(null);
        if (urlDownload == null) {
            throw new SharePointException("O SharePoint não devolveu o link de download do arquivo " + item.path("name").asText());
        }
        return chamar(() -> http.get().uri(URI.create(urlDownload)).retrieve().body(byte[].class),
            "Falha ao baixar o arquivo " + item.path("name").asText() + " do SharePoint.");
    }

    // ----- autenticação -----

    /** Token do app, reaproveitado até faltar 1 minuto para expirar (vale ~1 h). */
    synchronized String token() {
        if (!credenciaisConfiguradas()) {
            throw new SharePointException("SharePoint não configurado: faltam SHAREPOINT_TENANT_ID, SHAREPOINT_CLIENT_ID "
                + "e/ou SHAREPOINT_CLIENT_SECRET no .env do backend.");
        }
        if (token != null && Instant.now().isBefore(token.expiraEm().minusSeconds(60))) {
            return token.valor();
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("scope", "https://graph.microsoft.com/.default");
        form.add("grant_type", "client_credentials");
        try {
            JsonNode r = http.post()
                .uri(URI.create(loginUrl + "/" + UriUtils.encodePathSegment(tenantId, StandardCharsets.UTF_8) + "/oauth2/v2.0/token"))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(JsonNode.class);
            if (r == null || !r.hasNonNull("access_token")) {
                throw new SharePointException("O Entra ID não devolveu um token de acesso.");
            }
            token = new Token(r.get("access_token").asText(), Instant.now().plusSeconds(r.path("expires_in").asLong(3600)));
            return token.valor();
        } catch (RestClientResponseException e) {
            throw new SharePointException("O Entra ID recusou o login do app: " + descricaoErroLogin(e)
                + ". Confira SHAREPOINT_TENANT_ID, SHAREPOINT_CLIENT_ID e SHAREPOINT_CLIENT_SECRET "
                + "(use o VALOR do secret, não o ID dele; secrets expiram).", e);
        } catch (RestClientException e) {
            throw new SharePointException("Não foi possível falar com o Entra ID (" + loginUrl + "): " + e.getMessage(), e);
        }
    }

    // ----- HTTP -----

    private JsonNode getJson(String url, String mensagem404) {
        return chamar(() -> http.get()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + token())
            .accept(MediaType.APPLICATION_JSON)
            .retrieve()
            .body(JsonNode.class), mensagem404);
    }

    /** Uma nova tentativa em 429/503 (limite da Graph); demais erros viram mensagem clara. */
    private <T> T chamar(Supplier<T> chamada, String mensagem404) {
        for (int tentativa = 1; ; tentativa++) {
            try {
                return chamada.get();
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if ((status == 429 || status == 503) && tentativa == 1) {
                    log.warn("Graph respondeu {}, tentando de novo em 2 s", status);
                    esperar();
                    continue;
                }
                if (status == 401) {
                    synchronized (this) { token = null; }
                    throw new SharePointException("A Microsoft Graph recusou o token do app (401). Confira se o app está no mesmo tenant do site.", e);
                }
                if (status == 403) {
                    throw new SharePointException("Acesso negado pelo SharePoint (403). O app precisa da permissão de APLICATIVO "
                        + "Sites.Read.All da Microsoft Graph, com \"consentimento do administrador\" concedido no Entra ID.", e);
                }
                if (status == 404) {
                    throw new SharePointException(mensagem404, e);
                }
                throw new SharePointException("Erro da Microsoft Graph (HTTP " + status + "): " + mensagemErroGraph(e), e);
            } catch (SharePointException e) {
                throw e;
            } catch (RestClientException e) {
                throw new SharePointException("Não foi possível falar com o SharePoint: " + e.getMessage(), e);
            }
        }
    }

    private static boolean ehPdf(JsonNode item) {
        return item.path("name").asText().toLowerCase(Locale.ROOT).endsWith(".pdf")
            || "application/pdf".equalsIgnoreCase(item.path("file").path("mimeType").asText());
    }

    private static String descricaoErroLogin(RestClientResponseException e) {
        try {
            JsonNode r = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
            String d = r.path("error_description").asText(r.path("error").asText(""));
            // "AADSTS7000215: Invalid client secret provided. ... Trace ID: ..." → só a primeira frase
            int corte = d.indexOf("\r\n") >= 0 ? d.indexOf("\r\n") : d.indexOf(" Trace ID");
            return (corte > 0 ? d.substring(0, corte) : d).strip().replaceAll("[.\\s]+$", "");
        } catch (Exception ignorado) {
            return "HTTP " + e.getStatusCode().value();
        }
    }

    private static String mensagemErroGraph(RestClientResponseException e) {
        try {
            JsonNode r = new com.fasterxml.jackson.databind.ObjectMapper().readTree(e.getResponseBodyAsString());
            return r.path("error").path("message").asText("sem detalhes");
        } catch (Exception ignorado) {
            return "sem detalhes";
        }
    }

    private static Instant instante(String iso) {
        try {
            return iso == null ? null : Instant.parse(iso);
        } catch (Exception e) {
            return null;
        }
    }

    private static void esperar() {
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String semBarraFinal(String s) {
        return s == null ? "" : s.strip().replaceAll("/+$", "");
    }

    /**
     * Separa a URL que a pessoa copiou do navegador em host + caminho do site.
     * Aceita a URL do site ou de qualquer página dentro dele:
     *   https://empresa.sharepoint.com/sites/Manuais Fersa/Shared Documents/Forms/AllItems.aspx
     *   → host "empresa.sharepoint.com", caminho "/sites/Manuais Fersa"
     * Sem /sites/ ou /teams/ no caminho, é o site raiz.
     */
    record SiteUrl(String host, String caminho) {
        static SiteUrl de(String url) {
            if (url == null || url.isBlank()) {
                throw new SharePointException("SharePoint não configurado: falta SHAREPOINT_SITE_URL no .env do backend.");
            }
            URI uri;
            try {
                uri = URI.create(url.strip().replace(" ", "%20"));
            } catch (IllegalArgumentException e) {
                throw new SharePointException("SHAREPOINT_SITE_URL inválida: " + url);
            }
            if (uri.getHost() == null) {
                throw new SharePointException("SHAREPOINT_SITE_URL inválida (use a URL completa, com https://): " + url);
            }
            String[] partes = (uri.getPath() == null ? "" : uri.getPath()).split("/");
            List<String> segmentos = new ArrayList<>();
            for (String p : partes) if (!p.isBlank()) segmentos.add(p);
            String caminho = "";
            if (segmentos.size() >= 2 && (segmentos.get(0).equalsIgnoreCase("sites") || segmentos.get(0).equalsIgnoreCase("teams"))) {
                caminho = "/" + segmentos.get(0) + "/" + segmentos.get(1);
            }
            return new SiteUrl(uri.getHost(), caminho);
        }
    }
}
