import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, catchError, firstValueFrom, map, of, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { LoginPayload, RegistroPayload, Sessao, Usuario } from '../models/usuario.model';

/**
 * Estado da sessao do usuario.
 *
 * <p>A sessao vive em um cookie {@code HttpOnly} gerenciado pela API: o
 * JavaScript da pagina nunca tem acesso ao token, o que fecha a porta para
 * roubo de credencial por XSS. Nenhum segredo e gravado no
 * {@code localStorage}.</p>
 *
 * <p>A leitura do usuario autenticado e feita chamando {@code /api/auth/me};
 * o cache evita repetir a chamada a cada navegacao.</p>
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/auth`;

  private readonly usuarioAtual = signal<Usuario | null>(null);
  private readonly sessaoConsultada = signal(false);
  private consultaEmAndamento: Promise<Usuario | null> | null = null;

  /** Usuario autenticado no momento, ou null. */
  readonly usuario = this.usuarioAtual.asReadonly();

  /** Indica se existe uma sessao ativa. */
  readonly autenticado = computed(() => this.usuarioAtual() !== null);

  /** Indica se a sessao ja foi consultada no servidor. */
  readonly sessaoCarregada = this.sessaoConsultada.asReadonly();

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

  /**
   * Garante que a sessao foi consultada no servidor e devolve o usuario.
   * Chamadas concorrentes compartilham a mesma promessa.
   */
  garantirSessao(): Promise<Usuario | null> {
    if (this.sessaoConsultada()) {
      return Promise.resolve(this.usuarioAtual());
    }
    if (!this.consultaEmAndamento) {
      this.consultaEmAndamento = firstValueFrom(this.http.get<Usuario>(`${this.baseUrl}/me`))
        .then((usuario) => {
          this.usuarioAtual.set(usuario);
          return usuario;
        })
        .catch(() => {
          this.usuarioAtual.set(null);
          return null;
        })
        .finally(() => {
          this.sessaoConsultada.set(true);
          this.consultaEmAndamento = null;
        });
    }
    return this.consultaEmAndamento;
  }

  login(credenciais: LoginPayload): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.baseUrl}/login`, credenciais).pipe(
      tap((sessao) => this.registrarSessao(sessao.usuario)),
    );
  }

  registrar(dados: RegistroPayload): Observable<Sessao> {
    return this.http.post<Sessao>(`${this.baseUrl}/registro`, dados).pipe(
      tap((sessao) => this.registrarSessao(sessao.usuario)),
    );
  }

  /**
   * Tenta renovar a sessao a partir do cookie de renovacao.
   * Devolve o usuario renovado ou null quando a renovacao falha.
   */
  renovar(): Observable<Usuario | null> {
    return this.http.post<Sessao>(`${this.baseUrl}/refresh`, {}).pipe(
      map((sessao) => sessao.usuario),
      tap((usuario) => this.registrarSessao(usuario)),
      catchError(() => {
        this.usuarioAtual.set(null);
        return of(null);
      }),
    );
  }

  /** Encerra a sessao no servidor e limpa o estado local. */
  logout(): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/logout`, {}).pipe(
      tap(() => {
        this.usuarioAtual.set(null);
        this.sessaoConsultada.set(true);
      }),
      catchError(() => {
        this.usuarioAtual.set(null);
        this.sessaoConsultada.set(true);
        return of(void 0);
      }),
    );
  }

  /** Troca a senha do usuario autenticado, invalizando as outras sessoes. */
  trocarSenha(senhaAtual: string, novaSenha: string): Observable<void> {
    return this.http.post<void>(`${this.baseUrl}/trocar-senha`, { senhaAtual, novaSenha });
  }

  private registrarSessao(usuario: Usuario): void {
    this.usuarioAtual.set(usuario);
    this.sessaoConsultada.set(true);
  }
}
