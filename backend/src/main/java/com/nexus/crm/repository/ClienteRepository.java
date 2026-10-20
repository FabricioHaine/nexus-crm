package com.nexus.crm.repository;

import com.nexus.crm.model.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

    Optional<Cliente> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    /**
     * Busca clientes por nome, e-mail ou empresa. O termo e opcional: sem termo a
     * listagem paginada completa e devolvida.
     */
    @Query("""
            SELECT c FROM Cliente c
            WHERE (:termo IS NULL
                   OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR LOWER(c.email) LIKE LOWER(CONCAT('%', :termo, '%'))
                   OR LOWER(COALESCE(c.empresa, '')) LIKE LOWER(CONCAT('%', :termo, '%')))
            """)
    Page<Cliente> buscar(@Param("termo") String termo, Pageable pageable);
}
