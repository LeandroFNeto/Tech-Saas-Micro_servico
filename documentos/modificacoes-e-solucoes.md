# Modificações e soluções aplicadas

O backend Spring Boot já expõe o contrato OpenAPI (DTOs de empresa, headers `x-admin-token` / `x-cliente-token` e `ModuloEmpresa`). O que faltava era o painel web. Esta pasta registra as decisões do frontend Angular em `frontend/`.

## O que foi criado

Estrutura Angular 19 (standalone + lazy loading + Tailwind + Reactive Forms):

```
frontend/src/app/
  core/           guards, interceptor HTTP, AuthService, EmpresaService
  shared/         models do contrato OpenAPI, layout e banner de avisos
  features/auth   login
  features/admin  listagem e cadastro/edição de empresas + toggles de módulos
  features/client perfil do cliente SaaS
```

Rotas protegidas: `/login` público; `/admin/**` exige JWT simulado com papel `ADMIN`; `/cliente/**` exige papel `CLIENTE`.

## Soluções (lacunas do contrato vs. o que o painel precisa)

| Problema | Solução aplicada |
|---|---|
| Não existe endpoint de login/JWT no Swagger | `AuthService` autentica usuários demo, monta um JWT e guarda em `localStorage`. Erro explícito: **Usuário não encontrado**. |
| Admin no Java valida `x-admin-token` (`ADMIN_API_KEY`), não Bearer | Interceptor envia `x-admin-token` (valor em `environment.ts`) e `x-cliente-token` conforme o papel. |
| POST/PUT devolvem `EmpresaResponseDTO` (sem saudação, módulos, etc.) | Listagem e edição leem o HAL do Spring Data REST (`GET /empresas` e `findBySessaoWhatsapp`). |
| Cliente não pode alterar módulos/cobrança | Perfil usa só `EmpresaUpdateDTO`. Admin usa `EmpresaCreateDTO` / `EmpresaUpdateAdminDTO` com checkboxes de `modulosIniciais` / `modulosAtivos`. |
| CORS no browser → API na porta 8080 | Proxy `/api` → `http://localhost:8080` (`proxy.conf.json`). |
| Área de propaganda não existe na API | `AvisoService` local: banner de upgrade, tutorial e comunicado no painel do cliente. |

## Contratos HTTP usados

- `POST /empresas` → `EmpresaCreateDTO`
- `PUT /empresas/{sessao}/cliente` → `EmpresaUpdateDTO`
- `PUT /empresas/{sessao}/admin` → `EmpresaUpdateAdminDTO`
- Respostas tipadas como `EmpresaResponseDTO`
- Listagem via HAL (`_embedded.empresas`)

## Como rodar

1. Backend na porta 8080 (`ADMIN_API_KEY` igual a `frontend/src/environments/environment.ts`).
2. `cd frontend && npm start`
3. Abrir http://localhost:4200

**Admin:** `admin@techsaas.com` / `admin123`  
**Cliente:** `cliente@techsaas.com` / `cliente123`
