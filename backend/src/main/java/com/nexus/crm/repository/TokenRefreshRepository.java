package com.nexus.crm.repository;

import com.nexus.crm.model.TokenRefresh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TokenRefreshRepository extends JpaRepository<TokenRefresh, Long> {

    Optional<TokenRefresh> findByTokenHash(String tokenHash);

    /** Revoga todos os tokens ativos de um usuario (troca de senha ou logout global). */
    @Modifying
    @Query("UPDATE TokenRefresh t SET t.revogado = true WHERE t.usuarioId = :usuarioId AND t.revogado = false")
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId);

    /** Remove os tokens expirados ha mais tempo, evitando crescimento sem limite. */
    @Modifying
    @Query("DELETE FROM TokenRefresh t WHERE t.expiraEm < :limite")
    int removerExpirados(@Param("limite") LocalDateTime limite);
}
