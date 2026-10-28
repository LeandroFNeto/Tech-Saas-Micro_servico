# Inventário: corrigir-reserva-automatica

Só leitura, nada corrigido. Pesquisa pelo Graphify; leitura direta só nos arquivos que ele indicou.

## Rotas e guards

| Rota | Componente | Guard | Arquivo |
|---|---|---|---|
| `/cliente` | PainelComponent (dentro de ShellComponent) | authGuard + roleGuard('CLIENTE') | client.routes.ts, app.routes.ts |
| `/cliente/chatbot` | PerfilComponent (`features/client/perfil`) | idem | client.routes.ts |
| `/admin`, `/admin/empresas/nova`, `/admin/empresas/:sessao` | DashboardComponent, EmpresaFormComponent | authGuard + roleGuard('ADMIN') | admin.routes.ts |
| `/admin/avisos` | GestaoAvisosComponent | idem | admin.routes.ts |

Não existe rota `/cliente/perfil`. A tarefa cita `client/perfil` como pasta; a rota real é `/cliente/chatbot`.

## Links e navegação

- `routerLink`: shell.component.html (`/cliente`, `/cliente/chatbot`, `/admin`, `/admin/empresas/nova`, `/admin/avisos`); painel.component.html:18 (`/cliente/chatbot`); dashboard.component.html:6 e :68 (`/admin/empresas/nova`, `/admin/empresas/:sessao`); empresa-form.component.html:10 (`/admin`, voltar); perfil.component.html:2 (`/cliente`, voltar).
- `navigate(`: nenhum no código do app. auth.service.ts:40 só devolve a string `/admin` ou `/cliente`.

## Telas (a) e (b)

- (a) PerfilComponent: `frontend/src/app/features/client/perfil/perfil.component.{ts,html}`. Usado só em `/cliente/chatbot`. Aba `robo` (html:226-252): switch `permiteReservaAutomatica` editável (html:243-252). Form default false (ts:58), preenchido da empresa (ts:290). `salvar()` (ts:126-153) envia `...bruto` com o campo, então hoje grava.
- (b) EmpresaFormComponent: `frontend/src/app/features/admin/empresa-form/empresa-form.component.{ts,html}`. Usado em `/admin/empresas/nova` e `/admin/empresas/:sessao`. Campo `googleCalendarId` (html:50-60). Erro mostrado em html:13-14 via `erro()`. `mensagemHttp` (auth.interceptor.ts:20-45) usa `erro.error.message` no 400, então o texto do back aparece. O form não tem controle `permiteReservaAutomatica`.

## Design e conteúdo

- `telas.design.json`: `telas.clientePerfil.opcao` (linha, rotulo, dica), L298-301. Não há chave de aviso de leitura. Tela admin não existe no JSON (só apresentacao, login, clientePainel, clientePerfil, v0Dashboard).
- `cliente-perfil.content.json`, em `clientePerfil.robo.reservaAutomatica`: rotulo "Reserva Automática pelo Bot" e dica "Ligada = o WhatsApp mostra a opção...". Não diz que o admin libera.
- Tela admin não tem `*.content.json`: textos como "ID da agenda Google" estão no HTML.

## O que cada tela faz hoje e o que a tarefa pede

| Tela | Hoje com permiteReservaAutomatica | Pedido | Tipo |
|---|---|---|---|
| Perfil (`/cliente/chatbot`) | switch editável, envia no PUT | só leitura, aviso de que o admin libera, não envia o campo | alterada |
| Canvas `ClienteChatbot.dc.html:145` | switch `checked`, editável, sem aviso | idem, no board | alterada |
| Admin (`empresa-form`) | não mostra nem envia | 400 da agenda visível; controle para ligar | nova (controle) + ajuste (erro) |
| Canvas `AdminClienteForm.dc.html` | não tem o controle | idem | nova |

## Lacunas

1. Admin não tem controle para ligar `permiteReservaAutomatica`, no form nem no canvas. Sem ele o 400 da agenda nunca aparece. Resolve: visual (controle) e TypeScript (enviar no POST e PUT).
2. Rota `/cliente/perfil` não existe. Resolve: texto da tarefa (usar `/cliente/chatbot`).
3. Aviso "o admin libera" não tem chave em `cliente-perfil.content.json`. Resolve: texto, usar `[PLACEHOLDER]` até a pessoa pedir.
4. Tela admin sem `*.content.json` e sem entrada em `telas.design.json`. Resolve: visual, decidir se cria.

Estado: aprovado
