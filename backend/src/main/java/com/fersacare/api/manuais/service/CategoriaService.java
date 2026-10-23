package com.fersacare.api.manuais.service;

import com.fersacare.api.common.RecursoNaoEncontradoException;
import com.fersacare.api.common.RequisicaoInvalidaException;
import com.fersacare.api.manuais.dto.CategoriaDTO;
import com.fersacare.api.manuais.dto.CategoriaRequest;
import com.fersacare.api.manuais.repository.CategoriaRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * CRUD de categorias. Desativar uma categoria tira os manuais dela da
 * busca e do chat (sem precisar desativar manual por manual); excluir
 * deixa os manuais sem categoria.
 */
@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<CategoriaDTO> listarTodas() {
        return categoriaRepository.listarTodas();
    }

    public List<CategoriaDTO> listarAtivas() {
        return categoriaRepository.listarAtivas();
    }

    public CategoriaDTO buscar(UUID id) {
        return categoriaRepository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada."));
    }

    public CategoriaDTO criar(CategoriaRequest req) {
        try {
            UUID id = categoriaRepository.inserir(req.nome().strip(), descricao(req), req.ativo() == null || req.ativo());
            return buscar(id);
        } catch (DuplicateKeyException e) {
            throw nomeDuplicado(req);
        }
    }

    public CategoriaDTO atualizar(UUID id, CategoriaRequest req) {
        int alterados;
        try {
            alterados = categoriaRepository.atualizar(id, req.nome().strip(), descricao(req), req.ativo() == null || req.ativo());
        } catch (DuplicateKeyException e) {
            throw nomeDuplicado(req);
        }
        if (alterados == 0) {
            throw new RecursoNaoEncontradoException("Categoria não encontrada.");
        }
        return buscar(id);
    }

    public void excluir(UUID id) {
        if (categoriaRepository.excluir(id) == 0) {
            throw new RecursoNaoEncontradoException("Categoria não encontrada.");
        }
    }

    private static String descricao(CategoriaRequest req) {
        return req.descricao() == null || req.descricao().isBlank() ? null : req.descricao().strip();
    }

    private static RequisicaoInvalidaException nomeDuplicado(CategoriaRequest req) {
        return new RequisicaoInvalidaException("Já existe uma categoria chamada \"" + req.nome().strip() + "\".");
    }
}
