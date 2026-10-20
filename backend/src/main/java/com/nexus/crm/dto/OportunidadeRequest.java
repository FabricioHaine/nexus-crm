package com.nexus.crm.dto;

import com.nexus.crm.model.EtapaOportunidade;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dados de entrada para criacao e atualizacao de oportunidades.
 */
public record OportunidadeRequest(
        @NotBlank(message = "O titulo da oportunidade e obrigatorio")
        @Size(min = 2, max = 180, message = "O titulo deve ter entre 2 e 180 caracteres")
        String titulo,

        @Size(max = 2000, message = "A descricao deve ter no maximo 2000 caracteres")
        String descricao,

        @NotNull(message = "O cliente da oportunidade e obrigatorio")
        @Positive(message = "O cliente da oportunidade e invalido")
        Long clienteId,

        @NotNull(message = "O valor estimado e obrigatorio")
        @DecimalMin(value = "0.01", message = "O valor estimado deve ser maior que zero")
        BigDecimal valorEstimado,

        @NotNull(message = "A etapa da oportunidade e obrigatoria")
        EtapaOportunidade etapa,

        @Min(value = 0, message = "A probabilidade deve ser no minimo 0")
        @Max(value = 100, message = "A probabilidade deve ser no maximo 100")
        Integer probabilidade,

        @FutureOrPresent(message = "A data prevista de fechamento nao pode estar no passado")
        LocalDate dataPrevistaFechamento,

        @Size(max = 500, message = "O motivo da perda deve ter no maximo 500 caracteres")
        String motivoPerda
) {
}
