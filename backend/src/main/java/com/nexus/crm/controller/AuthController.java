package com.nexus.crm.controller;

import com.nexus.crm.dto.LoginRequest;
import com.nexus.crm.dto.LoginResponse;
import com.nexus.crm.dto.RecuperarSenhaRequest;
import com.nexus.crm.dto.RedefinirSenhaRequest;
import com.nexus.crm.dto.RegistroRequest;
import com.nexus.crm.dto.TrocarSenhaRequest;
import com.nexus.crm.dto.UsuarioResponse;
import com.nexus.crm.service.AuthService;
import com.nexus.crm.service.RecuperacaoSenhaService;
import com.nexus.crm.service.SessaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
 *
 * <p>A resposta traz o token no corpo (para clientes stateless como curl e
 * Swagger) e tambem grava a sessao em cookies {@code HttpOnly}, que e o
 * caminho usado pela interface web.</p>
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacao", description = "Login, cadastro, renovacao e encerramento de sessao")
public class AuthController {

    private final AuthService authService;
    private final SessaoService sessaoService;
    private final RecuperacaoSenhaService recuperacaoSenhaService;

    public AuthController(AuthService authService,
                          SessaoService sessaoService,
                          RecuperacaoSenhaService recuperacaoSenhaService) {
        this.authService = authService;
        this.sessaoService = sessaoService;
        this.recuperacaoSenhaService = recuperacaoSenhaService;
    }

    @PostMapping("/login")
    @Operation(summary = "Autentica o usuario e abre a sessao")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return authService.autenticarEEmitir(request, response);
    }

    @PostMapping("/registro")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo usuario (perfil inicial: Vendedor) e abre a sessao")
    public LoginResponse registro(@Valid @RequestBody RegistroRequest request, HttpServletResponse response) {
        return authService.registrarEEmitir(request, response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renova a sessao a partir do token de renovacao")
    public LoginResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        return authService.renovar(request, response);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Encerra a sessao e revoga o token de renovacao")
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        sessaoService.encerrar(request, response);
    }

    @GetMapping("/me")
    @Operation(summary = "Devolve os dados do usuario autenticado")
    public UsuarioResponse sessaoAtual(Authentication autenticacao) {
        return authService.usuarioAtual(autenticacao.getName());
    }

    @PostMapping("/trocar-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Troca a senha do usuario autenticado e revoga as outras sessoes")
    public void trocarSenha(@Valid @RequestBody TrocarSenhaRequest request, Authentication autenticacao) {
        authService.trocarSenha(autenticacao.getName(), request.senhaAtual(), request.novaSenha());
    }

    @PostMapping("/recuperar-senha")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Envia o link de recuperacao de senha (nao revela se o e-mail existe)")
    public void recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest request) {
        recuperacaoSenhaService.solicitarRecuperacao(request.email());
    }

    @PostMapping("/redefinir-senha")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Redefine a senha com o token de recuperacao")
    public void redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        recuperacaoSenhaService.redefinirSenha(request.token(), request.novaSenha());
    }
}
