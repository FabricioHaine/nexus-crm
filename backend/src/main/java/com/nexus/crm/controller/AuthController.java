package com.nexus.crm.controller;

import com.nexus.crm.dto.LoginRequest;
import com.nexus.crm.dto.LoginResponse;
import com.nexus.crm.dto.RegistroRequest;
import com.nexus.crm.dto.UsuarioResponse;
import com.nexus.crm.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticacao.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao", description = "Login, cadastro e consulta da sessao")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuario e devolve o token de acesso")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.autenticar(request);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo usuario (perfil inicial: Vendedor)")
    public LoginResponse registro(@Valid @RequestBody RegistroRequest request) {
        return authService.registrar(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Devolve os dados do usuario autenticado")
    public UsuarioResponse sessaoAtual(Authentication autenticacao) {
        return authService.usuarioAtual(autenticacao.getName());
    }
}
