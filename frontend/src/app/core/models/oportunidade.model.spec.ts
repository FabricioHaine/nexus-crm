import { describe, expect, it } from 'vitest';

import { ETAPAS, etapaInfo, proximasEtapas } from './oportunidade.model';

describe('Regras de etapa das oportunidades', () => {
  it('deve descrever as cinco etapas do funil', () => {
    expect(ETAPAS).toHaveLength(5);
    expect(ETAPAS.map((etapa) => etapa.valor)).toEqual([
      'NOVO_CONTATO',
      'QUALIFICACAO',
      'PROPOSTA',
      'GANHO',
      'PERDIDO',
    ]);
  });

  it('deve permitir o avanco linear do funil', () => {
    expect(proximasEtapas('NOVO_CONTATO')).toEqual(['QUALIFICACAO', 'PERDIDO']);
    expect(proximasEtapas('QUALIFICACAO')).toEqual(['PROPOSTA', 'PERDIDO']);
    expect(proximasEtapas('PROPOSTA')).toEqual(['GANHO', 'PERDIDO']);
  });

  it('deve tratar ganho e perdido como etapas terminais', () => {
    expect(proximasEtapas('GANHO')).toEqual([]);
    expect(proximasEtapas('PERDIDO')).toEqual([]);
  });

  it('deve retornar as cores e a probabilidade padrao de cada etapa', () => {
    expect(etapaInfo('GANHO').severidade).toBe('success');
    expect(etapaInfo('PERDIDO').severidade).toBe('danger');
    expect(etapaInfo('PROPOSTA').probabilidadePadrao).toBe(65);
    expect(etapaInfo('NOVO_CONTATO').aberta).toBe(true);
    expect(etapaInfo('GANHO').aberta).toBe(false);
  });
});
