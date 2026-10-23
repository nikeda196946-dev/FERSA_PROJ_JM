package com.fersacare.api.manuais.controller;

import com.fersacare.api.manuais.dto.CategoriaDTO;
import com.fersacare.api.manuais.dto.CategoriaRequest;
import com.fersacare.api.manuais.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/categorias")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCategoriaController {

    private final CategoriaService categoriaService;

    public AdminCategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public List<CategoriaDTO> listar() {
        return categoriaService.listarTodas();
    }

    @GetMapping("/{id}")
    public CategoriaDTO buscar(@PathVariable UUID id) {
        return categoriaService.buscar(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoriaDTO criar(@Valid @RequestBody CategoriaRequest req) {
        return categoriaService.criar(req);
    }

    @PutMapping("/{id}")
    public CategoriaDTO atualizar(@PathVariable UUID id, @Valid @RequestBody CategoriaRequest req) {
        return categoriaService.atualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable UUID id) {
        categoriaService.excluir(id);
    }
}
