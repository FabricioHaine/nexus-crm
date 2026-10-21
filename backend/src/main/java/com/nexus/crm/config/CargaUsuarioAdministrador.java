package com.nexus.crm.config;

import com.nexus.crm.model.Papel;
import com.nexus.crm.model.Usuario;
import com.nexus.crm.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Cria o usuario administrador inicial, caso ainda nao exista.
 *
 * <p>Executa em todas as subidas da aplicacao e e idempotente: se o e-mail
 * configurado ja existir, nada e feito. As credenciais podem ser alteradas
 * pelas propriedades {@code app.auth.admin.*} ou pelas variaveis de ambiente
 * {@code APP_ADMIN_EMAIL}, {@code APP_ADMIN_SENHA} e {@code APP_ADMIN_NOME}.</p>
 */
@Component
public class CargaUsuarioAdministrador implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CargaUsuarioAdministrador.class);

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;
    private final String nome;

    public CargaUsuarioAdministrador(UsuarioRepository usuarioRepository,
                                     PasswordEncoder passwordEncoder,
                                     @Value("${app.auth.admin.email}") String email,
                                     @Value("${app.auth.admin.senha}") String senha,
                                     @Value("${app.auth.admin.nome}") String nome) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email.trim().toLowerCase();
        this.senha = senha;
        this.nome = nome.trim();
    }

    @Override
    public void run(String... args) {
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            log.info("Usuario administrador ja existe: {}", email);
            return;
        }

        Usuario admin = new Usuario();
        admin.setNome(nome);
        admin.setEmail(email);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setPapel(Papel.ADMIN);
        admin.setAtivo(true);
        usuarioRepository.save(admin);

        log.warn("Usuario administrador criado: {} (troque a senha e defina APP_ADMIN_SENHA em producao).", email);
    }
}
