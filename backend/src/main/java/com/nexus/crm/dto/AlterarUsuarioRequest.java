package com.nexus.crm.dto;

import com.nexus.crm.model.Papel;

/**
 * Alteracoes administrativas sobre um usuario. Todos os campos sao opcionais:
 * os ausentes permanecem inalterados.
 */
public record AlterarUsuarioRequest(
        Papel papel,
        Boolean ativo
) {
}
