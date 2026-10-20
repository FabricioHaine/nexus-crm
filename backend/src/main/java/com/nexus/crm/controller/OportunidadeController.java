package com.nexus.crm.controller;

import com.nexus.crm.dto.AlterarEtapaRequest;
import com.nexus.crm.dto.OportunidadeRequest;
import com.nexus.crm.dto.OportunidadeResponse;
import com.nexus.crm.dto.PaginaResponse;
import com.nexus.crm.model.EtapaOportunidade;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Endpoints REST do modulo de oportunidades comerciais.
 */
@RestController
@RequestMapping("/api/oportunidades")
@Tag(name = "Oportunidades", description = "Funil de vendas e negociacoes comerciais")
public class OportunidadeController {

    private final OportunidadeService oportunidadeService;

    public OportunidadeController(OportunidadeService oportunidadeService) {
        this.oportunidadeService = oportunidadeService;
    }

    @GetMapping
    @Operation(summary = "Lista oportunidades com filtros por cliente, etapa e periodo de criacao")
    public PaginaResponse<OportunidadeResponse> listar(
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) EtapaOportunidade etapa,
            @RequestParam(required = false) LocalDate dataInicial,
            @RequestParam(required = false) LocalDate dataFinal,
            @PageableDefault(size = 10, sort = "dataCriacao", direction = Sort.Direction.DESC) Pageable pageable) {
        return oportunidadeService.listar(
                clienteId,
                etapa,
                dataInicial == null ? null : dataInicial.atStartOfDay(),
                dataFinal == null ? null : dataFinal.atTime(23, 59, 59),
                pageable
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta uma oportunidade pelo id")
    public OportunidadeResponse buscarPorId(@PathVariable Long id) {
        return oportunidadeService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Cadastra uma nova oportunidade")
    public OportunidadeResponse criar(@Valid @RequestBody OportunidadeRequest request) {
        return oportunidadeService.criar(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de uma oportunidade")
    public OportunidadeResponse atualizar(@PathVariable Long id,
                                          @Valid @RequestBody OportunidadeRequest request) {
        return oportunidadeService.atualizar(id, request);
    }

    @PatchMapping("/{id}/etapa")
    @Operation(summary = "Altera a etapa do funil de uma oportunidade")
    public OportunidadeResponse alterarEtapa(@PathVariable Long id,
                                             @Valid @RequestBody AlterarEtapaRequest request) {
        return oportunidadeService.alterarEtapa(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Exclui uma oportunidade")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        oportunidadeService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
