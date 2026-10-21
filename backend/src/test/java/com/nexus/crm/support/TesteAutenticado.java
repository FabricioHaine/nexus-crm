package com.nexus.crm.support;

import com.nexus.crm.model.Papel;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.UsuarioRepository;
import com.nexus.crm.security.JwtService;
import com.nexus.crm.security.UsuarioPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base dos testes de integracao que precisam de um usuario autenticado.
 *
 * <p>Em vez de simular a autenticacao, o helper emite um token JWT de verdade
 * para um usuario criado no banco. Assim o filtro de seguranca, o conversor do
 * token e as regras de perfil sao exercitados de ponta a ponta.</p>
 */
public abstract class TesteAutenticado {

    private static final AtomicInteger CONTADOR = new AtomicInteger();

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    /** Requisicao autenticada com um perfil especifico. */
    protected MockHttpServletRequestBuilder autenticadoComo(Papel papel, MockHttpServletRequestBuilder requisicao) {
        return requisicao.header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenDe(papel));
    }

    /** Requisicao autenticada com o perfil de administrador. */
    protected MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder requisicao) {
        return autenticadoComo(Papel.ADMIN, requisicao);
    }

    /** Token JWT (sem o prefixo "Bearer") de um usuario recem-criado com o perfil informado. */
    protected String tokenDe(Papel papel) {
        String email = "teste-" + papel.name().toLowerCase() + "-" + CONTADOR.incrementAndGet() + "@empresa.com.br";

        Usuario usuario = new Usuario();
        usuario.setNome("Usuario de Teste " + papel);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode("Senha@123"));
        usuario.setPapel(papel);
        usuario.setAtivo(true);
        usuario = usuarioRepository.save(usuario);

        return jwtService.gerarToken(UsuarioPrincipal.de(usuario));
    }

    /** Token JWT (sem o prefixo "Bearer") de um usuario ja existente. */
    protected String tokenDo(Usuario usuario) {
        return jwtService.gerarToken(UsuarioPrincipal.de(usuario));
    }
}
