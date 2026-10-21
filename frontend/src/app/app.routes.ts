import { Routes } from '@angular/router';

import { authGuard, guestGuard } from './core/guards/auth.guard';
import { ClienteDetalhes } from './features/clientes/cliente-detalhes';
import { ClienteForm } from './features/clientes/cliente-form';
import { ClienteLista } from './features/clientes/cliente-lista';
import { ContaPage } from './features/conta/conta-page';
import { DashboardPage } from './features/dashboard/dashboard-page';
import { ErroPage } from './features/erro/erro-page';
import { FunilPage } from './features/funil/funil-page';
import { LoginPage } from './features/auth/login-page';
import { OportunidadeForm } from './features/oportunidades/oportunidade-form';
import { OportunidadeLista } from './features/oportunidades/oportunidade-lista';
import { RegistroPage } from './features/auth/registro-page';
import { UsuariosPage } from './features/usuarios/usuarios-page';
import { Layout } from './layout/layout';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPage,
    canActivate: [guestGuard],
    title: 'Entrar | Nexus CRM',
  },
  {
    path: 'registro',
    component: RegistroPage,
    canActivate: [guestGuard],
    title: 'Criar conta | Nexus CRM',
  },
  {
    path: '',
    component: Layout,
    canActivate: [authGuard],
    children: [
      { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
      {
        path: 'dashboard',
        component: DashboardPage,
        title: 'Dashboard | Nexus CRM',
        data: { titulo: 'Dashboard', subtitulo: 'Visao geral da performance comercial' },
      },
      {
        path: 'clientes',
        component: ClienteLista,
        title: 'Clientes | Nexus CRM',
        data: { titulo: 'Clientes', subtitulo: 'Base de clientes cadastrada' },
      },
      {
        path: 'clientes/novo',
        component: ClienteForm,
        title: 'Novo cliente | Nexus CRM',
        data: { titulo: 'Novo cliente', subtitulo: 'Cadastre um novo cliente' },
      },
      {
        path: 'clientes/:clienteId/editar',
        component: ClienteForm,
        title: 'Editar cliente | Nexus CRM',
        data: { titulo: 'Editar cliente', subtitulo: 'Atualize os dados cadastrados' },
      },
      {
        path: 'clientes/:clienteId',
        component: ClienteDetalhes,
        title: 'Cliente | Nexus CRM',
        data: { titulo: 'Detalhes do cliente', subtitulo: 'Historico e oportunidades do cliente' },
      },
      {
        path: 'oportunidades',
        component: OportunidadeLista,
        title: 'Oportunidades | Nexus CRM',
        data: { titulo: 'Oportunidades', subtitulo: 'Funil de negociacoes comerciais' },
      },
      {
        path: 'oportunidades/novo',
        component: OportunidadeForm,
        title: 'Nova oportunidade | Nexus CRM',
        data: { titulo: 'Nova oportunidade', subtitulo: 'Registre uma nova negociacao' },
      },
      {
        path: 'oportunidades/:oportunidadeId/editar',
        component: OportunidadeForm,
        title: 'Editar oportunidade | Nexus CRM',
        data: { titulo: 'Editar oportunidade', subtitulo: 'Atualize os dados da negociacao' },
      },
      {
        path: 'funil',
        component: FunilPage,
        title: 'Funil comercial | Nexus CRM',
        data: { titulo: 'Funil comercial', subtitulo: 'Distribuicao das negociacoes por etapa' },
      },
      {
        path: 'conta',
        component: ContaPage,
        title: 'Minha conta | Nexus CRM',
        data: { titulo: 'Minha conta', subtitulo: 'Dados de acesso e seguranca' },
      },
      {
        path: 'usuarios',
        component: UsuariosPage,
        canActivate: [authGuard],
        title: 'Usuarios | Nexus CRM',
        data: {
          titulo: 'Usuarios',
          subtitulo: 'Gestao de usuarios e perfis de acesso',
          papeis: ['ADMIN'],
        },
      },
      {
        path: 'erro',
        component: ErroPage,
        title: 'Erro | Nexus CRM',
        data: { titulo: 'Pagina nao encontrada', subtitulo: 'A rota acessada nao existe' },
      },
    ],
  },
  { path: '**', component: ErroPage, title: 'Erro | Nexus CRM' },
];
