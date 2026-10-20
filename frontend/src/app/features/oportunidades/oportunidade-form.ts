import { NgIf } from '@angular/common';
import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { DatePicker } from 'primeng/datepicker';
import { InputNumber } from 'primeng/inputnumber';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Select } from 'primeng/select';
import { Skeleton } from 'primeng/skeleton';

import { Cliente, Pagina } from '../../core/models/cliente.model';
import {
  ETAPAS,
  EtapaOportunidade,
  Oportunidade,
  OportunidadePayload,
  etapaInfo,
  proximasEtapas,
} from '../../core/models/oportunidade.model';
import { ClienteService } from '../../core/services/cliente.service';
import { OportunidadeService } from '../../core/services/oportunidade.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

@Component({
  selector: 'app-oportunidade-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    ButtonDirective,
    InputText,
    Textarea,
    InputNumber,
    Select,
    DatePicker,
    Skeleton,
    NgIf,
  ],
  templateUrl: './oportunidade-form.html',
  styleUrl: './oportunidade-form.scss',
})
export class OportunidadeForm implements OnInit {
  /** Parametro de rota (oportunidades/:oportunidadeId/editar). */
  readonly oportunidadeId = input<string>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly oportunidadeService = inject(OportunidadeService);
  private readonly clienteService = inject(ClienteService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);

  formulario!: FormGroup;
  readonly clientes = signal<Cliente[]>([]);
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly editando = signal(false);
  readonly etapaAtual = signal<EtapaOportunidade | null>(null);

  /** Impede a escolha de uma data de fechamento no passado. */
  readonly dataMinima = new Date();

  readonly opcoesEtapa = computed(() => {
    const atual = this.etapaAtual();
    if (!atual) {
      return ETAPAS.map((etapa) => ({ label: etapa.rotulo, value: etapa.valor }));
    }
    const permitidas = [atual, ...proximasEtapas(atual)];
    return ETAPAS.filter((etapa) => permitidas.includes(etapa.valor)).map((etapa) => ({
      label: etapa.rotulo,
      value: etapa.valor,
    }));
  });

  readonly opcoesCliente = computed(() =>
    this.clientes().map((cliente) => ({
      label: cliente.empresa ? `${cliente.nome} - ${cliente.empresa}` : cliente.nome,
      value: cliente.id,
    })),
  );

  /** O campo de motivo da perda so aparece quando a etapa selecionada e PERDIDO. */
  get mostraMotivoPerda(): boolean {
    return this.formulario?.get('etapa')?.value === 'PERDIDO';
  }

  ngOnInit(): void {
    this.formulario = this.formBuilder.group({
      titulo: ['', [Validators.required, Validators.minLength(2), Validators.maxLength(180)]],
      descricao: ['', [Validators.maxLength(2000)]],
      clienteId: [null, [Validators.required]],
      valorEstimado: [null, [Validators.required, Validators.min(0.01)]],
      etapa: ['NOVO_CONTATO' as EtapaOportunidade, [Validators.required]],
      probabilidade: [10, [Validators.min(0), Validators.max(100)]],
      dataPrevistaFechamento: [null as Date | null],
      motivoPerda: ['', [Validators.maxLength(500)]],
    });

    this.carregarClientes();

    const id = this.oportunidadeId();
    if (id) {
      this.editando.set(true);
      this.carregarOportunidade(Number(id));
    } else {
      this.carregando.set(false);
    }
  }

  get titulo(): string {
    return this.editando() ? 'Editar oportunidade' : 'Nova oportunidade';
  }

  salvar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      this.messageService.add({
        severity: 'warn',
        summary: 'Dados invalidos',
        detail: 'Revise os campos destacados antes de salvar.',
      });
      return;
    }

    const bruto = this.formulario.getRawValue();
    const dataPrevista = bruto.dataPrevistaFechamento as Date | null;
    const etapa = bruto.etapa as EtapaOportunidade;

    const payload: OportunidadePayload = {
      titulo: bruto.titulo,
      descricao: bruto.descricao || null,
      clienteId: Number(bruto.clienteId),
      valorEstimado: Number(bruto.valorEstimado),
      etapa,
      probabilidade: bruto.probabilidade ?? etapaInfo(etapa).probabilidadePadrao,
      dataPrevistaFechamento: dataPrevista ? this.formatarData(dataPrevista) : null,
      motivoPerda: etapa === 'PERDIDO' ? bruto.motivoPerda || null : null,
    };

    this.salvando.set(true);
    const id = this.oportunidadeId();
    const requisicao = id
      ? this.oportunidadeService.atualizar(Number(id), payload)
      : this.oportunidadeService.criar(payload);

    requisicao.subscribe({
      next: (oportunidade: Oportunidade) => {
        this.salvando.set(false);
        this.messageService.add({
          severity: 'success',
          summary: id ? 'Oportunidade atualizada' : 'Oportunidade cadastrada',
          detail: `"${oportunidade.titulo}" foi salva com sucesso.`,
        });
        this.router.navigate(['/oportunidades']);
      },
      error: (erro) => {
        this.salvando.set(false);
        this.aplicarErrosDoFormulario(erro);
        this.messageService.add({
          severity: 'error',
          summary: 'Nao foi possivel salvar',
          detail: mensagemErro(erro),
        });
      },
    });
  }

  aoMudarEtapa(): void {
    const etapa = this.formulario.get('etapa')?.value as EtapaOportunidade;
    const padrao = etapaInfo(etapa).probabilidadePadrao;
    this.formulario.get('probabilidade')?.setValue(padrao);
  }

  campoInvalido(nome: string): boolean {
    const controle = this.formulario.get(nome);
    return !!controle && controle.invalid && (controle.touched || !!controle.errors?.['api']);
  }

  mensagemDoCampo(nome: string): string {
    const controle = this.formulario.get(nome);
    if (!controle) {
      return '';
    }
    if (controle.errors?.['api']) {
      return controle.errors['api'] as string;
    }
    if (controle.hasError('required')) {
      return 'Campo obrigatorio.';
    }
    if (controle.hasError('min')) {
      return 'Informe um valor maior que zero.';
    }
    return '';
  }

  private carregarOportunidade(id: number): void {
    this.oportunidadeService.buscarPorId(id).subscribe({
      next: (oportunidade) => {
        this.etapaAtual.set(oportunidade.etapa);
        this.formulario.patchValue({
          titulo: oportunidade.titulo,
          descricao: oportunidade.descricao ?? '',
          clienteId: oportunidade.clienteId,
          valorEstimado: oportunidade.valorEstimado,
          etapa: oportunidade.etapa,
          probabilidade: oportunidade.probabilidade,
          dataPrevistaFechamento: oportunidade.dataPrevistaFechamento
            ? this.paraData(oportunidade.dataPrevistaFechamento)
            : null,
          motivoPerda: oportunidade.motivoPerda ?? '',
        });
        this.carregando.set(false);
      },
      error: (erro) => {
        this.erro.set(mensagemErro(erro, 'Oportunidade nao encontrada.'));
        this.carregando.set(false);
      },
    });
  }

  private carregarClientes(): void {
    this.clienteService.listar(null, 0, 200, 'nome,asc').subscribe({
      next: (pagina: Pagina<Cliente>) => this.clientes.set(pagina.conteudo),
      error: () => this.clientes.set([]),
    });
  }

  private aplicarErrosDoFormulario(erro: unknown): void {
    const campos = (erro as { error?: { campos?: Record<string, string> } })?.error?.campos;
    if (!campos) {
      return;
    }
    Object.entries(campos).forEach(([campo, mensagem]) => {
      const controle = this.formulario.get(campo);
      if (controle) {
        controle.setErrors({ ...controle.errors, api: mensagem });
        controle.markAsTouched();
      }
    });
  }

  private formatarData(data: Date): string {
    const mes = `${data.getMonth() + 1}`.padStart(2, '0');
    const dia = `${data.getDate()}`.padStart(2, '0');
    return `${data.getFullYear()}-${mes}-${dia}`;
  }

  private paraData(valor: string): Date {
    const [ano, mes, dia] = valor.split('-').map(Number);
    return new Date(ano, mes - 1, dia);
  }
}
