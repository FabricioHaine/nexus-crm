package com.nexus.crm.service;

import com.nexus.crm.dto.LoginRequest;
import com.nexus.crm.dto.LoginResponse;
import com.nexus.crm.dto.RegistroRequest;
import com.nexus.crm.dto.UsuarioResponse;
import com.nexus.crm.exception.AutenticacaoFalhouException;
import com.nexus.crm.exception.ConflitoDeRegraException;
import com.nexus.crm.model.Papel;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.UsuarioRepository;
import com.nexus.crm.security.JwtService;
import com.nexus.crm.security.UsuarioPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negocio da autenticacao: cadastro, login e consulta de usuarios.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * Cadastra um novo usuario e devolve o token de acesso.
     * O perfil definido no cadastro publico e sempre {@code VENDEDOR}.
     */
    @Transactional
    public LoginResponse registrar(RegistroRequest request) {
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflitoDeRegraException("Ja existe um usuario cadastrado com o e-mail " + email + ".");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        usuario.setPapel(Papel.VENDEDOR);
        usuario.setAtivo(true);

        return emitirToken(usuarioRepository.save(usuario));
    }

    /**
     * Valida as credenciais e devolve o token de acesso.
     */
    @Transactional(readOnly = true)
    public LoginResponse autenticar(LoginRequest request) {
        String email = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AutenticacaoFalhouException("E-mail ou senha invalidos."));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new AutenticacaoFalhouException("E-mail ou senha invalidos.");
        }

        if (!usuario.isAtivo()) {
            throw new AutenticacaoFalhouException("Usuario desativado. Procure o administrador do sistema.");
        }

        return emitirToken(usuario);
    }

    /**
     * Devolve os dados do usuario autenticado, recarregados do banco.
     */
    @Transactional(readOnly = true)
    public UsuarioResponse usuarioAtual(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email)
                .map(AuthService::paraResponse)
                .orElseThrow(() -> new AutenticacaoFalhouException("Usuario nao encontrado."));
    }

    /**
     * Lista todos os usuarios cadastrados (uso restrito ao ADMIN).
     */
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listarUsuarios() {
        return usuarioRepository.findAllByOrderByNomeAsc().stream()
                .map(AuthService::paraResponse)
                .toList();
    }

    /**
     * Altera o perfil de acesso e a situacao de um usuario (uso restrito ao ADMIN).
     */
    @Transactional
    public UsuarioResponse alterarUsuario(Long id, Papel papel, Boolean ativo) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new com.nexus.crm.exception.RecursoNaoEncontradoException(
                        "Usuario nao encontrado com o id " + id + "."));

        if (papel != null) {
            usuario.setPapel(papel);
        }
        if (ativo != null) {
            usuario.setAtivo(ativo);
        }

        return paraResponse(usuarioRepository.save(usuario));
    }

    private LoginResponse emitirToken(Usuario usuario) {
        UsuarioPrincipal principal = UsuarioPrincipal.de(usuario);
        String token = jwtService.gerarToken(principal);
        return new LoginResponse(token, "Bearer", jwtService.getExpiracao(), paraResponse(usuario));
    }

    private static UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getPapel(),
                usuario.isAtivo(),
                usuario.getDataCriacao()
        );
    }
}
