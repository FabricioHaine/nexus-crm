package com.nexus.crm.controller;

import com.nexus.crm.dto.AlterarUsuarioRequest;
import com.nexus.crm.dto.UsuarioResponse;
import com.nexus.crm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints administrativos de gestao de usuarios (restritos ao perfil ADMIN).
 */
@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Gestao de usuarios e perfis de acesso")
public class UsuarioController {

    private final AuthService authService;

    public UsuarioController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping
    @Operation(summary = "Lista os usuarios cadastrados")
    public List<UsuarioResponse> listar() {
        return authService.listarUsuarios();
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Altera o perfil de acesso ou a situacao de um usuario")
    public UsuarioResponse alterar(@PathVariable Long id, @RequestBody AlterarUsuarioRequest request) {
        return authService.alterarUsuario(id, request.papel(), request.ativo());
    }
}
