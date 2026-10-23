package com.fersacare.api.manuais.service;

import com.fersacare.api.common.RecursoNaoEncontradoException;
import com.fersacare.api.common.RequisicaoInvalidaException;
import com.fersacare.api.manuais.dto.AtualizarManualRequest;
import com.fersacare.api.manuais.dto.ManualChunkDTO;
import com.fersacare.api.manuais.dto.ManualDTO;
import com.fersacare.api.manuais.repository.CategoriaRepository;
import com.fersacare.api.manuais.repository.ManualChunkRepository;
import com.fersacare.api.manuais.repository.ManualRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Regras do CMS de manuais (área admin) e da listagem pública.
 *
 * O upload grava o manual como 'processando' e devolve na hora; a
 * ingestão (IngestaoService) roda em segundo plano e o front acompanha
 * pelo status em GET /api/admin/manuais/{id}.
 */
@Service
public class ManualService {

    private final ManualRepository manualRepository;
    private final ManualChunkRepository chunkRepository;
    private final CategoriaRepository categoriaRepository;
    private final IngestaoService ingestaoService;

    public ManualService(
        ManualRepository manualRepository,
        ManualChunkRepository chunkRepository,
        CategoriaRepository categoriaRepository,
        IngestaoService ingestaoService
    ) {
        this.manualRepository = manualRepository;
        this.chunkRepository = chunkRepository;
        this.categoriaRepository = categoriaRepository;
        this.ingestaoService = ingestaoService;
    }

    public ManualDTO enviar(MultipartFile arquivo, String titulo, UUID categoriaId) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RequisicaoInvalidaException("Envie um arquivo PDF no campo 'arquivo'.");
        }
        try {
            return enviar(arquivo.getOriginalFilename(), arquivo.getBytes(), titulo, categoriaId);
        } catch (IOException e) {
            throw new RequisicaoInvalidaException("Não foi possível ler o arquivo enviado.");
        }
    }

    /**
     * Upload manual (multipart) e qualquer outra origem que só tenha nome +
     * bytes do PDF. A importação do SharePoint usa importarDoSharePoint.
     */
    public ManualDTO enviar(String nomeOriginal, byte[] pdf, String titulo, UUID categoriaId) {
        if (!PdfExtractor.pareceSerPdf(pdf)) {
            throw new RequisicaoInvalidaException("O arquivo não é um PDF.");
        }
        validarCategoria(categoriaId);

        String nomeArquivo = nomeSeguro(nomeOriginal);
        String tituloFinal = StringUtils.hasText(titulo) ? titulo.strip() : tituloPadrao(nomeArquivo);
        String hash = sha256(pdf);

        manualRepository.buscarPorHash(hash).ifPresent(existente -> {
            throw new RequisicaoInvalidaException("Este PDF já foi enviado como \"" + existente.titulo() + "\".");
        });

        UUID id;
        try {
            id = manualRepository.inserir(tituloFinal, categoriaId, nomeArquivo, pdf, hash);
        } catch (DuplicateKeyException e) {
            // dois uploads do mesmo arquivo ao mesmo tempo — o unique do banco segura
            throw new RequisicaoInvalidaException("Este PDF já foi enviado.");
        }

        // O insert acima já está commitado (não há transação aberta aqui),
        // então a thread da ingestão enxerga o manual.
        ingestaoService.processarEmSegundoPlano(id);
        return buscar(id);
    }

    /**
     * Manual novo vindo do SharePoint (chamado pela sincronização). Mesmo
     * caminho do upload — validação, hash e ingestão em segundo plano —, mas
     * guardando o id do arquivo e o eTag para as próximas sincronizações.
     */
    public ManualDTO importarDoSharePoint(String nomeOriginal, byte[] pdf, UUID categoriaId,
                                          String itemId, String etag, String webUrl, String caminho) {
        if (!PdfExtractor.pareceSerPdf(pdf)) {
            throw new RequisicaoInvalidaException("O arquivo não é um PDF válido.");
        }
        String nomeArquivo = nomeSeguro(nomeOriginal);
        String hash = sha256(pdf);
        manualRepository.buscarPorHash(hash).ifPresent(existente -> {
            throw new RequisicaoInvalidaException("Mesmo conteúdo do manual \"" + existente.titulo() + "\", que já está no sistema.");
        });
        UUID id;
        try {
            id = manualRepository.inserirDoSharePoint(tituloPadrao(nomeArquivo), categoriaId, nomeArquivo, pdf, hash,
                itemId, etag, webUrl, caminho);
        } catch (DuplicateKeyException e) {
            throw new RequisicaoInvalidaException("Este arquivo já foi importado.");
        }
        ingestaoService.processarEmSegundoPlano(id);
        return buscar(id);
    }

    /** Manual que já tem exatamente este PDF (mesmo hash), se houver. */
    public java.util.Optional<ManualDTO> buscarPorConteudo(byte[] pdf) {
        return manualRepository.buscarPorHash(sha256(pdf));
    }

    /** Arquivo alterado no SharePoint: troca o PDF e reprocessa (título e categoria ficam). */
    public ManualDTO atualizarDoSharePoint(UUID id, String nomeOriginal, byte[] pdf,
                                           String etag, String webUrl, String caminho) {
        if (!PdfExtractor.pareceSerPdf(pdf)) {
            throw new RequisicaoInvalidaException("O arquivo não é um PDF válido.");
        }
        String hash = sha256(pdf);
        manualRepository.buscarPorHash(hash).filter(outro -> !outro.id().equals(id)).ifPresent(outro -> {
            throw new RequisicaoInvalidaException("O conteúdo novo é igual ao do manual \"" + outro.titulo() + "\".");
        });
        boolean atualizou;
        try {
            atualizou = manualRepository.atualizarDoSharePoint(id, nomeSeguro(nomeOriginal), pdf, hash, etag, webUrl, caminho);
        } catch (DuplicateKeyException e) {
            throw new RequisicaoInvalidaException("O conteúdo novo é igual ao de outro manual.");
        }
        if (!atualizou) {
            throw new RequisicaoInvalidaException("O manual está sendo processado agora; a próxima sincronização tenta de novo.");
        }
        ingestaoService.processarEmSegundoPlano(id);
        return buscar(id);
    }

    public List<ManualDTO> listarTodos() {
        return manualRepository.listarTodos();
    }

    public ManualDTO buscar(UUID id) {
        return manualRepository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Manual não encontrado."));
    }

    public List<ManualChunkDTO> listarChunks(UUID id) {
        buscar(id);
        return chunkRepository.listarPorManual(id);
    }

    public ManualDTO atualizar(UUID id, AtualizarManualRequest req) {
        validarCategoria(req.categoriaId());
        if (manualRepository.atualizar(id, req.titulo().strip(), req.categoriaId()) == 0) {
            throw new RecursoNaoEncontradoException("Manual não encontrado.");
        }
        return buscar(id);
    }

    public ManualDTO alterarAtivo(UUID id, boolean ativo) {
        if (manualRepository.alterarAtivo(id, ativo) == 0) {
            throw new RecursoNaoEncontradoException("Manual não encontrado.");
        }
        return buscar(id);
    }

    public void excluir(UUID id) {
        if (manualRepository.excluir(id) == 0) {
            throw new RecursoNaoEncontradoException("Manual não encontrado.");
        }
    }

    public ManualDTO reprocessar(UUID id) {
        buscar(id);
        if (!manualRepository.marcarProcessando(id)) {
            throw new RequisicaoInvalidaException("Este manual já está sendo processado.");
        }
        ingestaoService.processarEmSegundoPlano(id);
        return buscar(id);
    }

    /** O PDF de qualquer manual (inclusive inativo ou com erro), para o admin conferir. */
    public ManualRepository.ArquivoPdf baixar(UUID id) {
        return manualRepository.buscarArquivo(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Manual não encontrado."));
    }

    // ----- área pública -----

    public List<ManualDTO> listarPublicos(String termo, UUID categoriaId) {
        return manualRepository.listarPublicos(termo, categoriaId);
    }

    public ManualRepository.ArquivoPdf baixarPublico(UUID id) {
        manualRepository.buscarPublicoPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Manual não encontrado."));
        return manualRepository.buscarArquivo(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Manual não encontrado."));
    }

    // ----- auxiliares -----

    private void validarCategoria(UUID categoriaId) {
        if (categoriaId != null && categoriaRepository.buscarPorId(categoriaId).isEmpty()) {
            throw new RequisicaoInvalidaException("Categoria não encontrada.");
        }
    }

    /** Só o nome do arquivo, sem pasta (alguns navegadores mandam o caminho inteiro). */
    static String nomeSeguro(String nomeOriginal) {
        String nome = StringUtils.getFilename(StringUtils.cleanPath(nomeOriginal == null ? "" : nomeOriginal.replace('\\', '/')));
        if (!StringUtils.hasText(nome)) nome = "manual.pdf";
        return nome.length() > 200 ? nome.substring(nome.length() - 200) : nome;
    }

    static String tituloPadrao(String nomeArquivo) {
        String semExtensao = nomeArquivo.replaceFirst("(?i)\\.pdf$", "");
        String titulo = semExtensao.replaceAll("[_-]+", " ").strip();
        return titulo.isEmpty() ? "Manual sem título" : titulo;
    }

    private static String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // SHA-256 sempre existe na JVM
        }
    }
}
