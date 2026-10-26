package com.fersacare.api.usuarios.service;

import com.fersacare.api.common.RecursoNaoEncontradoException;
import com.fersacare.api.common.RequisicaoInvalidaException;
import com.fersacare.api.usuarios.dto.UsuarioAdminDTO;
import com.fersacare.api.usuarios.model.PerfilUsuario;
import com.fersacare.api.usuarios.repository.UsuarioAdminRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Gestão de perfis pelo admin. Quem se cadastra entra como MECANICO
 * (database/cadastro.sql); é aqui que alguém vira TECNICO ou ADMIN.
 */
@Service
public class UsuarioAdminService {

    private final UsuarioAdminRepository repository;

    public UsuarioAdminService(UsuarioAdminRepository repository) {
        this.repository = repository;
    }

    public List<UsuarioAdminDTO> listar() {
        return repository.listar();
    }

    public UsuarioAdminDTO alterarPerfil(UUID id, PerfilUsuario perfil, UUID adminLogado) {
        UsuarioAdminDTO usuario = repository.buscarPorId(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        // Evita o sistema ficar sem administrador por um clique errado.
        if (id.equals(adminLogado) && perfil != PerfilUsuario.ADMIN) {
            throw new RequisicaoInvalidaException("Você não pode tirar o seu próprio perfil de administrador. Peça a outro admin.");
        }
        if (perfil != usuario.perfil()) {
            repository.definirPerfil(id, perfil);
        }
        return repository.buscarPorId(id).orElseThrow();
    }
}
