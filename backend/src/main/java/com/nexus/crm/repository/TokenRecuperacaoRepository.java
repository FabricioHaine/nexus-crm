package com.nexus.crm.repository;

import com.nexus.crm.model.TokenRecuperacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRecuperacaoRepository extends JpaRepository<TokenRecuperacao, Long> {

    Optional<TokenRecuperacao> findByTokenHash(String tokenHash);

    /** Invalida os tokens anteriores de um usuario: apenas o ultimo vale. */
    @Modifying
    @Query("UPDATE TokenRecuperacao t SET t.usado = true WHERE t.usuarioId = :usuarioId AND t.usado = false")
    int invalidarAnteriores(@Param("usuarioId") Long usuarioId);

    @Modifying
    @Query("DELETE FROM TokenRecuperacao t WHERE t.expiraEm < :limite")
    int removerExpirados(@Param("limite") LocalDateTime limite);
}
