import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { provideRouter } from '@angular/router';
import { providePrimeNG } from 'primeng/config';
import { beforeEach, describe, expect, it } from 'vitest';

import { environment } from '../../../environments/environment';
import { DashboardPage } from './dashboard-page';

const RESUMO = {
  totalClientes: 6,
  totalOportunidades: 10,
  totalOportunidadesAbertas: 6,
  vendasGanhas: 2,
  vendasPerdidas: 2,
  valorTotalAbertas: 476000,
  valorTotalGanho: 163500,
  valorTotalPerdido: 89500,
  taxaConversao: 50,
};

const ETAPAS = [
  { etapa: 'NOVO_CONTATO', etapaDescricao: 'Novo contato', quantidade: 2, valorTotal: 50000 },
  { etapa: 'QUALIFICACAO', etapaDescricao: 'Qualificacao', quantidade: 2, valorTotal: 168000 },
  { etapa: 'PROPOSTA', etapaDescricao: 'Proposta', quantidade: 2, valorTotal: 258000 },
  { etapa: 'GANHO', etapaDescricao: 'Ganho', quantidade: 2, valorTotal: 163500 },
  { etapa: 'PERDIDO', etapaDescricao: 'Perdido', quantidade: 2, valorTotal: 89500 },
];

const PERIODOS = [
  { periodo: '2026-08', quantidade: 1, valorTotal: 18500 },
  { periodo: '2026-09', quantidade: 1, valorTotal: 145000 },
  { periodo: '2026-10', quantidade: 0, valorTotal: 0 },
];

describe('DashboardPage', () => {
  let fixture: ComponentFixture<DashboardPage>;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardPage],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        provideNoopAnimations(),
        providePrimeNG(),
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(DashboardPage);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('deve exibir o estado de carregamento enquanto os indicadores sao buscados', () => {
    fixture.detectChanges();

    expect(fixture.componentInstance.carregando()).toBe(true);

    httpMock.expectOne(`${environment.apiUrl}/dashboard/resumo`).flush(RESUMO);
    httpMock.expectOne(`${environment.apiUrl}/dashboard/oportunidades-por-etapa`).flush(ETAPAS);
    httpMock
      .expectOne(`${environment.apiUrl}/dashboard/vendas-por-periodo?meses=6`)
      .flush(PERIODOS);
  });

  it('deve apresentar os indicadores reais vindos da API', () => {
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiUrl}/dashboard/resumo`).flush(RESUMO);
    httpMock.expectOne(`${environment.apiUrl}/dashboard/oportunidades-por-etapa`).flush(ETAPAS);
    httpMock
      .expectOne(`${environment.apiUrl}/dashboard/vendas-por-periodo?meses=6`)
      .flush(PERIODOS);

    fixture.detectChanges();

    const cartoes = fixture.componentInstance.cartoes();
    expect(cartoes.length).toBe(5);
    expect(cartoes[0].valor).toBe('6');
    expect(cartoes[4].valor).toBe('50.0%');
    expect(fixture.componentInstance.carregando()).toBe(false);

    const texto = fixture.nativeElement.textContent as string;
    expect(texto).toContain('Clientes cadastrados');
    expect(texto).toContain('Taxa de conversao');
  });

  it('deve montar os dados dos graficos a partir da API', () => {
    fixture.detectChanges();

    httpMock.expectOne(`${environment.apiUrl}/dashboard/resumo`).flush(RESUMO);
    httpMock.expectOne(`${environment.apiUrl}/dashboard/oportunidades-por-etapa`).flush(ETAPAS);
    httpMock
      .expectOne(`${environment.apiUrl}/dashboard/vendas-por-periodo?meses=6`)
      .flush(PERIODOS);

    const graficoEtapas = fixture.componentInstance.dadosGraficoEtapas();
    expect(graficoEtapas.labels).toHaveLength(5);
    expect(graficoEtapas.datasets[0].data).toEqual([2, 2, 2, 2, 2]);

    const graficoEvolucao = fixture.componentInstance.dadosGraficoEvolucao();
    expect(graficoEvolucao.datasets[0].data).toEqual([18500, 145000, 0]);
  });

  it('deve tratar erro da API sem quebrar a tela', () => {
    fixture.detectChanges();

    httpMock
      .expectOne(`${environment.apiUrl}/dashboard/resumo`)
      .flush({ mensagem: 'Erro interno' }, { status: 500, statusText: 'Server Error' });
    httpMock.expectOne(`${environment.apiUrl}/dashboard/oportunidades-por-etapa`).flush(ETAPAS);
    httpMock
      .expectOne(`${environment.apiUrl}/dashboard/vendas-por-periodo?meses=6`)
      .flush(PERIODOS);

    expect(fixture.componentInstance.erro()).toContain('Erro interno');
    expect(fixture.componentInstance.carregando()).toBe(false);
  });
});
