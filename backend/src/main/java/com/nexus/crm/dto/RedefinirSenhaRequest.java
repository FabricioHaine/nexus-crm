package com.nexus.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Pedido de redefinicao de senha.
 */
public record RedefinirSenhaRequest(

        @NotBlank(message = "Informe o token de recuperacao.")
        String token,

        @NotBlank(message = "Informe a nova senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "A senha deve conter ao menos uma letra e um numero.")
        String novaSenha
) {
}
