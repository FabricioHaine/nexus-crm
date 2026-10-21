package com.nexus.crm.dto;

import com.nexus.crm.model.Papel;

import java.time.LocalDateTime;

/**
 * Dados publicos de um usuario. Nunca expoe o hash da senha.
 */
public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        Papel papel,
        boolean ativo,
        LocalDateTime dataCriacao
) {
}
