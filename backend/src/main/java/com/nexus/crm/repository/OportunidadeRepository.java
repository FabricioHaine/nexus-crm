package com.nexus.crm.repository;

import com.nexus.crm.model.EtapaOportunidade;
import com.nexus.crm.model.Oportunidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface OportunidadeRepository extends JpaRepository<Oportunidade, Long> {

    boolean existsByClienteId(Long clienteId);

    long countByEtapa(EtapaOportunidade etapa);

    long countByEtapaIn(Collection<EtapaOportunidade> etapas);

    long countByClienteId(Long clienteId);

    @Query("""
            SELECT o FROM Oportunidade o
            JOIN FETCH o.cliente c
            WHERE (:clienteId IS NULL OR c.id = :clienteId)
              AND (:etapa IS NULL OR o.etapa = :etapa)
              AND (:dataInicial IS NULL OR o.dataCriacao >= :dataInicial)
              AND (:dataFinal IS NULL OR o.dataCriacao <= :dataFinal)
            """)
    Page<Oportunidade> buscarComFiltros(@Param("clienteId") Long clienteId,
                                        @Param("etapa") EtapaOportunidade etapa,
                                        @Param("dataInicial") LocalDateTime dataInicial,
                                        @Param("dataFinal") LocalDateTime dataFinal,
                                        Pageable pageable);

    @Query("""
            SELECT o FROM Oportunidade o
            JOIN FETCH o.cliente c
            WHERE c.id = :clienteId
            ORDER BY o.dataCriacao DESC
            """)
    List<Oportunidade> listarPorCliente(@Param("clienteId") Long clienteId);

    @Query("""
            SELECT o FROM Oportunidade o
            JOIN FETCH o.cliente
            WHERE o.etapa = :etapa
              AND o.dataFechamento IS NOT NULL
              AND o.dataFechamento >= :inicio
              AND o.dataFechamento < :fim
            ORDER BY o.dataFechamento ASC
            """)
    List<Oportunidade> listarFechadasNoPeriodo(@Param("etapa") EtapaOportunidade etapa,
                                                @Param("inicio") LocalDateTime inicio,
                                                @Param("fim") LocalDateTime fim);

    @Query("""
            SELECT COALESCE(SUM(o.valorEstimado), 0) FROM Oportunidade o WHERE o.etapa IN :etapas
            """)
    java.math.BigDecimal somarValorPorEtapas(@Param("etapas") Collection<EtapaOportunidade> etapas);

    @Query("""
            SELECT o.etapa AS etapa, COUNT(o) AS total, COALESCE(SUM(o.valorEstimado), 0) AS valor
            FROM Oportunidade o
            GROUP BY o.etapa
            """)
    List<ProjecaoEtapa> agruparPorEtapa();

    interface ProjecaoEtapa {
        EtapaOportunidade getEtapa();

        Long getTotal();

        java.math.BigDecimal getValor();
    }
}
