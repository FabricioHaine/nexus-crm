package com.nexus.crm.dto;

import java.time.Instant;

/**
 * Resposta do login e do registro: o token de acesso e os dados do usuario.
 */
public record LoginResponse(
        String token,
        String tipo,
        Instant expiraEm,
        UsuarioResponse usuario
) {
}
