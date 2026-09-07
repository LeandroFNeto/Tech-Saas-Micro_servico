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

**Admin:** `admin@gamb.com` / `admin123`  
**Cliente:** `cliente@gamb.com` / `cliente123` (sessão WhatsApp `RecantoBot`)

## QR Code do WhatsApp (`Auto Close Called`)

O painel já chama `POST /whatsapp/{sessao}/iniciar`. O QR sumia na inicialização por outro motivo: o WPPConnect reutilizava token e pasta `dados-wpp/userDataDir/RecantoBot` de uma sessão antiga, falhava em `Checking is logged...` e disparava `Auto Close Called` **antes de gerar o QR**.

| Problema | Solução aplicada |
|---|---|
| Sessão Chromium/token corrompida → browser fecha sem QR | Se `create()` cair em Auto Close/timeout, o WPPConnect apaga token + `userDataDir` da sessão e tenta de novo uma vez. |
| `waitQrCode: true` prendia o Java até o auto-close | `POST /start-session` agora usa `waitQrCode: false`. O QR chega no webhook/`status-session`; o painel mostra “aguardando leitura”. |
| Flags `--disable-cache` / `--disk-cache-size=0` quebram o WhatsApp Web | `createOptions.browserArgs` ficou só com o necessário para Docker (`no-sandbox`, `disable-dev-shm-usage`, `disable-gpu`). |
| `start.sh` não rodava (ENTRYPOINT era `node dist/server.js`) | O container volta a limpar `SingletonLock` do Chromium na subida. |
| Webhook `status-find` / `onack` ia para o bot como mensagem (`JID: null`) | `WhatsappWebhookDTO.ehEventoConexao()` ignora esses eventos. |
| Polling via `status` via CLOSED no meio da abertura do Chromium | O painel ignora `DISCONNECTED` por 45s depois do clique em Conectar. |

Depois desta mudança: `Ctrl+C` no Docker e `docker-compose up --build`. No painel do cliente RecantoBot, clique de novo em **Conectar WhatsApp** e espere o QR (pode levar alguns segundos na primeira tentativa, se a sessão antiga for apagada).

## WPPConnect 2.10.0 (pasta local, um clone só)

A limpeza da sessão antiga não bastou: o WhatsApp Web da lib `1.37.9` (server `2.8.11`) está velho demais e o Chromium fecha sem QR. A pasta `wppconnect-server/` no **mesmo repositório** foi atualizada para o server **2.10.0**, com a lib **@wppconnect-team/wppconnect 2.3.3** (versão do WhatsApp Web de setembro/2026).

Não é submodule e não precisa de segundo `git clone`. No servidor: um clone deste repo + `docker-compose up --build`.

Customizações que ficaram em cima da 2.10.0 (arquivos nossos, não do GitHub):

- `src/config.ts` — `SECRET_KEY` do ambiente, `autoClose: 0`, Chromium só com flags de Docker
- `src/util/createSessionUtil.ts` — devolve QR se a sessão já existe; se Auto Close, limpa e tenta de novo
- `start.sh` + `Dockerfile` — limpa `SingletonLock` e sobe `node dist/server.js`

Reconstruir: `Ctrl+C` e `docker-compose up --build`. Depois **Conectar WhatsApp** no painel.
