/**
 * Perfis de acesso do Nexus CRM, espelhando o enum `Papel` da API.
 */
export type Papel = 'ADMIN' | 'GERENTE' | 'VENDEDOR';

/** Rotulo amigavel de cada perfil. */
export const ROTULO_PAPEL: Record<Papel, string> = {
  ADMIN: 'Administrador',
  GERENTE: 'Gerente',
  VENDEDOR: 'Vendedor',
};

/** Dados publicos de um usuario. */
export interface Usuario {
  id: number;
  nome: string;
  email: string;
  papel: Papel;
  ativo: boolean;
  dataCriacao: string;
}

/** Resposta de login e registro. */
export interface Sessao {
  token: string;
  tipo: string;
  expiraEm: string;
  usuario: Usuario;
}

/** Credenciais de login. */
export interface LoginPayload {
  email: string;
  senha: string;
}

/** Dados de cadastro de um novo usuario. */
export interface RegistroPayload {
  nome: string;
  email: string;
  senha: string;
}
