package com.nexus.crm.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Collection;

/**
 * Converte o JWT validado em uma autenticacao do Spring Security.
 *
 * <p>Diferente do conversor padrao, o usuario e recarregado do banco a cada
 * requisicao e as autoridades vem da entidade {@code Usuario}. Assim, alterar
 * o perfil de acesso ou desativar um usuario tem efeito imediato, sem esperar
 * o token expirar.</p>
 */
public class ConversorJwtAuthentication implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UsuarioDetailsService usuarioDetailsService;

    public ConversorJwtAuthentication(UsuarioDetailsService usuarioDetailsService) {
        this.usuarioDetailsService = usuarioDetailsService;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UserDetails usuario = usuarioDetailsService.loadUserByUsername(jwt.getSubject());

        if (!usuario.isEnabled()) {
            throw new BadCredentialsException("Usuario desativado.");
        }

        Collection<? extends GrantedAuthority> authorities = usuario.getAuthorities();

        UsernamePasswordAuthenticationToken autenticacao =
                new UsernamePasswordAuthenticationToken(usuario, null, authorities);
        autenticacao.setDetails(jwt);
        return autenticacao;
    }
}
