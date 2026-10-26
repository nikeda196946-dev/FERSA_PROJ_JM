package com.fersacare.api.usuarios;

import com.fersacare.api.common.RecursoNaoEncontradoException;
import com.fersacare.api.common.RequisicaoInvalidaException;
import com.fersacare.api.usuarios.dto.UsuarioAdminDTO;
import com.fersacare.api.usuarios.model.PerfilUsuario;
import com.fersacare.api.usuarios.repository.UsuarioAdminRepository;
import com.fersacare.api.usuarios.service.UsuarioAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UsuarioAdminServiceTest {

    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID MECANICO = UUID.randomUUID();

    private UsuarioAdminRepository repo;
    private UsuarioAdminService service;

    @BeforeEach
    void setUp() {
        repo = mock(UsuarioAdminRepository.class);
        service = new UsuarioAdminService(repo);
    }

    private static UsuarioAdminDTO usuario(UUID id, PerfilUsuario perfil) {
        return new UsuarioAdminDTO(id, "Nome", "x@y.com", perfil, Instant.now(), null, true);
    }

    @Test
    void promoveMecanicoATecnico() {
        when(repo.buscarPorId(MECANICO)).thenReturn(Optional.of(usuario(MECANICO, PerfilUsuario.MECANICO)),
            Optional.of(usuario(MECANICO, PerfilUsuario.TECNICO)));

        assertThat(service.alterarPerfil(MECANICO, PerfilUsuario.TECNICO, ADMIN).perfil()).isEqualTo(PerfilUsuario.TECNICO);
        verify(repo).definirPerfil(MECANICO, PerfilUsuario.TECNICO);
    }

    @Test
    void contaSemProfileGanhaUmQuandoOAdminEscolhe() {
        when(repo.buscarPorId(MECANICO)).thenReturn(Optional.of(usuario(MECANICO, null)),
            Optional.of(usuario(MECANICO, PerfilUsuario.MECANICO)));

        service.alterarPerfil(MECANICO, PerfilUsuario.MECANICO, ADMIN);

        verify(repo).definirPerfil(MECANICO, PerfilUsuario.MECANICO);
    }

    @Test
    void adminNaoTiraOProprioAdmin() {
        when(repo.buscarPorId(ADMIN)).thenReturn(Optional.of(usuario(ADMIN, PerfilUsuario.ADMIN)));

        assertThatThrownBy(() -> service.alterarPerfil(ADMIN, PerfilUsuario.MECANICO, ADMIN))
            .isInstanceOf(RequisicaoInvalidaException.class)
            .hasMessageContaining("seu próprio perfil");
        verify(repo, never()).definirPerfil(any(), any());
    }

    @Test
    void usuarioInexistenteDa404() {
        when(repo.buscarPorId(MECANICO)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.alterarPerfil(MECANICO, PerfilUsuario.TECNICO, ADMIN))
            .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
