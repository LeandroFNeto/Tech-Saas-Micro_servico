# Fase 1: inventário (26/10/2026)

Só leitura. Nenhum arquivo do produto foi alterado.

## 1. Rotas

| Caminho | Componente | Guard |
|---|---|---|
| `''` | redirect para `login` | nenhum |
| `login` | LoginComponent | nenhum |
| `admin` | ShellComponent, filhos abaixo | authGuard + roleGuard('ADMIN') |
| `admin` (filho vazio) | DashboardComponent | herda |
| `admin/empresas/nova` | EmpresaFormComponent | herda |
| `admin/empresas/:sessao` | EmpresaFormComponent | herda |
| `admin/avisos` | GestaoAvisosComponent | herda |
| `cliente` | ShellComponent, filho vazio: PerfilComponent | authGuard + roleGuard('CLIENTE') |
| `design-v0` | V0DashboardComponent | nenhum (fora da change) |
| `**` | redirect para `login` | nenhum |

## 2. Navegação

| Origem | Tipo | Destino | Rota existe |
|---|---|---|---|
| shell.component.html | routerLink | /admin, /admin/empresas/nova, /admin/avisos, /cliente | sim |
| dashboard.component.html | routerLink | /admin/empresas/nova; /admin/empresas/:sessaoWhatsapp | sim |
| empresa-form.component.html:10 | routerLink | /admin | sim |
| empresa-form.component.ts:137 | navigateByUrl | /admin | sim |
| login.component.ts:30, 49 | navigateByUrl | rotaInicial(): /admin ou /cliente | sim |
| auth.service.ts:59 | navigateByUrl | /login | sim |

Nenhum destino sem rota. Nenhum `href` interno nos templates Angular.

## 3. Componentes

| Componente | Template | Usado em |
|---|---|---|
| ShellComponent | shared/components/shell/ | admin e cliente |
| LoginComponent | features/auth/login/ | rota login |
| DashboardComponent | features/admin/dashboard/ | rota admin |
| EmpresaFormComponent | features/admin/empresa-form/ | rotas nova e :sessao |
| GestaoAvisosComponent | features/admin/gestao-avisos/ | rota admin/avisos |
| PerfilComponent | features/client/perfil/ | rota cliente |
| BannerAvisosComponent | shared/components/banner-avisos/ | perfil (2 vezes) |
| ConexaoWhatsappComponent | shared/components/conexao-whatsapp/ | empresa-form e perfil |
| V0DashboardComponent | features/design-v0/ | só rota design-v0, sem link |

Não existe `features/apresentacao/`. Ele será criado.

## 4. Telas da referência

| Canvas | Componente que recebe | .spec.ts |
|---|---|---|
| Apresentacao | nenhum (criar) | não |
| Main | LoginComponent | não |
| AdminEmpresas | DashboardComponent | não |
| AdminClienteForm | EmpresaFormComponent e ConexaoWhatsapp | não |
| AdminAvisos | GestaoAvisosComponent | não |
| ClientePerfil | PerfilComponent, BannerAvisos, ConexaoWhatsapp | não |
| menu dos painéis | ShellComponent | não |

Único spec do front: `app.component.spec.ts`.

## 5. Texto

- Usa `*.content.json`: só o perfil (`cliente-perfil.content.json`, 50 acessos a `conteudo.`) e o v0 (`v0-dashboard.content.json`).
- Texto fixo no HTML: login, dashboard, empresa-form, gestao-avisos, shell, banner-avisos e conexao-whatsapp.
- Textos a trocar: `login.component.ts:54` ("Senha incorreta"); `empresa-form.component.html:95` (Fazer Upload da Foto Principal), `:139` (Adicionar Fotos à Galeria), `:213` (Salvar infraestrutura).
- Dica "Use ao menos 6 caracteres" não existe no perfil: a fazer.
- Não existem `apresentacao.content.json` nem `login.content.json`: a criar.

## 6. logo-gamb.jpg

- `frontend/public/logo-gamb.jpg` (arquivo; o `.png` ainda não existe em `public/`).
- `frontend/src/index.html:8` (ícone, `type="image/jpeg"`).
- `frontend/src/app/shared/components/shell/shell.component.html:5`.
- `frontend/src/app/features/auth/login/login.component.html:8`.
- `docs/gerar-readme-hero.py:15` (script do README; decidir se troca).
- `frontend/dist/` tem cópias antigas: build gerado, não editar.

## 7. Versões

- Angular: `@angular/core` `^19.2.0` no package.json; instalado 19.2.25.
- Tailwind: `^3.4.19`; instalado 3.4.19.
- `tailwind.config.js` lê `../especificacao/front/telas.design.json` (fontes, `brand`, `accent`) e faz content de `../especificacao/front/**/*.json`.
- `telas.design.json`: `accent` vai só até 700. Falta `accent-800` (#9a4508).
- `styles.css`: existem `.btn-primary`, `.btn-secondary`, `.input-field`, `.card`. Faltam `.switch`, `.chip`, `.wordmark`. `.input-field` ainda sem `min-h-12` e `text-base`.

## Lacunas e riscos

- Tela de criar cliente (`admin/empresas/nova`) não tem desenho no canvas.
- `ClientePerfil.dc.html:181` tem `href="#"`: destino a definir.
- Rota `design-v0` fica aberta e sem guard; a change não fala dela.
- Nenhum componente de tela tem spec.

Estado: aprovado
