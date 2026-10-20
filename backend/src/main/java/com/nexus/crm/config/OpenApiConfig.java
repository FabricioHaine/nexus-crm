package com.nexus.crm.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadados exibidos na documentacao Swagger (OpenAPI).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI nexusCrmOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Nexus CRM API")
                .version("1.0.0")
                .description("""
                        API REST do Nexus CRM: gestao de clientes, oportunidades de vendas e indicadores
                        do dashboard. Todas as informacoes sao persistidas em banco de dados.
                        """)
                .contact(new Contact().name("Nexus CRM"))
                .license(new License().name("MIT")));
    }
}
