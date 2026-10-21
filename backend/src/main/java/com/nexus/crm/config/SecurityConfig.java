package com.nexus.crm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexus.crm.exception.ErrorResponse;
import com.nexus.crm.security.ConversorJwtAuthentication;
import com.nexus.crm.security.UsuarioDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Configuracao de seguranca da API.
 *
 * <p>A API e stateless: cada requisicao carrega um token JWT assinado com
 * HMAC-SHA256, enviado no cabecalho {@code Authorization: Bearer &lt;token&gt;}.</p>
 *
 * <p>Regras de autorizacao por perfil:</p>
 * <ul>
 *   <li>{@code /api/auth/login} e {@code /api/auth/registro} - publicos;</li>
 *   <li>Swagger, OpenAPI e console H2 - publicos (apenas em desenvolvimento);</li>
 *   <li>{@code DELETE} de clientes e oportunidades - ADMIN e GERENTE;</li>
 *   <li>{@code /api/usuarios} - apenas ADMIN;</li>
 *   <li>demais endpoints - qualquer usuario autenticado.</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;

    public SecurityConfig(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Bean
    public SecretKey chaveJwt(@Value("${app.jwt.secret}") String segredo) {
        byte[] bytes = segredo.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException(
                    "app.jwt.secret deve ter no minimo 32 caracteres para assinar o token com HMAC-SHA256.");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey chave) {
        return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(chave));
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey chave) {
        return NimbusJwtDecoder.withSecretKey(chave)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   UsuarioDetailsService usuarioDetailsService) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(gerenciador -> gerenciador.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(registro -> registro
                        .requestMatchers("/api/auth/login", "/api/auth/registro").permitAll()
                        .requestMatchers("/h2-console/**", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/clientes/**", "/api/oportunidades/**")
                        .hasAnyRole("ADMIN", "GERENTE")
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .headers(cabecalho -> cabecalho.frameOptions(frame -> frame.sameOrigin()))
                .oauth2ResourceServer(recurso -> recurso
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(
                                new ConversorJwtAuthentication(usuarioDetailsService)))
                        .authenticationEntryPoint(pontoDeEntradaNaoAutenticado()))
                .exceptionHandling(excecao -> excecao
                        .authenticationEntryPoint(pontoDeEntradaNaoAutenticado())
                        .accessDeniedHandler(acessoNegado()));

        return http.build();
    }

    /**
     * Responde em JSON (mesmo formato das demais falhas da API) quando a
     * requisicao nao possui credenciais validas.
     */
    private AuthenticationEntryPoint pontoDeEntradaNaoAutenticado() {
        return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) -> {
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), ErrorResponse.de(
                    HttpStatus.UNAUTHORIZED.value(),
                    "Nao autenticado",
                    "E necessario autenticar-se para acessar este recurso.",
                    request.getRequestURI()));
        };
    }

    /**
     * Responde em JSON quando o usuario autenticado nao tem o perfil exigido.
     */
    private AccessDeniedHandler acessoNegado() {
        return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), ErrorResponse.de(
                    HttpStatus.FORBIDDEN.value(),
                    "Acesso negado",
                    "Seu perfil de acesso nao permite executar esta operacao.",
                    request.getRequestURI()));
        };
    }
}
