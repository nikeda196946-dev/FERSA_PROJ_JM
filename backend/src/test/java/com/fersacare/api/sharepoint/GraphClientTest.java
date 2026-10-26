package com.fersacare.api.sharepoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Conferir as chamadas ao Entra ID e à Graph sem rede. */
class GraphClientTest {

    private static final String LOGIN = "https://login.teste";
    private static final String GRAPH = "https://graph.teste/v1.0";

    private MockRestServiceServer servidor;
    private GraphClient graph;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        graph = new GraphClient(builder.build(), "tenant-1", "cliente-1", "segredo", LOGIN, GRAPH);
    }

    private void esperaToken() {
        servidor.expect(once(), requestTo(LOGIN + "/tenant-1/oauth2/v2.0/token"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(content().formDataContains(java.util.Map.of(
                "grant_type", "client_credentials", "client_id", "cliente-1",
                "client_secret", "segredo", "scope", "https://graph.microsoft.com/.default")))
            .andRespond(withSuccess("{\"access_token\":\"tok-123\",\"expires_in\":3599}", MediaType.APPLICATION_JSON));
    }

    private void esperaJson(String url, String json) {
        servidor.expect(once(), requestTo(url))
            .andExpect(header("Authorization", "Bearer tok-123"))
            .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
    }

    @Test
    void resolveSiteBibliotecaEPastaEListaPdfsComSubpastasEPaginacao() {
        esperaToken(); // um token só: é reaproveitado em todas as chamadas
        esperaJson(GRAPH + "/sites/empresa.sharepoint.com:/sites/Manuais%20Fersa?$select=id,webUrl,displayName",
            "{\"id\":\"site-1\",\"webUrl\":\"https://empresa.sharepoint.com/sites/Manuais Fersa\"}");
        esperaJson(GRAPH + "/sites/site-1/drive?$select=id,name", "{\"id\":\"drive-1\",\"name\":\"Documentos\"}");
        esperaJson(GRAPH + "/drives/drive-1/root:/Manuais/Rolamentos?$select=id,name,folder",
            "{\"id\":\"pasta-1\",\"name\":\"Rolamentos\",\"folder\":{\"childCount\":3}}");

        String sel = "/children?$top=200&$select=id,name,size,eTag,webUrl,lastModifiedDateTime,file,folder";
        esperaJson(GRAPH + "/drives/drive-1/items/pasta-1" + sel, """
            {"value":[
              {"id":"a1","name":"Manual FRS-1001.pdf","size":1000,"eTag":"\\"{A1},1\\"","webUrl":"https://x/a1",
               "lastModifiedDateTime":"2026-10-20T10:00:00Z","file":{"mimeType":"application/pdf"}},
              {"id":"txt","name":"leia-me.txt","size":10,"file":{"mimeType":"text/plain"}},
              {"id":"sub","name":"Cubo","folder":{"childCount":1}}
            ],
            "@odata.nextLink":"https://graph.teste/v1.0/drives/drive-1/items/pasta-1/children?$skiptoken=p2"}""");
        esperaJson(GRAPH + "/drives/drive-1/items/pasta-1/children?$skiptoken=p2",
            "{\"value\":[{\"id\":\"a2\",\"name\":\"FRS-1002.PDF\",\"size\":2000,\"eTag\":\"e2\",\"file\":{}}]}");
        esperaJson(GRAPH + "/drives/drive-1/items/sub" + sel,
            "{\"value\":[{\"id\":\"a3\",\"name\":\"cubo.pdf\",\"size\":3000,\"eTag\":\"e3\",\"file\":{}}]}");

        GraphClient.Local local = graph.resolver("https://empresa.sharepoint.com/sites/Manuais Fersa/Shared Documents/Forms/AllItems.aspx", "/Manuais/Rolamentos/");
        List<GraphClient.ArquivoRemoto> pdfs = graph.listarPdfs(local);

        assertThat(local.driveId()).isEqualTo("drive-1");
        assertThat(pdfs).extracting(GraphClient.ArquivoRemoto::nome).containsExactly("FRS-1002.PDF", "Manual FRS-1001.pdf", "cubo.pdf");
        assertThat(pdfs).extracting(GraphClient.ArquivoRemoto::caminho).containsExactly("", "", "Cubo");
        assertThat(pdfs.get(1).etag()).isEqualTo("\"{A1},1\"");
        servidor.verify();
    }

    @Test
    void baixaPelaUrlPreAutenticadaSemMandarOToken() {
        esperaToken();
        esperaJson(GRAPH + "/drives/drive-1/items/a1",
            "{\"id\":\"a1\",\"name\":\"m.pdf\",\"@microsoft.graph.downloadUrl\":\"https://download.teste/arquivo?tempauth=xyz\"}");
        servidor.expect(once(), requestTo("https://download.teste/arquivo?tempauth=xyz"))
            .andExpect(headerDoesNotExist("Authorization"))
            .andRespond(withSuccess(new byte[] {'%', 'P', 'D', 'F'}, MediaType.APPLICATION_OCTET_STREAM));

        byte[] pdf = graph.baixar(new GraphClient.Local("site-1", "drive-1", "pasta-1", null), "a1");

        assertThat(new String(pdf)).isEqualTo("%PDF");
        servidor.verify();
    }

    @Test
    void secretErradoViraMensagemClara() {
        servidor.expect(once(), requestTo(LOGIN + "/tenant-1/oauth2/v2.0/token"))
            .andRespond(withStatus(HttpStatus.UNAUTHORIZED).contentType(MediaType.APPLICATION_JSON).body("""
                {"error":"invalid_client","error_description":"AADSTS7000215: Invalid client secret provided. Trace ID: abc"}"""));

        assertThatThrownBy(() -> graph.resolver("https://empresa.sharepoint.com/sites/X", ""))
            .isInstanceOf(SharePointException.class)
            .hasMessageContaining("AADSTS7000215: Invalid client secret provided. Confira")
            .hasMessageContaining("SHAREPOINT_CLIENT_SECRET")
            .hasMessageNotContaining("segredo");
    }

    @Test
    void semPermissaoExplicaQueFaltaSitesReadAll() {
        esperaToken();
        servidor.expect(once(), requestTo(GRAPH + "/sites/empresa.sharepoint.com:/sites/X?$select=id,webUrl,displayName"))
            .andRespond(withStatus(HttpStatus.FORBIDDEN).contentType(MediaType.APPLICATION_JSON)
                .body("{\"error\":{\"code\":\"accessDenied\",\"message\":\"Access denied\"}}"));

        assertThatThrownBy(() -> graph.resolver("https://empresa.sharepoint.com/sites/X", ""))
            .isInstanceOf(SharePointException.class)
            .hasMessageContaining("Sites.Read.All");
    }

    @Test
    void pastaInexistenteDizQualPasta() {
        esperaToken();
        esperaJson(GRAPH + "/sites/empresa.sharepoint.com:/sites/X?$select=id,webUrl,displayName", "{\"id\":\"s\"}");
        esperaJson(GRAPH + "/sites/s/drive?$select=id,name", "{\"id\":\"d\",\"name\":\"Documentos\"}");
        servidor.expect(once(), requestTo(GRAPH + "/drives/d/root:/Nao%20Existe?$select=id,name,folder"))
            .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> graph.resolver("https://empresa.sharepoint.com/sites/X", "Nao Existe"))
            .isInstanceOf(SharePointException.class)
            .hasMessageContaining("Pasta \"Nao Existe\" não encontrada");
    }

    @Test
    void semCredenciaisNemTentaLogar() {
        GraphClient semConfig = new GraphClient(RestClient.create(), "", "", "", LOGIN, GRAPH);
        assertThat(semConfig.credenciaisConfiguradas()).isFalse();
        assertThatThrownBy(semConfig::token).isInstanceOf(SharePointException.class).hasMessageContaining("SHAREPOINT_TENANT_ID");
    }

    @Test
    void entendeAsUrlsQueAPessoaCopiaDoNavegador() {
        assertThat(GraphClient.SiteUrl.de("https://empresa.sharepoint.com/sites/Manuais"))
            .isEqualTo(new GraphClient.SiteUrl("empresa.sharepoint.com", "/sites/Manuais"));
        assertThat(GraphClient.SiteUrl.de("https://empresa.sharepoint.com/teams/Eng/Shared%20Documents/Forms/AllItems.aspx?id=1"))
            .isEqualTo(new GraphClient.SiteUrl("empresa.sharepoint.com", "/teams/Eng"));
        assertThat(GraphClient.SiteUrl.de("https://empresa.sharepoint.com/sites/Manuais Fersa/"))
            .isEqualTo(new GraphClient.SiteUrl("empresa.sharepoint.com", "/sites/Manuais Fersa"));
        assertThat(GraphClient.SiteUrl.de("https://empresa.sharepoint.com/"))
            .isEqualTo(new GraphClient.SiteUrl("empresa.sharepoint.com", ""));
        assertThatThrownBy(() -> GraphClient.SiteUrl.de("empresa.sharepoint.com/sites/X")).isInstanceOf(SharePointException.class);
    }
}
