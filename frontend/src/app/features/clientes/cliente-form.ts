import { NgIf } from '@angular/common';
import { Component, inject, input, OnInit, signal } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Button, ButtonDirective } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Textarea } from 'primeng/textarea';
import { Skeleton } from 'primeng/skeleton';

import { Cliente, ClientePayload } from '../../core/models/cliente.model';
import { ClienteService } from '../../core/services/cliente.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

@Component({
  selector: 'app-cliente-form',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    ButtonDirective,
    InputText,
    Textarea,
    Skeleton,
    NgIf,
  ],
  templateUrl: './cliente-form.html',
  styleUrl: './cliente-form.scss',
})
export class ClienteForm implements OnInit {
  /** Parametro de rota preenchido automaticamente pelo Angular (clientes/:clienteId/editar). */
  readonly clienteId = input<string>();

  private readonly formBuilder = inject(FormBuilder);
  private readonly clienteService = inject(ClienteService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);

  formulario!: FormGroup;
  readonly carregando = signal(true);
  readonly salvando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly editando = signal(false);

  ngOnInit(): void {
    this.formulario = this.formBuilder.group({
      nome: ['', [Validators.required, Validators.minLength(3), Validators.maxLength(150)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(180)]],
      telefone: ['', [Validators.maxLength(30)]],
      empresa: ['', [Validators.maxLength(150)]],
      cargo: ['', [Validators.maxLength(100)]],
      observacoes: ['', [Validators.maxLength(1000)]],
    });

    const id = this.clienteId();
    if (id) {
      this.editando.set(true);
      this.carregarCliente(Number(id));
    } else {
      this.carregando.set(false);
    }
  }

  get titulo(): string {
    return this.editando() ? 'Editar cliente' : 'Novo cliente';
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

    const payload: ClientePayload = this.formulario.getRawValue();
    this.salvando.set(true);

    const id = this.clienteId();
    const requisicao = id
      ? this.clienteService.atualizar(Number(id), payload)
      : this.clienteService.criar(payload);

    requisicao.subscribe({
      next: (cliente: Cliente) => {
        this.salvando.set(false);
        this.messageService.add({
          severity: 'success',
          summary: id ? 'Cliente atualizado' : 'Cliente cadastrado',
          detail: `Os dados de ${cliente.nome} foram salvos com sucesso.`,
        });
        this.router.navigate(['/clientes', cliente.id]);
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

  private carregarCliente(id: number): void {
    this.clienteService.buscarPorId(id).subscribe({
      next: (cliente) => {
        this.formulario.patchValue({
          nome: cliente.nome,
          email: cliente.email,
          telefone: cliente.telefone ?? '',
          empresa: cliente.empresa ?? '',
          cargo: cliente.cargo ?? '',
          observacoes: cliente.observacoes ?? '',
        });
        this.carregando.set(false);
      },
      error: (erro) => {
        this.erro.set(mensagemErro(erro, 'Cliente nao encontrado.'));
        this.carregando.set(false);
      },
    });
  }

  /** Reposiciona os erros devolvidos pela API nos respectivos campos do formulario. */
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
    if (controle.hasError('email')) {
      return 'Informe um e-mail valido.';
    }
    if (controle.hasError('minlength')) {
      return 'Informe ao menos 3 caracteres.';
    }
    return '';
  }
}
