package com.nexus.crm.model;

/**
 * Perfis de acesso disponiveis no Nexus CRM.
 *
 * <p>As regras de autorizacao ficam centralizadas no {@code SecurityConfig}:</p>
 * <ul>
 *   <li>{@link #ADMIN} - acesso total, incluindo a gestao de usuarios;</li>
 *   <li>{@link #GERENTE} - acesso a todos os dados e as operacoes de exclusao;</li>
 *   <li>{@link #VENDEDOR} - cadastro e edicao, mas sem excluir registros.</li>
 * </ul>
 */
public enum Papel {

    ADMIN("Administrador"),
    GERENTE("Gerente"),
    VENDEDOR("Vendedor");

    private final String descricao;

    Papel(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
