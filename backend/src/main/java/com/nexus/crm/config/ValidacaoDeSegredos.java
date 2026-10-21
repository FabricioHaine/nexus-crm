package com.nexus.crm.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Impede que a aplicacao suba com credenciais de desenvolvimento fora do
 * ambiente local.
 *
 * <p>Nos perfis {@code dev} e {@code test} os valores padrao sao aceitos, para
 * que o projeto funcione logo apos o clone. Em qualquer outro perfil, a
 * aplicacao <strong>falha na inicializacao</strong> se a chave JWT ou a senha
 * do administrador ainda forem as de desenvolvimento — uma falha cedo e
 * explicita e muito melhor do que um ambiente exposto.</p>
 */
@Component
public class ValidacaoDeSegredos implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ValidacaoDeSegredos.class);

    /** Chave de desenvolvimento que nunca pode ser usada fora de dev/test. */
    private static final String CHAVE_DE_DESENVOLVIMENTO =
            "nexus-crm-chave-de-desenvolvimento-troque-em-producao-2026";

    /** Senha de desenvolvimento que nunca pode ser usada fora de dev/test. */
    private static final String SENHA_DE_DESENVOLVIMENTO = "Nexus@2026";

    private final Environment ambiente;
    private final String chaveJwt;
    private final String senhaAdmin;
    private final String emailAdmin;

    public ValidacaoDeSegredos(Environment ambiente,
                               @Value("${app.jwt.secret}") String chaveJwt,
                               @Value("${app.auth.admin.senha}") String senhaAdmin,
                               @Value("${app.auth.admin.email}") String emailAdmin) {
        this.ambiente = ambiente;
        this.chaveJwt = chaveJwt;
        this.senhaAdmin = senhaAdmin;
        this.emailAdmin = emailAdmin;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (ambiente.acceptsProfiles(Profiles.of("dev", "test"))) {
            log.warn("Perfil de desenvolvimento ativo: chave JWT e senha do administrador padrao estao em uso. "
                    + "Nao use estas credenciais fora do ambiente local.");
            return;
        }

        List<String> problemas = new ArrayList<>();

        if (CHAVE_DE_DESENVOLVIMENTO.equals(chaveJwt)) {
            problemas.add("app.jwt.secret (APP_JWT_SECRET) ainda e a chave de desenvolvimento");
        }
        if (SENHA_DE_DESENVOLVIMENTO.equals(senhaAdmin)) {
            problemas.add("app.auth.admin.senha (APP_ADMIN_SENHA) ainda e a senha de desenvolvimento");
        }
        if (emailAdmin.endsWith(".local")) {
            problemas.add("app.auth.admin.email (APP_ADMIN_EMAIL) ainda e um endereco de desenvolvimento");
        }

        if (!problemas.isEmpty()) {
            throw new IllegalStateException(
                    "Configuracao insegura para o perfil '" + String.join(",", ambiente.getActiveProfiles())
                            + "'. Corrija antes de iniciar: " + String.join("; ", problemas) + ".");
        }

        log.info("Configuracao de seguranca validada para o perfil '{}'.",
                String.join(",", ambiente.getActiveProfiles()));
    }
}
