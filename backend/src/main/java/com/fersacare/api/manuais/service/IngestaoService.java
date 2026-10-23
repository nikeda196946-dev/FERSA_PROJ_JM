package com.fersacare.api.manuais.service;

import com.fersacare.api.ia.GeminiClient;
import com.fersacare.api.manuais.dto.ManualDTO;
import com.fersacare.api.manuais.repository.ManualChunkRepository;
import com.fersacare.api.manuais.repository.ManualChunkRepository.NovoChunk;
import com.fersacare.api.manuais.repository.ManualRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Pipeline da ingestão: PDF → PDFBox → chunks → embeddings (Gemini) → banco,
 * atualizando o status do manual (processando → pronto | erro).
 *
 * Roda em segundo plano (@Async, pool "ingestaoExecutor"). Quem chama é o
 * ManualService, depois de já ter gravado o manual com status 'processando'
 * — por isso aqui só lemos o PDF do banco pelo id, tanto no primeiro
 * processamento quanto no "reprocessar".
 *
 * As chamadas ao Gemini ficam FORA da transação; só a troca dos chunks +
 * status 'pronto' é transacional. Assim um reprocessamento que falha no
 * meio não apaga os chunks antigos: o manual só fica com status 'erro'.
 */
@Service
public class IngestaoService {

    private static final Logger log = LoggerFactory.getLogger(IngestaoService.class);

    private final ManualRepository manualRepository;
    private final ManualChunkRepository chunkRepository;
    private final PdfExtractor pdfExtractor;
    private final Chunker chunker;
    private final GeminiClient gemini;
    private final TransactionTemplate transacao;

    public IngestaoService(
        ManualRepository manualRepository,
        ManualChunkRepository chunkRepository,
        PdfExtractor pdfExtractor,
        Chunker chunker,
        GeminiClient gemini,
        TransactionTemplate transacao
    ) {
        this.manualRepository = manualRepository;
        this.chunkRepository = chunkRepository;
        this.pdfExtractor = pdfExtractor;
        this.chunker = chunker;
        this.gemini = gemini;
        this.transacao = transacao;
    }

    @Async("ingestaoExecutor")
    public void processarEmSegundoPlano(UUID manualId) {
        processar(manualId);
    }

    /** Versão síncrona (útil em teste). Nunca lança exceção: erro vira status 'erro' no manual. */
    public void processar(UUID manualId) {
        long inicio = System.currentTimeMillis();
        try {
            ManualDTO manual = manualRepository.buscarPorId(manualId)
                .orElseThrow(() -> new IllegalStateException("manual não existe mais"));
            byte[] pdf = manualRepository.buscarArquivo(manualId)
                .orElseThrow(() -> new IllegalStateException("manual não existe mais"))
                .conteudo();

            PdfExtractor.Resultado extraido = pdfExtractor.extrair(pdf);
            List<Chunker.Pedaco> pedacos = chunker.dividir(extraido.paginas());
            if (pedacos.isEmpty()) {
                manualRepository.marcarErro(manualId, "Nenhum texto encontrado no PDF ("
                    + extraido.totalPaginas() + " página(s)). Provavelmente é um PDF escaneado (só imagem) — "
                    + "use uma versão com texto selecionável.");
                return;
            }

            List<float[]> vetores = gemini.embedDocumentos(
                manual.titulo(), pedacos.stream().map(Chunker.Pedaco::texto).toList());

            List<NovoChunk> chunks = new ArrayList<>(pedacos.size());
            for (int i = 0; i < pedacos.size(); i++) {
                Chunker.Pedaco p = pedacos.get(i);
                chunks.add(new NovoChunk(p.ordem(), p.pagina(), p.texto(), vetores.get(i)));
            }

            transacao.executeWithoutResult(status -> {
                chunkRepository.substituir(manualId, chunks);
                manualRepository.marcarPronto(manualId, extraido.totalPaginas(), chunks.size());
            });
            log.info("Manual {} ('{}') pronto: {} páginas, {} chunks, {} ms",
                manualId, manual.titulo(), extraido.totalPaginas(), chunks.size(), System.currentTimeMillis() - inicio);
        } catch (Exception e) {
            log.warn("Falha ao processar manual {}: {}", manualId, e.getMessage(), e);
            try {
                manualRepository.marcarErro(manualId, resumir(e.getMessage()));
            } catch (Exception e2) {
                log.error("Não consegui nem marcar o manual {} como erro", manualId, e2);
            }
        }
    }

    /**
     * Se a API reiniciar no meio de uma ingestão, o manual ficaria
     * 'processando' para sempre. Na subida, esses viram 'erro' com uma
     * mensagem pedindo para reprocessar.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void marcarInterrompidos() {
        try {
            int n = manualRepository.marcarInterrompidosComoErro();
            if (n > 0) {
                log.warn("{} manual(is) estavam 'processando' quando a API caiu — marcados como erro", n);
            }
        } catch (Exception e) {
            // Não derruba a API (catálogo e chamados continuam funcionando) —
            // o caso típico é database/manuais.sql ainda não ter sido rodado.
            log.warn("Não consegui verificar manuais interrompidos ({}). Rodou database/manuais.sql?", e.getMessage());
        }
    }

    private static String resumir(String mensagem) {
        if (mensagem == null || mensagem.isBlank()) return "Erro inesperado no processamento.";
        return mensagem.length() > 500 ? mensagem.substring(0, 500) + "…" : mensagem;
    }
}
