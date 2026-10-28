# Segurança: aplicar-novo-design-front

Escopo: só os arquivos da seção "Arquivos" de `design.md` (diff de `frontend`, `especificacao/front`, `docs`). Java, testes e `openspec/specs/whatsapp` ficaram de fora.

## 1. Rotas `''` e `**` (OK)
- `app.routes.ts:6-11`: `''` agora abre `ApresentacaoComponent` (pública, sem dado); `**` → `''` (linha 32). As rotas `admin`/`cliente`, com `authGuard` e `roleGuard`, ficaram iguais; `core/guards` não mudou.
- Não há open redirect: nenhum `returnUrl` nem `queryParams` lido. Os links são fixos (`routerLink="/"` e `"/login"`).

## 2. Redirecionamento de quem já tem sessão (OK)
- `apresentacao.component.ts:19-22` usa só `auth.isAuthenticated()` e `navigateByUrl(auth.rotaInicial())`. O login segue o fluxo antigo.
- Nada novo vai para o DOM, o log, a URL ou o `localStorage`: o `localStorage` continua só em `auth.service.ts`, que não mudou. O botão Mostrar senha muda só o `type` do campo local (`login.component.ts:44-46`).

## 3. Erro 401 no login (OK, melhorou)
- Antes: "Senha incorreta", que dava a entender que o e-mail existia. Agora: `login.content.json` → "E-mail ou senha inválidos...", texto genérico (`login.component.ts:63-65`).
- Os outros erros continuam passando por `mensagemHttp` e saem por interpolação `{{ erro }}`, com escape do Angular.

## 4. `[innerHTML]` no banner de avisos (OK)
- `banner-avisos.component.html` continua com `[innerHTML]="aviso.conteudo"` e `[href]="aviso.link"`, como antes. A change só reorganizou o layout e os links externos mantêm `target="_blank" rel="noreferrer"`.
- Não há `bypassSecurityTrust*` em `frontend/src`, então o sanitizador do Angular continua ativo.

## 5. Arquivos públicos e referência do canvas (OK)
- `frontend/public/conversa-whatsapp.jpg`: conferi a imagem. Foto, nome do contato, nome da empresa e a linha de mensagens temporárias estão cobertos, e não aparece número de telefone. O arquivo é idêntico (mesmo SHA-256) à cópia em `referencia-canvas/`.
- `logo-gamb.png` e `docs/logo-gamb-antiga.jpg` são só logos (o script `gerar-readme-hero.py` só mudou o caminho).
- `referencia-canvas/*.dc.html`: não há token, chave nem telefone real. Os e-mails são fictícios (`admin@`, `sessao@`, `sessao_recanto_01@gamb.com.br`), os campos de senha vêm vazios e as URLs são placeholders (`.../`). O `/_blob/<hash>` aponta para a mesma imagem já coberta.
- O campo `googleCalendarId` e os campos de e-mail e senha do `empresa-form` só mudaram de posição: continuam na tela de admin, protegida pelo guard.

## 6. Dependências e scripts externos (OK)
- `package.json` e `package-lock.json` não mudaram. Em `index.html`, só o favicon mudou (jpg → png local); as fontes Google já existiam. Não entrou domínio novo.

## 7. `tailwind.config.js` e `styles.css` (OK)
- `tailwind.config.js` não mudou. `styles.css` não tem `@import`, `url(` nem `http`.

## Recomendação (baixo)
- `design.md:51`: conferir de novo a imagem da conversa antes do deploy, como o próprio design já pede (ela é pública em `/conversa-whatsapp.jpg`).

Estado: aprovado
