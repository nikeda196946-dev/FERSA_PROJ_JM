package com.fersacare.api.usuarios.controller;

import com.fersacare.api.security.UsuarioAutenticado;
import com.fersacare.api.usuarios.dto.AlterarPerfilRequest;
import com.fersacare.api.usuarios.dto.UsuarioAdminDTO;
import com.fersacare.api.usuarios.service.UsuarioAdminService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Usuários do CMS (só ADMIN): ver quem se cadastrou e trocar o perfil. */
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    private final UsuarioAdminService service;
    private final UsuarioAutenticado usuarioAutenticado;

    public AdminUsuarioController(UsuarioAdminService service, UsuarioAutenticado usuarioAutenticado) {
        this.service = service;
        this.usuarioAutenticado = usuarioAutenticado;
    }

    @GetMapping
    public List<UsuarioAdminDTO> listar() {
        return service.listar();
    }

    @PatchMapping("/{id}/perfil")
    public UsuarioAdminDTO alterarPerfil(@PathVariable UUID id, @Valid @RequestBody AlterarPerfilRequest req, JwtAuthenticationToken auth) {
        return service.alterarPerfil(id, req.perfil(), usuarioAutenticado.id(auth));
    }
}
