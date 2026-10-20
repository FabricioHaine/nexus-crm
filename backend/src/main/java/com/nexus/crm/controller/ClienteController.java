package com.nexus.crm.controller;

import com.nexus.crm.dto.ClienteRequest;
import com.nexus.crm.dto.ClienteResponse;
import com.nexus.crm.dto.OportunidadeResponse;
import com.nexus.crm.dto.PaginaResponse;
import com.nexus.crm.service.ClienteService;
import com.nexus.crm.service.OportunidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Endpoints REST do modulo de clientes.
 */
@RestController
@RequestMapping("/api/clientes")
@Tag(name = "Clientes", description = "Cadastro e gestao de clientes do CRM")
public class ClienteController {

    private final ClienteService clienteService;
    private final OportunidadeService oportunidadeService;

    public ClienteController(ClienteService clienteService, OportunidadeService oportunidadeService) {
        this.clienteService = clienteService;
        this.oportunidadeService = oportunidadeService;
    }

    @GetMapping
    @Operation(summary = "Lista clientes com paginacao, ordenacao e busca por nome, e-mail ou empresa")
    public PaginaResponse<ClienteResponse> listar(
            @RequestParam(required = false) String termo,
            @PageableDefault(size = 10, sort = "nome", direction = Sort.Direction.ASC) Pageable pageable) {
        return clienteService.listar(termo, pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um cliente pelo id")
    public ClienteResponse buscarPorId(@PathVariable Long id) {
        return clienteService.buscarPorId(id);
    }

    @GetMapping("/{id}/oportunidades")
    @Operation(summary = "Lista as oportunidades vinculadas a um cliente")
    public List<OportunidadeResponse> listarOportunidades(@PathVariable Long id) {
        return oportunidadeService.listarPorCliente(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra um novo cliente")
    public ClienteResponse criar(@Valid @RequestBody ClienteRequest request) {
        return clienteService.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de um cliente")
    public ClienteResponse atualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui um cliente sem oportunidades vinculadas")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        clienteService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
