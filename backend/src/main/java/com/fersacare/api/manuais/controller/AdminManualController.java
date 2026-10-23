package com.fersacare.api.manuais.controller;

import com.fersacare.api.manuais.dto.AlterarAtivoRequest;
import com.fersacare.api.manuais.dto.AtualizarManualRequest;
import com.fersacare.api.manuais.dto.ManualChunkDTO;
import com.fersacare.api.manuais.dto.ManualDTO;
import com.fersacare.api.manuais.service.ManualService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * CMS de manuais. Só ADMIN — a regra está aqui (@PreAuthorize) e também
 * no SecurityConfig (/api/admin/**), de propósito em dobro.
 */
@RestController
@RequestMapping("/api/admin/manuais")
@PreAuthorize("hasRole('ADMIN')")
public class AdminManualController {

    private final ManualService manualService;

    public AdminManualController(ManualService manualService) {
        this.manualService = manualService;
    }

    /**
     * Upload: multipart/form-data com "arquivo" (PDF) e, opcionalmente,
     * "titulo" e "categoriaId". Responde 202 com o manual em PROCESSANDO;
     * o front consulta GET /{id} até virar PRONTO ou ERRO.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ManualDTO enviar(
        @RequestParam("arquivo") MultipartFile arquivo,
        @RequestParam(required = false) String titulo,
        @RequestParam(required = false) UUID categoriaId
    ) {
        return manualService.enviar(arquivo, titulo, categoriaId);
    }

    @GetMapping
    public List<ManualDTO> listar() {
        return manualService.listarTodos();
    }

    @GetMapping("/{id}")
    public ManualDTO buscar(@PathVariable UUID id) {
        return manualService.buscar(id);
    }

    /** Para conferir a ingestão: o texto de cada chunk e a dimensão do vetor gravado. */
    @GetMapping("/{id}/chunks")
    public List<ManualChunkDTO> listarChunks(@PathVariable UUID id) {
        return manualService.listarChunks(id);
    }

    @PutMapping("/{id}")
    public ManualDTO atualizar(@PathVariable UUID id, @Valid @RequestBody AtualizarManualRequest req) {
        return manualService.atualizar(id, req);
    }

    @PatchMapping("/{id}/ativo")
    public ManualDTO alterarAtivo(@PathVariable UUID id, @Valid @RequestBody AlterarAtivoRequest req) {
        return manualService.alterarAtivo(id, req.ativo());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        manualService.excluir(id);
    }

    @PostMapping("/{id}/reprocessar")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ManualDTO reprocessar(@PathVariable UUID id) {
        return manualService.reprocessar(id);
    }
}
