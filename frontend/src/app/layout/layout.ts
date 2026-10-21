import { NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonDirective } from 'primeng/button';
import { Tooltip } from 'primeng/tooltip';
import { filter, map, startWith } from 'rxjs';

import { AuthService } from '../core/services/auth.service';
import { ROTULO_PAPEL } from '../core/models/usuario.model';

interface ItemMenu {
  rotulo: string;
  icone: string;
  rota: string;
  apenasAdmin?: boolean;
}

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonDirective, Tooltip, NgFor, NgIf],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);
  private readonly authService = inject(AuthService);

  /** Controla a abertura do menu lateral em telas pequenas. */
  readonly menuAberto = signal(false);

  /** Usuario autenticado no momento. */
  readonly usuario = this.authService.usuario;

  /** Rotulo do perfil de acesso do usuario autenticado. */
  readonly perfilAtual = computed(() => {
    const papel = this.usuario()?.papel;
    return papel ? (ROTULO_PAPEL[papel] ?? papel) : '';
  });

  /** Iniciais exibidas no avatar do topo. */
  readonly iniciais = computed(() => {
    const nome = this.usuario()?.nome?.trim() ?? '';
    if (!nome) {
      return '';
    }
    const partes = nome.split(/\s+/);
    const primeira = partes[0]?.charAt(0) ?? '';
    const segunda = partes.length > 1 ? (partes[partes.length - 1]?.charAt(0) ?? '') : '';
    return (primeira + segunda).toUpperCase();
  });

  private readonly todosItensMenu: ItemMenu[] = [
    { rotulo: 'Dashboard', icone: 'pi pi-chart-pie', rota: '/dashboard' },
    { rotulo: 'Clientes', icone: 'pi pi-users', rota: '/clientes' },
    { rotulo: 'Oportunidades', icone: 'pi pi-briefcase', rota: '/oportunidades' },
    { rotulo: 'Funil comercial', icone: 'pi pi-chart-bar', rota: '/funil' },
    { rotulo: 'Usuarios', icone: 'pi pi-id-card', rota: '/usuarios', apenasAdmin: true },
  ];

  /** Itens visiveis para o perfil do usuario autenticado. */
  readonly itensMenu = computed(() => {
    const ehAdmin = this.authService.temPapel('ADMIN');
    return this.todosItensMenu.filter((item) => !item.apenasAdmin || ehAdmin);
  });

  private readonly dadosRota = toSignal(
    this.router.events.pipe(
      filter((evento): evento is NavigationEnd => evento instanceof NavigationEnd),
      startWith(null),
      map(() => this.dadosDaRotaAtual()),
    ),
    { initialValue: { titulo: 'Dashboard', subtitulo: '' } },
  );

  readonly titulo = computed(() => this.dadosRota().titulo);
  readonly subtitulo = computed(() => this.dadosRota().subtitulo);

  alternarMenu(): void {
    this.menuAberto.update((aberto) => !aberto);
  }

  fecharMenu(): void {
    this.menuAberto.set(false);
  }

  /** Encerra a sessao e devolve o usuario para a tela de login. */
  sair(): void {
    this.authService.logout();
    this.fecharMenu();
    this.router.navigate(['/login']);
  }

  private dadosDaRotaAtual(): { titulo: string; subtitulo: string } {
    let rota = this.activatedRoute.snapshot;
    while (rota?.firstChild) {
      rota = rota.firstChild;
    }
    const dados = rota?.data ?? {};
    return {
      titulo: (dados['titulo'] as string) ?? 'Nexus CRM',
      subtitulo: (dados['subtitulo'] as string) ?? '',
    };
  }
}
