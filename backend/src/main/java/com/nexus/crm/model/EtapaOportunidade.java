package com.nexus.crm.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Etapas do ciclo de vida de uma oportunidade comercial.
 *
 * <p>O funil segue uma progressao linear: NOVO_CONTATO -> QUALIFICACAO -> PROPOSTA.
 * A partir da PROPOSTA a negociacao pode ser encerrada como GANHO ou PERDIDO.
 * GANHO e PERDIDO sao etapas terminais.</p>
 */
public enum EtapaOportunidade {

    NOVO_CONTATO("Novo contato", 10),
    QUALIFICACAO("Qualificação", 35),
    PROPOSTA("Proposta", 65),
    GANHO("Ganho", 100),
    PERDIDO("Perdido", 0);

    private final String descricao;
    private final int probabilidadePadrao;

    EtapaOportunidade(String descricao, int probabilidadePadrao) {
        this.descricao = descricao;
        this.probabilidadePadrao = probabilidadePadrao;
    }

    public String getDescricao() {
        return descricao;
    }

    public int getProbabilidadePadrao() {
        return probabilidadePadrao;
    }

    /** Uma oportunidade e considerada "aberta" enquanto nao foi ganha nem perdida. */
    public boolean isAberta() {
        return this != GANHO && this != PERDIDO;
    }

    /**
     * Indica se a mudanca de etapa para {@code destino} respeita o fluxo comercial.
     * Permanecer na mesma etapa sempre e permitido (operacao idempotente).
     */
    public boolean permiteTransicaoPara(EtapaOportunidade destino) {
        if (destino == null || destino == this) {
            return true;
        }
        return switch (this) {
            case NOVO_CONTATO -> destino == QUALIFICACAO || destino == PERDIDO;
            case QUALIFICACAO -> destino == PROPOSTA || destino == PERDIDO;
            case PROPOSTA -> destino == GANHO || destino == PERDIDO;
            case GANHO, PERDIDO -> false;
        };
    }

    /** Etapas que representam o pipeline ainda em negotiation. */
    public static Set<EtapaOportunidade> etapasAbertas() {
        return EnumSet.of(NOVO_CONTATO, QUALIFICACAO, PROPOSTA);
    }
}
