# Fase 2A: visual, fundação (26/10/2026)

## O que foi feito
- `frontend/src/styles.css`: `.btn-primary` e `.btn-secondary` com `min-h-11` (44 px), sem mudar fonte nem padding. `.input-field` com `min-h-12`, `text-base`, `px-4 py-3`, `rounded-xl`, borda brand-200, foco accent-400 + anel accent-100. Novas: `.switch` (48×28, `input[type=checkbox]` real por cima, invisível, ligado = accent-500, foco com contorno accent-400, estado desabilitado), `.chip` (pílula; a cor vem da tela) e `.wordmark` (Cinzel, caixa alta, degradê cobre com `bg-clip-text` e reserva `#e2a775`).
- `especificacao/front/telas.design.json`: `accent.800 = #9a4508`; seções `telas.apresentacao` e `telas.login`. Nada removido.
- `especificacao/front/apresentacao.content.json` e `login.content.json`: todo o texto visível, igual aos `.dc.html`. A linha "[CONTATO DA GAMB]" ficou fora. 401 = "E-mail ou senha inválidos. Confira os dados e tente de novo."
- Imagens: `frontend/public/logo-gamb.png` e `conversa-whatsapp.jpg` copiados; `logo-gamb.jpg` movido para `docs/logo-gamb-antiga.jpg`; `docs/gerar-readme-hero.py:15` aponta para lá. `index.html`: ícone `image/png` + `logo-gamb.png`. Nenhum `logo-gamb.jpg` sobrou em `frontend/src`.
- `features/apresentacao/apresentacao.component.ts/.html` (novo, standalone, `RouterLink`): visual do `Apresentacao.dc.html`; Entrar (topo e fim) com `routerLink="/login"`; "Ver como funciona" é `<a href="#como">` com `(click)` que faz `preventDefault` + `scrollIntoView` (suave, respeita `prefers-reduced-motion`) e leva o foco para a seção (`tabindex="-1"`). Rota NÃO registrada.
- Login (`login.component.html/.ts`): faixa da marca + cartão, `label for` ligado a cada campo, logo 64–96 px (`clamp`) + "gamb" em `.wordmark`, Mostrar/Ocultar (`mostrarSenha` signal, `aria-pressed`, `aria-label`), "← Conhecer o gamb" e logo com `routerLink="/"`. Formulário reativo, validação, `auth.login` e redirecionamento intactos; só a mensagem do 401 mudou (vem do JSON).
- Shell: celular = barra superior (logo 40 px, "gamb", "Painel Master"/"Painel do Cliente", Sair) + abas fixas embaixo só para admin (ícones do desenho, ativa = accent-800 com filete laranja). `lg:` = barra lateral 248 px, sticky com altura da tela, mesma navegação, e-mail e Sair. Mesmos `routerLink`, `RouterLinkActive` (+ `aria-current="page"`), `<router-outlet />` e lógica do `.ts`; só entrou `NgTemplateOutlet` e duas strings de classe.

## Decisões de design
- Classes das duas telas novas ficam no JSON (padrão do perfil); o shell mantém Tailwind no HTML, como pedido.
- Ícones, marca e Sair do shell são `ng-template` reusados: uma fonte só para celular e desktop.
- Campos do login com 52 px e cantos de 14 px, como o desenho (a `.input-field` global fica em 48 px).
- Barra lateral sticky: em listas longas o Sair não some do desktop.

## Para a parte B e a fase 3
- Interruptor: `<span class="switch"><input type="checkbox" ... /><span aria-hidden="true"></span></span>` (o `input` precisa de `aria-label` ou estar dentro do `label` da linha). `.chip` + cores por tela, ex. `bg-accent-50 text-accent-800`.
- `.switch` e `.chip` ainda não saem no CSS do build porque nenhum template os usa (o Tailwind remove classe de `@layer components` sem uso); aparecem assim que a parte B usar.
- O shell não tem mais o cabeçalho branco com e-mail: o `main` é `px-4 py-5 lg:px-10 lg:py-8`; cada tela traz o próprio título.
- Fase 3: registrar `''` → `ApresentacaoComponent` (lazy) e `**` → `''`; a apresentação ainda não manda quem tem sessão para `auth.rotaInicial()` (é lógica de navegação, fica para a fase 3). Hoje `routerLink="/"` do login cai no redirect atual para `/login`.

## Verificação
- `npm run build` em `frontend/`: sem erro e sem aviso de budget. A apresentação não entra no bundle (sem rota); foi conferida com `ngc` e `strictTemplates` à parte, sem erro.
- Não conferido no navegador em 375/768/1280 px (fica para a auditoria).

## Riscos
- Botões pequenos (`btn-secondary px-3 py-1 text-xs` do perfil) ficam com 44 px de altura: mais altos que antes, sem quebrar o layout.
- `conversa-whatsapp.jpg` é conversa real: conferir antes do deploy (já previsto no `design.md`).

Estado: aprovado
