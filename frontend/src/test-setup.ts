/**
 * Configuracao dos testes unitarios (executados com Vitest).
 * O inicializador do TestBed e injetado automaticamente pelo builder do Angular.
 */
import 'zone.js/testing';

/**
 * O jsdom nao implementa <canvas>. Sem este stub, o Chart.js imprime um aviso
 * gigante durante os testes do dashboard. Os graficos sao validados a nivel de
 * dados (labels e series), portanto o contexto 2D nao e necessario.
 */
if (typeof HTMLCanvasElement !== 'undefined') {
  HTMLCanvasElement.prototype.getContext = (() => null) as unknown as HTMLCanvasElement['getContext'];
}
