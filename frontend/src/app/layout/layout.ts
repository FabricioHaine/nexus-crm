import { NgFor, NgIf } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { ActivatedRoute, NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ButtonDirective } from 'primeng/button';
import { filter, map, startWith } from 'rxjs';

interface ItemMenu {
  rotulo: string;
  icone: string;
  rota: string;
}

@Component({
  selector: 'app-layout',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, ButtonDirective, NgFor, NgIf],
  templateUrl: './layout.html',
  styleUrl: './layout.scss',
})
export class Layout {
  private readonly router = inject(Router);
  private readonly activatedRoute = inject(ActivatedRoute);

  /** Controla a abertura do menu lateral em telas pequenas. */
  readonly menuAberto = signal(false);

  readonly itensMenu: ItemMenu[] = [
    { rotulo: 'Dashboard', icone: 'pi pi-chart-pie', rota: '/dashboard' },
    { rotulo: 'Clientes', icone: 'pi pi-users', rota: '/clientes' },
    { rotulo: 'Oportunidades', icone: 'pi pi-briefcase', rota: '/oportunidades' },
    { rotulo: 'Funil comercial', icone: 'pi pi-chart-bar', rota: '/funil' },
  ];

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
