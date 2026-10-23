package com.fersacare.api.manuais.controller;

import com.fersacare.api.manuais.dto.CategoriaDTO;
import com.fersacare.api.manuais.dto.ManualDTO;
import com.fersacare.api.manuais.repository.ManualRepository;
import com.fersacare.api.manuais.service.CategoriaService;
import com.fersacare.api.manuais.service.ManualService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * Área pública (sem login): busca de manuais, categorias e o PDF para
 * abrir a fonte citada pelo chat. Só aparecem manuais ativos, prontos e
 * de categoria ativa.
 */
@RestController
@RequestMapping("/api")
public class ManualPublicoController {

    private final ManualService manualService;
    private final CategoriaService categoriaService;

    public ManualPublicoController(ManualService manualService, CategoriaService categoriaService) {
        this.manualService = manualService;
        this.categoriaService = categoriaService;
    }

    @GetMapping("/manuais")
    public List<ManualDTO> buscar(
        @RequestParam(required = false) String termo,
        @RequestParam(required = false) UUID categoriaId
    ) {
        return manualService.listarPublicos(termo, categoriaId);
    }

    @GetMapping("/manuais/{id}/pdf")
    public ResponseEntity<byte[]> baixarPdf(@PathVariable UUID id) {
        ManualRepository.ArquivoPdf arquivo = manualService.baixarPublico(id);
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                .filename(arquivo.nomeArquivo(), StandardCharsets.UTF_8).build().toString())
            .body(arquivo.conteudo());
    }

    @GetMapping("/categorias")
    public List<CategoriaDTO> categorias() {
        return categoriaService.listarAtivas();
    }
}
