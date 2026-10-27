# Design

## Decisão

Porte manual, arquivo por arquivo, a partir de `origin/main`. Fonte do design: `git diff 449d852 cbc6fdd -- frontend especificacao docs`. Regra de conflito: o código da `main` vence em comportamento (serviços, polling, Cloudinary, editor do WhatsApp, perfil no celular); o `cbc6fdd` vence em visual (marcação, classes, textos aprovados).

**Achado que muda o plano**: a `main` apagou `especificacao/front/` inteiro, tirou `resolveJsonModule` do `tsconfig.json` e os `assets` de `../especificacao/front` do `angular.json`, e fixou o tema direto em `tailwind.config.js` (provavelmente para a Vercel, com raiz em `frontend/`, buildar sem sair da pasta). O design da `producao` importa JSON de `especificacao/front` (`apresentacao`, `login`, `perfil`) e o Tailwind lê esses JSON. Trazer isso de volta reabriria o risco que a `main` removeu. Proposta: **o porte não recria `especificacao/front` nem `resolveJsonModule`**; os textos de `apresentacao.content.json`, `login.content.json` e `cliente-perfil.content.json` e as classes de `telas.design.json` entram como constantes TypeScript dentro de cada componente, e o token `accent-800` entra direto em `tailwind.config.js`. O dono decide nas perguntas abaixo.

## Conflitos esperados e como resolver

| Arquivo | O que a `main` tem | Resolução |
|---|---|---|
| `features/admin/dashboard/dashboard.component.ts` e `.html` | polling de 5 s, `statusPorSessao`, `statusDe` | HTML novo (cartão/linha) + `ngOnInit`/`ngOnDestroy` e `statusDe` da `main`. Chip de status com a cor do desenho (conectado, desconectado, indisponível). Estado "consultando" distinto de "DISCONNECTED". Corrige o bug relatado |
| `features/admin/empresa-form/` | campos novos (Cloudinary, editor do WhatsApp) | layout novo com todos os campos da `main` |
| `features/client/perfil/` | perfil no celular, `.ts` com +151 linhas | abas e seletor do desenho sobre a lógica da `main` |
| `styles.css` | `body` com Inter | `.switch`, `.chip`, `.wordmark`, `.input-field` e campos 48 px/16 px somados, mantendo o `body` da `main` |
| `app.routes.ts` | sem `design-v0` | `''` → `ApresentacaoComponent`, `**` → `''`. `design-v0` não existe na `main`, então a dúvida do guard some |
| `tailwind.config.js` | tema fixo, `content` sem JSON | mantém o da `main`; só soma `accent-800` se algum componente usar |
| `telas.design.json`, `cliente-perfil.content.json` | apagados | não voltam (ver Decisão) |

## Arquivos
- Contrato: nenhum.
- Java: nenhum.
- Front: `frontend/src/app/app.routes.ts`, `features/apresentacao/` (novo, com spec), `features/auth/login/`, `shared/components/shell/`, `features/admin/dashboard/`, `features/admin/empresa-form/`, `features/admin/gestao-avisos/`, `features/client/perfil/`, `shared/components/banner-avisos/`, `shared/components/conexao-whatsapp/`, `frontend/src/styles.css`, `frontend/src/index.html`, `frontend/public/logo-gamb.png`, `frontend/public/conversa-whatsapp.jpg`, `docs/gerar-readme-hero.py`, `docs/logo-gamb-antiga.jpg`.
- Specs: `openspec/specs/auth/spec.md` (AUTH-03), `openspec/specs/README.md`, `openspec/central-tarefas.md`.
- Prova: `apresentacao.component.spec.ts`, `app.component.spec.ts`, suíte existente; `npm run build` e `npm test -- --watch=false --browsers=ChromeHeadless` em `frontend/`; `./mvnw -q test`; navegador em 375 e 1280 px com o status do admin mudando (API ou stub).

## Alinhamento
- Stack de `00-architecture.md`.
- Sem DTO, rota de API ou `$ref` novo.
- `.cursor/skills/horadoshow` e `.cursor/skills/diasabatico`, coluna Claude: haiku lê, opus faz o visual, sonnet navega/testa/qualidade, opus faz segurança e revisão; uma etapa por agente.
- Materiais do design (não versionados) lidos de `C:\Projeto-futuro-junior\Tech-Saas-Micro_servico`: `openspec/changes/aplicar-novo-design-front/` e `especificacao/front/referencia-canvas/`.

## Riscos
- **Regressão do que só a `main` tem**: perfil no celular, Cloudinary, editor do WhatsApp, polling. Mitigação: diff de cada arquivo conflitante contra `origin/main` antes do commit; teste do polling no dashboard (stub).
- **Texto de produção**: "[CONTATO DA GAMB]" fica fora até o dono informar o contato.
- **Imagem real**: conferir `conversa-whatsapp.jpg` (contato, empresa, mensagens temporárias) antes do deploy.
- **Raiz da Vercel**: nada pode depender de arquivo fora de `frontend/`.
- **Publicar**: subir para a `main` só com confirmação explícita do dono; `graphify-out/` e `.env` nunca entram; sem `--force`.

## Perguntas ao dono
1. Aceita os textos e classes como constantes TypeScript, sem recriar `especificacao/front`? (recomendado)
2. Contato da gamb: informa agora ou a linha segue fora?
3. `design-v0` não existe na `main`; confirma que não volta.
