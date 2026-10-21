package com.nexus.crm.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Dados de cadastro de um novo usuario.
 *
 * <p>O perfil definido no cadastro publico e sempre {@code VENDEDOR}.
 * Promover um usuario a gerente ou administrador e uma acao restrita ao ADMIN.</p>
 */
public record RegistroRequest(

        @NotBlank(message = "Informe o nome.")
        @Size(max = 150, message = "O nome deve ter no maximo 150 caracteres.")
        String nome,

        @NotBlank(message = "Informe o e-mail.")
        @Email(message = "Informe um e-mail valido.")
        @Size(max = 180, message = "O e-mail deve ter no maximo 180 caracteres.")
        String email,

        @NotBlank(message = "Informe a senha.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "A senha deve conter ao menos uma letra e um numero.")
        String senha
) {
}
