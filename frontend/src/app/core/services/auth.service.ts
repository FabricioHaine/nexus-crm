import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { LoginPayload, RegistroPayload, Sessao, Usuario } from '../models/usuario.model';

const CHAVE_TOKEN = 'nexus-crm.token';
const CHAVE_USUARIO = 'nexus-crm.usuario';

/**
 * Estado da sessao do usuario: login, registro, logout e leitura do token.
 *
 * <p>O token e mantido no `localStorage` para que a sessao sobreviva ao
 * recarregamento da pagina. O usuario logado e exposto como signal, para que
 * a interface reaja as mudancas de perfil de acesso.</p>
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly usuarioAtual = signal<Usuario | null>(lerUsuarioArmazenado());
  private readonly tokenAtual = signal<string | null>(lerTokenArmazenado());

  /** Usuario autenticado, ou null quando a sessao nao existe. */
  readonly usuario = this.usuarioAtual.asReadonly();

  /** Token de acesso corrente, ou null quando a sessao nao existe. */
  readonly token = this.tokenAtual.asReadonly();

  /** Indica existe uma sessao ativa. */
  readonly autenticado = computed(() => this.tokenAtual() !== null && this.usuarioAtual() !== null);

  /** Perfis do usuario autenticado. */
  readonly papeis = computed(() => {
    const papel = this.usuarioAtual()?.papel;
    return papel ? [papel] : [];
  });

  /** Indica se o usuario autenticado possui um dos perfis informados. */
  temPapel(...papeis: Usuario['papel'][]): boolean {
    const atual = this.usuarioAtual()?.papel;
    return atual !== undefined && papeis.includes(atual);
  }

  /** Indica se o usuario autenticado pode excluir registros. */
  readonly podeExcluir = computed(() => this.temPapel('ADMIN', 'GERENTE'));

  login(credenciais: LoginPayload): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.baseUrl}/login`, credenciais).pipe(tap((sessao) => this.gravar(sessao)));
  }

  registrar(dados: RegistroPayload): Observable<Sessao> {
    return this.http
      .post<Sessao>(`${this.baseUrl}/registro`, dados)
      .pipe(tap((sessao) => this.gravar(sessao)));
  }

  /** Revalida a sessao no servidor, confirmando que o token continua valido. */
  sessaoAtual(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.baseUrl}/me`).pipe(
      tap((usuario) => {
        this.usuarioAtual.set(usuario);
        gravarUsuario(usuario);
      }),
    );
  }

  logout(): void {
    this.tokenAtual.set(null);
    this.usuarioAtual.set(null);
    localStorage.removeItem(CHAVE_TOKEN);
    localStorage.removeItem(CHAVE_USUARIO);
  }

  private gravar(sessao: Sessao): void {
    this.tokenAtual.set(sessao.token);
    this.usuarioAtual.set(sessao.usuario);
    localStorage.setItem(CHAVE_TOKEN, sessao.token);
    gravarUsuario(sessao.usuario);
  }
}

function lerTokenArmazenado(): string | null {
  return localStorage.getItem(CHAVE_TOKEN);
}

function lerUsuarioArmazenado(): Usuario | null {
  const bruto = localStorage.getItem(CHAVE_USUARIO);
  if (!bruto) {
    return null;
  }
  try {
    return JSON.parse(bruto) as Usuario;
  } catch {
    localStorage.removeItem(CHAVE_USUARIO);
    return null;
  }
}

function gravarUsuario(usuario: Usuario): void {
  localStorage.setItem(CHAVE_USUARIO, JSON.stringify(usuario));
}
