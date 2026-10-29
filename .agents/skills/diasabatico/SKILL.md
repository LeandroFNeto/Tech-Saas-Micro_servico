---
name: diasabatico
description: >-
  Foco total na parte visual do front Angular: faz primeiro só o visual
  (HTML, Tailwind, layout, estados), depois a troca de telas, e fecha com uma
  auditoria do que falta e se toda tela já usada está disponível. O trabalho
  visual foca em decisão de design e acabamento. Cada fase roda com contexto
  passado por arquivo em openspec/changes/<nome>/diasabatico/. Não liga API,
  serviço nem backend. Use quando o pedido trouxer diasabatico, ou quando a tarefa
  for tela nova, ajuste de layout, estilo ou navegação do frontend. A horadoshow
  abre esta skill quando a change mexe em elemento visual.
---

# diasabatico

Dia de foco só na tela. Primeiro o que se vê, depois como se navega, por último o que ficou faltando. Nada de API, Java ou YAML.

## Passagem entre fases

Cada fase grava seu resultado em `openspec/changes/<nome>/diasabatico/`, com `<nome>` da change (ou o da tela):

`1-inventario.md` → `2-visual.md` → `3-navegacao.md` → `4-auditoria.md` → `5-entrega.md`

Cada arquivo resume: o que fez, arquivos tocados, o que a próxima fase precisa saber, e termina com `Estado: aprovado`, `reprovado` ou `erro`.

## Fonte visual

Nesta ordem de autoridade:

1. Desenho ou captura fornecida (canvas de design, v0, imagem). Se existir, é a referência.
2. `especificacao/front/telas.design.json` (cores `brand` e `accent`, fontes Inter e Cinzel, classes por tela) e `*.content.json` (textos).
3. `frontend/src/styles.css` (`btn-primary`, `btn-secondary`, `input-field`, `card`) e as telas prontas, a de `client/perfil` como modelo.

Tela nova segue o padrão do perfil: classes em `telas.design.json`, textos em `*.content.json`, nada hardcoded no HTML quando já existe lugar para ele. Cor ou fonte nova vai para o `telas.design.json`, não para o HTML.

## Fases

1. **Inventário**:
   - Listar rotas de `app.routes.ts` e dos `*.routes.ts` de `admin` e `client`, com componente e guard.
   - Listar `routerLink` e `navigate(` do código com destino.
   - Listar componentes em `features/` e `shared/components/`.
   - Gerar `1-inventario.md`.
2. **Só o visual**:
   - Angular 19 standalone, `@if` e `@for` (sem `*ngIf`), signals.
   - Decisão de layout, Tailwind e alinhamento visual.
   - Gerar `2-visual.md`.
3. **Troca de telas**:
   - Rotas, guards e estado local em TypeScript.
   - Formulários e `routerLink`.
   - Gerar `3-navegacao.md`.
4. **Auditoria**:
   - Compilação e testes do front: `npm test -- --watch=false --browsers=ChromeHeadless` na pasta `frontend/`.
   - Verificar se há telas órfãs ou links quebrados.
   - Gerar `4-auditoria.md`.
5. **Entrega**:
   - Consolidação de inventário e entrega em `5-entrega.md`.
