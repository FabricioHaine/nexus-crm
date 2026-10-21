import { NgIf } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MessageService } from 'primeng/api';
import { Button } from 'primeng/button';
import { InputText } from 'primeng/inputtext';
import { Password } from 'primeng/password';

import { AuthService } from '../../core/services/auth.service';
import { mensagemErro } from '../../core/utils/mensagem-erro';

@Component({
  selector: 'app-login-page',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    Button,
    InputText,
    Password,
    NgIf,
  ],
  templateUrl: './login-page.html',
  styleUrls: ['./auth.scss'],
})
export class LoginPage {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly messageService = inject(MessageService);
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);

  formulario!: FormGroup;
  readonly enviando = signal(false);
  readonly erro = signal<string | null>(null);
  readonly mostrarSenha = signal(false);

  constructor() {
    this.formulario = this.formBuilder.group({
      email: ['', [Validators.required, Validators.email]],
      senha: ['', [Validators.required]],
    });
  }

  entrar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.enviando.set(true);
    this.erro.set(null);

    this.authService.login(this.formulario.getRawValue()).subscribe({
      next: (sessao) => {
        this.enviando.set(false);
        this.messageService.add({
          severity: 'success',
          summary: 'Bem-vindo',
          detail: `Acesso liberado para ${sessao.usuario.nome}.`,
        });
        this.router.navigateByUrl(this.destino());
      },
      error: (erro) => {
        this.enviando.set(false);
        this.erro.set(mensagemErro(erro, 'Nao foi possivel entrar com as credenciais informadas.'));
      },
    });
  }

  /** Rota de destino apos o login: a originalmente pedida ou o dashboard. */
  private destino(): string {
    const pedido = this.activatedRoute.snapshot.queryParamMap.get('redirecionar');
    return pedido && pedido.startsWith('/') ? pedido : '/dashboard';
  }

  campoInvalido(nome: string): boolean {
    const controle = this.formulario.get(nome);
    return !!controle && controle.invalid && controle.touched;
  }

  mensagemDoCampo(nome: string): string {
    const controle = this.formulario.get(nome);
    if (!controle) {
      return '';
    }
    if (controle.hasError('required')) {
      return 'Campo obrigatorio.';
    }
    if (controle.hasError('email')) {
      return 'Informe um e-mail valido.';
    }
    return '';
  }
}
