package com.fersacare.api.sharepoint;

import com.fersacare.api.common.RequisicaoInvalidaException;
import com.fersacare.api.manuais.dto.CategoriaDTO;
import com.fersacare.api.manuais.dto.ManualDTO;
import com.fersacare.api.manuais.model.StatusManual;
import com.fersacare.api.manuais.repository.CategoriaRepository;
import com.fersacare.api.manuais.repository.ManualRepository;
import com.fersacare.api.manuais.repository.ManualRepository.VinculoSharePoint;
import com.fersacare.api.manuais.service.ManualService;
import com.fersacare.api.sharepoint.GraphClient.ArquivoRemoto;
import com.fersacare.api.sharepoint.dto.ArquivoSharePointDTO.SituacaoArquivo;
import com.fersacare.api.sharepoint.dto.ImportarSharePointRequest;
import com.fersacare.api.sharepoint.dto.ResultadoImportacaoDTO;
import com.fersacare.api.sharepoint.dto.ResultadoImportacaoDTO.Resultado;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SharePointServiceTest {

    private static final GraphClient.Local LOCAL = new GraphClient.Local("s", "d", "p", null);
    private static final byte[] PDF = {'%', 'P', 'D', 'F', '-'};

    private GraphClient graph;
    private ManualService manualService;
    private ManualRepository manualRepository;
    private CategoriaRepository categoriaRepository;
    private SharePointService service;

    @BeforeEach
    void setUp() {
        graph = mock(GraphClient.class);
        manualService = mock(ManualService.class);
        manualRepository = mock(ManualRepository.class);
        categoriaRepository = mock(CategoriaRepository.class);
        when(graph.credenciaisConfiguradas()).thenReturn(true);
        when(graph.resolver(anyString(), anyString())).thenReturn(LOCAL);
        when(graph.baixar(eq(LOCAL), anyString())).thenReturn(PDF);
        service = new SharePointService(graph, manualService, manualRepository, categoriaRepository,
            "https://empresa.sharepoint.com/sites/Manuais", "Manuais", 0, true, 25);
    }

    private static ArquivoRemoto arquivo(String id, String nome, String etag, String caminho, long tamanho) {
        return new ArquivoRemoto(id, nome, tamanho, etag, "https://sp/" + id, Instant.parse("2026-10-20T10:00:00Z"), caminho);
    }

    private static ManualDTO manual(UUID id, String categoria) {
        return new ManualDTO(id, "titulo", null, categoria, "a.pdf", 5, null, 0, StatusManual.PROCESSANDO, null, true,
            "sharepoint", "https://sp/x", "", Instant.now(), Instant.now(), List.of());
    }

    @Test
    void classificaNovoImportadoAlteradoEGrandeDemais() {
        UUID m1 = UUID.randomUUID(), m2 = UUID.randomUUID();
        List<SharePointService.Item> itens = SharePointService.classificar(
            List.of(arquivo("novo", "n.pdf", "e1", "", 10), arquivo("igual", "i.pdf", "e2", "", 10),
                arquivo("mudou", "m.pdf", "e3-novo", "", 10), arquivo("grande", "g.pdf", "e4", "", 100)),
            List.of(new VinculoSharePoint(m1, "igual", "e2", "Igual", StatusManual.PRONTO),
                new VinculoSharePoint(m2, "mudou", "e3-velho", "Mudou", StatusManual.PRONTO)),
            50);

        assertThat(itens).extracting(i -> i.dto().situacao()).containsExactly(
            SituacaoArquivo.NOVO, SituacaoArquivo.IMPORTADO, SituacaoArquivo.ALTERADO, SituacaoArquivo.GRANDE_DEMAIS);
        assertThat(itens.get(2).dto().manualId()).isEqualTo(m2);
    }

    @Test
    void importaNovosComCategoriaDaSubpastaEAtualizaAlterados() {
        UUID existente = UUID.randomUUID(), categoriaCubo = UUID.randomUUID(), novoId = UUID.randomUUID();
        when(graph.listarPdfs(LOCAL)).thenReturn(List.of(
            arquivo("a", "FRS-1001.pdf", "e1", "Rolamento de Cubo/2026", 10),
            arquivo("b", "FRS-1002.pdf", "e2", "Rolamento de Cubo", 10),
            arquivo("c", "FRS-1003.pdf", "e3-novo", "", 10),
            arquivo("d", "FRS-1004.pdf", "e4", "", 10)));
        when(manualRepository.listarVinculosSharePoint()).thenReturn(List.of(
            new VinculoSharePoint(existente, "c", "e3-velho", "FRS-1003", StatusManual.PRONTO),
            new VinculoSharePoint(UUID.randomUUID(), "d", "e4", "FRS-1004", StatusManual.PRONTO)));
        when(categoriaRepository.buscarPorNome("Rolamento de Cubo")).thenReturn(Optional.empty());
        when(categoriaRepository.inserir(eq("Rolamento de Cubo"), anyString(), eq(true))).thenReturn(categoriaCubo);
        when(manualService.importarDoSharePoint(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(manual(novoId, "Rolamento de Cubo"));
        when(manualService.atualizarDoSharePoint(eq(existente), anyString(), any(), anyString(), anyString(), anyString()))
            .thenReturn(manual(existente, null));

        List<ResultadoImportacaoDTO> r = service.importar(new ImportarSharePointRequest(null, null, null));

        assertThat(r).extracting(ResultadoImportacaoDTO::resultado)
            .containsExactly(Resultado.IMPORTADO, Resultado.IMPORTADO, Resultado.ATUALIZADO); // "d" sem mudança nem aparece
        verify(categoriaRepository, times(1)).inserir(eq("Rolamento de Cubo"), anyString(), eq(true)); // criada uma vez só
        verify(manualService).importarDoSharePoint("FRS-1001.pdf", PDF, categoriaCubo, "a", "e1", "https://sp/a", "Rolamento de Cubo/2026");
        verify(graph, never()).baixar(LOCAL, "d");
        assertThat(service.status().resumoUltimaSincronizacao()).isEqualTo("2 importado(s), 1 atualizado(s).");
    }

    @Test
    void categoriaFixaQuandoNaoUsaAPasta() {
        UUID fixa = UUID.randomUUID();
        when(categoriaRepository.buscarPorId(fixa)).thenReturn(Optional.of(new CategoriaDTO(fixa, "Geral", null, true, 0, Instant.now())));
        when(graph.listarPdfs(LOCAL)).thenReturn(List.of(arquivo("a", "x.pdf", "e", "Sub", 10)));
        when(manualRepository.listarVinculosSharePoint()).thenReturn(List.of());
        when(manualService.importarDoSharePoint(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(manual(UUID.randomUUID(), "Geral"));

        service.importar(new ImportarSharePointRequest(null, fixa, false));

        verify(manualService).importarDoSharePoint("x.pdf", PDF, fixa, "a", "e", "https://sp/a", "Sub");
        verify(categoriaRepository, never()).inserir(anyString(), anyString(), anyBoolean());
    }

    @Test
    void escolhidosForaDaPastaEErrosDeUmArquivoNaoParamOsOutros() {
        when(graph.listarPdfs(LOCAL)).thenReturn(List.of(arquivo("a", "a.pdf", "e", "", 10), arquivo("b", "b.pdf", "e", "", 10)));
        when(manualRepository.listarVinculosSharePoint()).thenReturn(List.of());
        when(graph.baixar(LOCAL, "a")).thenThrow(new SharePointException("Falha ao baixar"));
        when(manualService.importarDoSharePoint(eq("b.pdf"), any(), any(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(manual(UUID.randomUUID(), null));

        List<ResultadoImportacaoDTO> r = service.importar(new ImportarSharePointRequest(List.of("a", "b", "sumiu"), null, false));

        assertThat(r).extracting(ResultadoImportacaoDTO::resultado).containsExactly(Resultado.ERRO, Resultado.IMPORTADO, Resultado.ERRO);
        assertThat(r.get(2).mensagem()).contains("não encontrado");
    }

    @Test
    void pdfQueJaExisteViraVinculoSemCriarCategoria() {
        UUID existente = UUID.randomUUID();
        when(graph.listarPdfs(LOCAL)).thenReturn(List.of(arquivo("a", "a.pdf", "e", "Pasta Nova", 10)));
        when(manualRepository.listarVinculosSharePoint()).thenReturn(List.of());
        ManualDTO jaEnviado = new ManualDTO(existente, "Manual antigo", null, null, "a.pdf", 5, 1, 1, StatusManual.PRONTO, null,
            true, "upload", null, null, Instant.now(), Instant.now(), List.of());
        when(manualService.buscarPorConteudo(PDF)).thenReturn(Optional.of(jaEnviado));
        when(manualRepository.vincularAoSharePoint(existente, "a", "e", "https://sp/a", "Pasta Nova")).thenReturn(true, false);

        List<ResultadoImportacaoDTO> r = service.importar(new ImportarSharePointRequest(null, null, true));

        assertThat(r).extracting(ResultadoImportacaoDTO::resultado).containsExactly(Resultado.VINCULADO);
        verify(categoriaRepository, never()).inserir(anyString(), anyString(), anyBoolean());
        verify(manualService, never()).importarDoSharePoint(anyString(), any(), any(), anyString(), anyString(), anyString(), anyString());

        // se o manual já estiver ligado a OUTRO arquivo, é erro (conteúdo duplicado no SharePoint)
        assertThat(service.importar(new ImportarSharePointRequest(null, null, true)).get(0).resultado()).isEqualTo(Resultado.ERRO);
    }

    @Test
    void semConfiguracaoDizOQueFalta() {
        when(graph.credenciaisConfiguradas()).thenReturn(false);
        SharePointService semSite = new SharePointService(graph, manualService, manualRepository, categoriaRepository,
            "", "", 0, true, 25);

        assertThat(semSite.status().configurado()).isFalse();
        assertThat(semSite.status().faltando()).contains("SHAREPOINT_TENANT_ID").contains("SHAREPOINT_SITE_URL");
        assertThatThrownBy(semSite::listar).isInstanceOf(RequisicaoInvalidaException.class).hasMessageContaining("SHAREPOINT_SITE_URL");
    }
}
