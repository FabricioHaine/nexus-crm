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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Regras de negocio da autenticacao: cadastro, login, renovacao e consulta
 * de usuarios.
 */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final SessaoService sessaoService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       SessaoService sessaoService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.sessaoService = sessaoService;
    }

    /**
     * Cadastra um novo usuario, abre a sessao e devolve o token.
     * O perfil definido no cadastro publico e sempre {@code VENDEDOR}.
     */
    @Transactional
    public LoginResponse registrarEEmitir(RegistroRequest request, HttpServletResponse response) {
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

        Usuario salvo = usuarioRepository.save(usuario);
        sessaoService.emitir(salvo, response);
        return resposta(salvo);
    }

    /**
     * Valida as credenciais, abre a sessao e devolve o token.
     */
    @Transactional
    public LoginResponse autenticarEEmitir(LoginRequest request, HttpServletResponse response) {
        String email = request.email().trim().toLowerCase();

        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AutenticacaoFalhouException("E-mail ou senha invalidos."));

        if (!passwordEncoder.matches(request.senha(), usuario.getSenhaHash())) {
            throw new AutenticacaoFalhouException("E-mail ou senha invalidos.");
        }

        if (!usuario.isAtivo()) {
            throw new AutenticacaoFalhouException("Usuario desativado. Procure o administrador do sistema.");
        }

        sessaoService.emitir(usuario, response);
        return resposta(usuario);
    }

    /**
     * Renova a sessao a partir do token de renovacao.
     */
    @Transactional
    public LoginResponse renovar(HttpServletRequest request, HttpServletResponse response) {
        Usuario usuario = sessaoService.renovar(request, response);
        if (usuario == null) {
            throw new AutenticacaoFalhouException("Sessao expirada. Entre novamente.");
        }
        return resposta(usuario);
    }

    /**
     * Troca a senha do usuario autenticado.
     *
     * <p>Todas as outras sessoes do usuario sao revogadas, para que uma senha
     * comprometida nao continue valendo em outro dispositivo.</p>
     */
    @Transactional
    public void trocarSenha(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new AutenticacaoFalhouException("Usuario nao encontrado."));

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenhaHash())) {
            throw new AutenticacaoFalhouException("A senha atual esta incorreta.");
        }

        if (passwordEncoder.matches(novaSenha, usuario.getSenhaHash())) {
            throw new ConflitoDeRegraException("A nova senha deve ser diferente da atual.");
        }

        usuario.setSenhaHash(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);

        sessaoService.revogarSessoesDoUsuario(usuario.getId());
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

    private LoginResponse resposta(Usuario usuario) {
        UsuarioPrincipal principal = UsuarioPrincipal.de(usuario);
        return new LoginResponse(jwtService.gerarToken(principal), "Bearer", jwtService.getExpiracao(),
                paraResponse(usuario));
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
