# 3 - Navegação (terminal 3)

## O que mudou
- `frontend/src/app/app.routes.ts`: rota `''` agora faz `loadComponent` lazy de `ApresentacaoComponent` (pathMatch full); `**` redireciona para `''`.
- `features/apresentacao/apresentacao.component.ts`: injeta `AuthService` e `Router`; no construtor, quem está autenticado vai para `rotaInicial()` (igual ao login). Resto intacto.
- Nenhum outro arquivo alterado (sem html, Java, guards, serviços).

## Links conferidos
- Apresentação: "Entrar" (topo e chamada final) -> `/login`; "Como funciona" é âncora com scroll (`irParaComo`), sem rota.
- Login: logo e "← Conhecer o gamb" -> `/`.
- `logout()` segue para `/login` (não alterado). Nenhum routerLink/navigate quebrado.
- "Ver tutorial →" em `banner-avisos` (usado pelo perfil nas duas variantes): usa `[href]="aviso.link"`, `target="_blank"`, `rel="noreferrer"`, só dentro de `@if (aviso.link)`. Não há `href="#"`. O perfil não tem botão próprio. Nada a corrigir.

## Verificação
- `npm run build`: sem erro; novo chunk lazy `apresentacao-component` (6,42 kB).
- `npm test -- --watch=false --browsers=ChromeHeadless`: Chrome Headless 155, 1 de 1 SUCCESS.

## Riscos
- Rota `design-v0` segue aberta (fora desta change).
- Usuário autenticado vê a apresentação por um instante antes do redirecionamento (renderiza e navega no construtor, como o login).
- Só há 1 teste no projeto; navegação não coberta por testes automatizados.

Estado: aprovado
