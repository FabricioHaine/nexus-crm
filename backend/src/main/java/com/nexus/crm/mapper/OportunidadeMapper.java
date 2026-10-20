package com.nexus.crm.mapper;

import com.nexus.crm.dto.OportunidadeRequest;
import com.nexus.crm.dto.OportunidadeResponse;
import com.nexus.crm.model.Cliente;
import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.model.Oportunidade;

/**
 * Conversao entre DTOs e entidade de oportunidade.
 */
public final class OportunidadeMapper {

    private OportunidadeMapper() {
    }

    public static Oportunidade toEntity(OportunidadeRequest request, Cliente cliente, Oportunidade destino) {
        Oportunidade oportunidade = destino != null ? destino : new Oportunidade();
        oportunidade.setTitulo(request.titulo().trim());
        oportunidade.setDescricao(textoVazioParaNull(request.descricao()));
        oportunidade.setCliente(cliente);
        oportunidade.setValorEstimado(request.valorEstimado());
        aplicarEtapa(oportunidade, request.etapa());
        oportunidade.setProbabilidade(request.probabilidade() != null
                ? request.probabilidade()
                : request.etapa().getProbabilidadePadrao());
        oportunidade.setDataPrevistaFechamento(request.dataPrevistaFechamento());
        oportunidade.setMotivoPerda(textoVazioParaNull(request.motivoPerda()));
        return oportunidade;
    }

    /**
     * Aplica a etapa e ajusta os campos derivados (probabilidade, data de fechamento e
     * motivo de perda).
     */
    public static void aplicarEtapa(Oportunidade oportunidade, EtapaOportunidade etapa, String motivoPerda) {
        aplicarEtapa(oportunidade, etapa);
        if (etapa == EtapaOportunidade.PERDIDO) {
            oportunidade.setMotivoPerda(motivoPerda == null || motivoPerda.isBlank()
                    ? "Motivo nao informado"
                    : motivoPerda.trim());
        } else if (etapa == EtapaOportunidade.GANHO) {
            oportunidade.setProbabilidade(100);
            oportunidade.setMotivoPerda(null);
        }
    }

    public static OportunidadeResponse toResponse(Oportunidade oportunidade) {
        Cliente cliente = oportunidade.getCliente();
        return new OportunidadeResponse(
                oportunidade.getId(),
                oportunidade.getTitulo(),
                oportunidade.getDescricao(),
                cliente.getId(),
                cliente.getNome(),
                cliente.getEmpresa(),
                oportunidade.getValorEstimado(),
                oportunidade.getEtapa(),
                oportunidade.getEtapa().getDescricao(),
                oportunidade.getProbabilidade(),
                oportunidade.getDataCriacao(),
                oportunidade.getDataPrevistaFechamento(),
                oportunidade.getDataAtualizacao(),
                oportunidade.getDataFechamento(),
                oportunidade.getMotivoPerda()
        );
    }

    private static void aplicarEtapa(Oportunidade oportunidade, EtapaOportunidade etapa) {
        oportunidade.setEtapa(etapa);
        if (etapa == EtapaOportunidade.GANHO) {
            oportunidade.setProbabilidade(100);
        } else if (etapa == EtapaOportunidade.PERDIDO) {
            oportunidade.setProbabilidade(0);
        }
    }

    private static String textoVazioParaNull(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
