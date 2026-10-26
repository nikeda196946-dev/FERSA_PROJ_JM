package com.fersacare.api.sharepoint;

import com.fersacare.api.common.RequisicaoInvalidaException;
import com.fersacare.api.manuais.dto.ManualDTO;
import com.fersacare.api.manuais.repository.CategoriaRepository;
import com.fersacare.api.manuais.repository.ManualRepository;
import com.fersacare.api.manuais.repository.ManualRepository.VinculoSharePoint;
import com.fersacare.api.manuais.service.ManualService;
import com.fersacare.api.sharepoint.GraphClient.ArquivoRemoto;
import com.fersacare.api.sharepoint.dto.ArquivoSharePointDTO;
import com.fersacare.api.sharepoint.dto.ArquivoSharePointDTO.SituacaoArquivo;
import com.fersacare.api.sharepoint.dto.ImportarSharePointRequest;
import com.fersacare.api.sharepoint.dto.ResultadoImportacaoDTO;
import com.fersacare.api.sharepoint.dto.ResultadoImportacaoDTO.Resultado;
import com.fersacare.api.sharepoint.dto.SharePointStatusDTO;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sincronização SharePoint → manuais.
 *
 * Cada PDF da pasta configurada é comparado com os manuais pelo id do
 * arquivo na Graph:
 *   - NOVO: ainda não importado → baixa e cria o manual (ingestão normal)
 *   - ALTERADO: eTag mudou desde a última importação → baixa e reprocessa
 *   - IMPORTADO: nada mudou → não baixa de novo
 * O título e a categoria editados no CMS são preservados nas atualizações.
 * Arquivo apagado no SharePoint NÃO apaga o manual (o admin decide no CMS).
 *
 * Roda quando o admin clica em "Importar" e, se SHAREPOINT_SYNC_MINUTOS > 0,
 * sozinho de tempos em tempos. Nunca duas ao mesmo tempo.
 */
@Service
public class SharePointService {

    private static final Logger log = LoggerFactory.getLogger(SharePointService.class);

    private final GraphClient graph;
    private final ManualService manualService;
    private final ManualRepository manualRepository;
    private final CategoriaRepository categoriaRepository;
    private final String siteUrl;
    private final String pasta;
    private final int sincronizacaoMinutos;
    private final boolean categoriaPelaPastaPadrao;
    private final long tamanhoMaximoBytes;

    private final ReentrantLock emAndamento = new ReentrantLock();
    private volatile Instant ultimaSincronizacao;
    private volatile String resumoUltimaSincronizacao;
    private ScheduledExecutorService agendador;

    public SharePointService(
        GraphClient graph,
        ManualService manualService,
        ManualRepository manualRepository,
        CategoriaRepository categoriaRepository,
        @Value("${fersa.sharepoint.site-url:}") String siteUrl,
        @Value("${fersa.sharepoint.pasta:}") String pasta,
        @Value("${fersa.sharepoint.sincronizacao-minutos:0}") int sincronizacaoMinutos,
        @Value("${fersa.sharepoint.categoria-pela-pasta:true}") boolean categoriaPelaPastaPadrao,
        @Value("${fersa.sharepoint.tamanho-maximo-mb:25}") int tamanhoMaximoMb
    ) {
        this.graph = graph;
        this.manualService = manualService;
        this.manualRepository = manualRepository;
        this.categoriaRepository = categoriaRepository;
        this.siteUrl = siteUrl == null ? "" : siteUrl.strip();
        this.pasta = pasta == null ? "" : pasta.strip();
        this.sincronizacaoMinutos = Math.max(0, sincronizacaoMinutos);
        this.categoriaPelaPastaPadrao = categoriaPelaPastaPadrao;
        this.tamanhoMaximoBytes = tamanhoMaximoMb * 1024L * 1024L;
    }

    public boolean configurado() {
        return graph.credenciaisConfiguradas() && !siteUrl.isEmpty();
    }

    public SharePointStatusDTO status() {
        List<String> faltando = new ArrayList<>();
        if (!graph.credenciaisConfiguradas()) faltando.add("SHAREPOINT_TENANT_ID / SHAREPOINT_CLIENT_ID / SHAREPOINT_CLIENT_SECRET");
        if (siteUrl.isEmpty()) faltando.add("SHAREPOINT_SITE_URL");
        return new SharePointStatusDTO(configurado(), siteUrl.isEmpty() ? null : siteUrl, pasta, sincronizacaoMinutos,
            ultimaSincronizacao, resumoUltimaSincronizacao, faltando.isEmpty() ? null : String.join(", ", faltando));
    }

    public boolean categoriaPelaPastaPadrao() {
        return categoriaPelaPastaPadrao;
    }

    /** Os PDFs da pasta, com a situação de cada um em relação aos manuais já importados. */
    public List<ArquivoSharePointDTO> listar() {
        exigirConfiguracao();
        GraphClient.Local local = graph.resolver(siteUrl, pasta);
        return classificar(graph.listarPdfs(local), manualRepository.listarVinculosSharePoint(), tamanhoMaximoBytes)
            .stream().map(Item::dto).toList();
    }

    public List<ResultadoImportacaoDTO> importar(ImportarSharePointRequest req) {
        exigirConfiguracao();
        if (!emAndamento.tryLock()) {
            throw new RequisicaoInvalidaException("Já existe uma importação do SharePoint em andamento. Aguarde terminar.");
        }
        try {
            List<ResultadoImportacaoDTO> resultados = executar(req);
            ultimaSincronizacao = Instant.now();
            resumoUltimaSincronizacao = resumir(resultados);
            return resultados;
        } finally {
            emAndamento.unlock();
        }
    }

    private List<ResultadoImportacaoDTO> executar(ImportarSharePointRequest req) {
        UUID categoriaFixa = req == null ? null : req.categoriaId();
        if (categoriaFixa != null && categoriaRepository.buscarPorId(categoriaFixa).isEmpty()) {
            throw new RequisicaoInvalidaException("Categoria não encontrada.");
        }
        boolean pelaPasta = req != null && req.categoriaPelaPasta() != null ? req.categoriaPelaPasta() : categoriaPelaPastaPadrao;
        Set<String> escolhidos = req == null || req.itemIds() == null || req.itemIds().isEmpty() ? null : new HashSet<>(req.itemIds());

        GraphClient.Local local = graph.resolver(siteUrl, pasta);
        List<Item> itens = classificar(graph.listarPdfs(local), manualRepository.listarVinculosSharePoint(), tamanhoMaximoBytes);
        Map<String, UUID> categoriasCriadas = new HashMap<>();
        List<ResultadoImportacaoDTO> resultados = new ArrayList<>();

        for (Item item : itens) {
            ArquivoSharePointDTO a = item.dto();
            if (escolhidos != null ? !escolhidos.remove(a.itemId())
                : a.situacao() != SituacaoArquivo.NOVO && a.situacao() != SituacaoArquivo.ALTERADO) {
                continue;
            }
            switch (a.situacao()) {
                case GRANDE_DEMAIS -> resultados.add(new ResultadoImportacaoDTO(a.itemId(), a.nome(), Resultado.IGNORADO,
                    "Maior que o limite de " + tamanhoMaximoBytes / (1024 * 1024) + " MB.", null));
                case IMPORTADO -> resultados.add(new ResultadoImportacaoDTO(a.itemId(), a.nome(), Resultado.SEM_ALTERACAO,
                    "Já importado e sem alterações no SharePoint.", a.manualId()));
                case NOVO, ALTERADO -> resultados.add(baixarEGravar(local, item, pelaPasta ? null : categoriaFixa,
                    pelaPasta, categoriaFixa, categoriasCriadas));
            }
        }
        if (escolhidos != null) {
            escolhidos.forEach(id -> resultados.add(new ResultadoImportacaoDTO(id, null, Resultado.ERRO,
                "Arquivo não encontrado na pasta do SharePoint (apagado ou movido?).", null)));
        }
        return resultados;
    }

    private ResultadoImportacaoDTO baixarEGravar(GraphClient.Local local, Item item, UUID categoria, boolean pelaPasta,
                                                 UUID categoriaFixa, Map<String, UUID> categoriasCriadas) {
        ArquivoRemoto r = item.remoto();
        try {
            byte[] pdf = graph.baixar(local, r.itemId());
            if (item.dto().situacao() == SituacaoArquivo.ALTERADO) {
                ManualDTO m = manualService.atualizarDoSharePoint(item.dto().manualId(), r.nome(), pdf, r.etag(), r.webUrl(), r.caminho());
                return new ResultadoImportacaoDTO(r.itemId(), r.nome(), Resultado.ATUALIZADO,
                    "Arquivo alterado no SharePoint: PDF trocado e reprocessando.", m.id());
            }
            // Mesmo PDF já no sistema (ex.: enviado à mão antes de existir o SharePoint):
            // liga o manual existente a este arquivo em vez de duplicar — e antes de
            // criar categoria, para não sobrar categoria vazia.
            java.util.Optional<ManualDTO> igual = manualService.buscarPorConteudo(pdf);
            if (igual.isPresent()) {
                ManualDTO m = igual.get();
                if (manualRepository.vincularAoSharePoint(m.id(), r.itemId(), r.etag(), r.webUrl(), r.caminho())) {
                    return new ResultadoImportacaoDTO(r.itemId(), r.nome(), Resultado.VINCULADO,
                        "Já estava no sistema como \"" + m.titulo() + "\"; agora acompanha este arquivo do SharePoint.", m.id());
                }
                return new ResultadoImportacaoDTO(r.itemId(), r.nome(), Resultado.ERRO,
                    "Mesmo conteúdo do manual \"" + m.titulo() + "\", que já vem de outro arquivo do SharePoint.", m.id());
            }
            UUID categoriaId = pelaPasta ? categoriaDaPasta(r.caminho(), categoriaFixa, categoriasCriadas) : categoria;
            ManualDTO m = manualService.importarDoSharePoint(r.nome(), pdf, categoriaId, r.itemId(), r.etag(), r.webUrl(), r.caminho());
            return new ResultadoImportacaoDTO(r.itemId(), r.nome(), Resultado.IMPORTADO,
                "Importado" + (m.categoriaNome() != null ? " em \"" + m.categoriaNome() + "\"" : "") + "; processando.", m.id());
        } catch (RequisicaoInvalidaException | SharePointException e) {
            log.warn("SharePoint: falha em {}: {}", r.nome(), e.getMessage());
            return new ResultadoImportacaoDTO(r.itemId(), r.nome(), Resultado.ERRO, e.getMessage(), item.dto().manualId());
        }
    }

    /**
     * Subpasta "Rolamentos de Cubo/2026" → categoria "Rolamentos de Cubo"
     * (o primeiro nível), criada se ainda não existir. Arquivo direto na
     * pasta sincronizada fica com a categoria escolhida na tela (ou nenhuma).
     */
    private UUID categoriaDaPasta(String caminho, UUID categoriaFixa, Map<String, UUID> cache) {
        if (caminho == null || caminho.isBlank()) return categoriaFixa;
        String nome = caminho.split("/")[0].strip();
        if (nome.isEmpty()) return categoriaFixa;
        return cache.computeIfAbsent(nome.toLowerCase(), k -> categoriaRepository.buscarPorNome(nome)
            .map(c -> c.id())
            .orElseGet(() -> categoriaRepository.inserir(nome, "Criada pela importação do SharePoint (pasta \"" + nome + "\").", true)));
    }

    // ----- sincronização automática -----

    @EventListener(ApplicationReadyEvent.class)
    public void iniciarSincronizacaoAutomatica() {
        if (sincronizacaoMinutos <= 0) return;
        if (!configurado()) {
            log.warn("SHAREPOINT_SYNC_MINUTOS={} mas o SharePoint não está configurado — sincronização automática desligada.", sincronizacaoMinutos);
            return;
        }
        agendador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "sharepoint-sync");
            t.setDaemon(true);
            return t;
        });
        agendador.scheduleWithFixedDelay(this::sincronizarAgora, 1, sincronizacaoMinutos, TimeUnit.MINUTES);
        log.info("Sincronização automática com o SharePoint a cada {} min.", sincronizacaoMinutos);
    }

    void sincronizarAgora() {
        try {
            List<ResultadoImportacaoDTO> r = importar(new ImportarSharePointRequest(null, null, null));
            if (!r.isEmpty()) log.info("Sincronização SharePoint: {}", resumir(r));
        } catch (Exception e) {
            resumoUltimaSincronizacao = "Falhou: " + e.getMessage();
            ultimaSincronizacao = Instant.now();
            log.warn("Sincronização SharePoint falhou: {}", e.getMessage());
        }
    }

    @PreDestroy
    public void parar() {
        if (agendador != null) agendador.shutdownNow();
    }

    // ----- auxiliares -----

    record Item(ArquivoRemoto remoto, ArquivoSharePointDTO dto) {}

    static List<Item> classificar(List<ArquivoRemoto> remotos, List<VinculoSharePoint> vinculos, long tamanhoMaximo) {
        Map<String, VinculoSharePoint> porItem = vinculos.stream()
            .collect(Collectors.toMap(VinculoSharePoint::itemId, Function.identity(), (a, b) -> a));
        List<Item> itens = new ArrayList<>(remotos.size());
        for (ArquivoRemoto r : remotos) {
            VinculoSharePoint v = porItem.get(r.itemId());
            SituacaoArquivo situacao;
            if (r.tamanho() > tamanhoMaximo) situacao = SituacaoArquivo.GRANDE_DEMAIS;
            else if (v == null) situacao = SituacaoArquivo.NOVO;
            else if (r.etag() != null && !r.etag().equals(v.etag())) situacao = SituacaoArquivo.ALTERADO;
            else situacao = SituacaoArquivo.IMPORTADO;
            itens.add(new Item(r, new ArquivoSharePointDTO(r.itemId(), r.nome(), r.caminho(), r.tamanho(), r.modificadoEm(),
                r.webUrl(), situacao, v == null ? null : v.manualId(), v == null ? null : v.titulo(), v == null ? null : v.status())));
        }
        return itens;
    }

    static String resumir(List<ResultadoImportacaoDTO> resultados) {
        if (resultados.isEmpty()) return "Nada novo ou alterado no SharePoint.";
        Map<Resultado, Long> n = resultados.stream().collect(Collectors.groupingBy(ResultadoImportacaoDTO::resultado, Collectors.counting()));
        List<String> partes = new ArrayList<>();
        if (n.containsKey(Resultado.IMPORTADO)) partes.add(n.get(Resultado.IMPORTADO) + " importado(s)");
        if (n.containsKey(Resultado.ATUALIZADO)) partes.add(n.get(Resultado.ATUALIZADO) + " atualizado(s)");
        if (n.containsKey(Resultado.VINCULADO)) partes.add(n.get(Resultado.VINCULADO) + " ligado(s) a manual existente");
        if (n.containsKey(Resultado.SEM_ALTERACAO)) partes.add(n.get(Resultado.SEM_ALTERACAO) + " sem alteração");
        if (n.containsKey(Resultado.IGNORADO)) partes.add(n.get(Resultado.IGNORADO) + " ignorado(s)");
        if (n.containsKey(Resultado.ERRO)) partes.add(n.get(Resultado.ERRO) + " com erro");
        return String.join(", ", partes) + ".";
    }

    private void exigirConfiguracao() {
        if (!configurado()) {
            throw new RequisicaoInvalidaException("SharePoint não configurado. Falta no .env do backend: " + status().faltando() + ".");
        }
    }
}
