---
name: diasabatico
description: >-
  Foco total na parte visual do front Angular: faz primeiro só o visual
  (HTML, Tailwind, layout, estados), depois a troca de telas, e fecha com uma
  auditoria do que falta e se toda tela já usada está disponível. O trabalho
  visual roda no Opus, porque a pessoa conhece pouco de design e o agente
  decide e explica. Cada fase roda em terminal próprio, no modelo que a fase
  pede, passando o contexto por arquivo. Não liga API, serviço nem backend.
  Use quando o pedido trouxer diasabatico, ou quando a tarefa for tela nova,
  ajuste de layout, estilo ou navegação do frontend. A horadoshow abre esta
  skill quando a change mexe em elemento visual.
disable-model-invocation: true
---

# diasabatico

Dia de foco só na tela. Primeiro o que se vê, depois como se navega, por último o que ficou faltando. Nada de API, Java ou YAML.

## Terminais e ferramentas

Cada fase abre num terminal próprio, com um agente novo, no modelo que a fase pede. A skill roda em duas ferramentas, o Cursor e o Claude. O agente usa a coluna da ferramenta em que está rodando, sem misturar.

### Qual coluna usar

| Onde o agente está | Coluna | O que é um "terminal" |
|---|---|---|
| Cursor | Cursor | um agente novo no Cursor, no modelo da coluna (os mesmos de `.cursor/rules/papeis-ia.mdc`) |
| Claude (Claude Code ou app Claude) | Claude | um subagente em primeiro plano (`Agent` com o `model` da coluna) ou uma sessão nova nesse modelo |

Em dúvida sobre onde está, o agente pergunta uma vez e segue. Não troca de coluna no meio de uma tarefa.

### Fases e modelos

| Terminal | Fase | Cursor | Claude | Por que esse nível | Escopo de escrita |
|---|---|---|---|---|---|
| 1 | Inventário | `cursor-grok-4.6-low` | `claude-haiku-5-5` | só lê e lista; muito arquivo, pouco raciocínio | só `1-inventario.md` |
| 2 | Só o visual | `claude-opus-5-5-medium` | `claude-opus-5-5` | decisão de design, o que a pessoa conhece menos | `*.component.html`, classes Tailwind, `styles.css`, `especificacao/front/telas.design.json` e `*.content.json` |
| 3 | Troca de telas | `claude-sonnet-5-5-high` | `claude-sonnet-5-5` | rotas e estado local em TypeScript: mudança pequena e exata | `app.routes.ts`, `*.routes.ts`, `*.component.ts` (estado local, formulário, `routerLink`) |
| 4 | Auditoria | `gemini-4-argon-medium` | `claude-sonnet-5-5` em agente novo | lê todas as telas de uma vez e roda build e testes: contexto largo | nenhum arquivo de produto, só `4-auditoria.md` |
| 5 | Entrega | `cursor-grok-4.6-low` | `claude-haiku-5-5` | só consolida os arquivos das fases em um quadro | só `5-entrega.md` |

As duas colunas mantêm o mesmo nível de potência por fase: barato para ler e consolidar, o mais forte para o design, médio para código exato, contexto largo para auditar.

Regra de escolha: sobe de modelo só onde a fase pede julgamento (design). Contexto largo vai para quem lê tudo (auditoria). Fase mecânica fica no modelo barato. A auditoria nunca é o agente que escreveu: no Cursor troca de modelo, no Claude abre um agente novo sem o contexto de quem escreveu o visual.

No Claude, o resultado de cada subagente volta pelo arquivo da fase, não só pelo texto da resposta.

### Passagem entre terminais

Nenhum terminal herda a conversa do anterior. Cada um lê o arquivo da fase anterior e grava o seu em `openspec/changes/<nome>/diasabatico/`, com `<nome>` da change (ou o da tela, se não houver change):

`1-inventario.md` → `2-visual.md` → `3-navegacao.md` → `4-auditoria.md` → `5-entrega.md`

Cada arquivo tem até 40 linhas: o que fez, arquivos tocados, o que o próximo precisa saber, e termina com `Estado: aprovado`, `reprovado` ou `erro`. O próximo terminal só abre com `aprovado`.

A sessão que recebeu o chamado só encaminha: abre o terminal, entrega o nome e o arquivo anterior, espera o estado e encerra aquele agente. Não executa fase nenhuma.

### Telas em paralelo

Na fase 2, se a tarefa tem várias telas que não compartilham arquivo, abra um terminal Opus por tela, cada um gravando `2-visual-<tela>.md`. Telas que mexem no mesmo `telas.design.json` ou `styles.css` entram em fila, uma por vez. As demais fases rodam uma vez só, depois que todos os `2-visual-*` estiverem `aprovado`.

### Volta

Auditoria `reprovado` devolve para o terminal da fase que causou o problema (2 ou 3) e depois repete a 4. No máximo 2 voltas; a terceira para e chama a pessoa, como na `horadoshow`.

## Como o agente visual fala

O Opus decide o visual e explica em português simples. A pessoa não precisa saber design: o agente escolhe, mostra o resultado e dá no máximo duas opções quando for gosto, sempre com uma recomendada. Nunca devolve pergunta técnica de CSS.

## Fonte visual

Nesta ordem de autoridade:

1. Desenho ou captura que a pessoa trouxer (canvas de design, v0, imagem). Se existir, é a referência.
2. `especificacao/front/telas.design.json` (cores `brand` e `accent`, fontes Inter e Cinzel, classes por tela) e `*.content.json` (textos).
3. `frontend/src/styles.css` (`btn-primary`, `btn-secondary`, `input-field`, `card`) e as telas prontas, a de `client/perfil` como modelo.

Tela nova segue o padrão do perfil: classes em `telas.design.json`, textos em `*.content.json`, nada hardcoded no HTML quando já existe lugar para ele. O Tailwind já lê esses JSON (`tailwind.config.js`). Cor ou fonte nova vai para o `telas.design.json`, não para o HTML.

A pasta `features/design-v0/` é a área de teste: HTML colado do v0.dev entra ali antes de virar tela de verdade.

## Fases

Uma de cada vez, cada uma no seu terminal da tabela acima. Só avança quando a anterior fecha com `aprovado`.

### 1. Inventário

Antes de escrever, liste e mostre em tabela:

- cada rota de `app.routes.ts` e dos `*.routes.ts` de `admin` e `client`, com o componente e o guard
- cada `routerLink` e `navigate(` do código, com o destino
- cada componente em `features/` e `shared/components/`, e onde é usado
- o que a tarefa pede de tela: nova, alterada ou só ajuste

Destino de link sem rota, componente sem rota nem uso em template e rota sem link entram em Lacunas (fase 4). Não corrija agora.

### 2. Só o visual

- Angular 19 standalone, `@if` e `@for` (sem `*ngIf`), signals como nas telas atuais.
- Dados de exemplo estáticos no próprio componente. Nenhuma chamada a `core/services`, nenhum `HttpClient`, nenhuma mudança em guard ou interceptor.
- Cada tela já com todos os estados: normal, carregando, vazio, erro, sucesso e botão desabilitado. O estado que ainda não tem dado real fica visível com dado de exemplo.
- Responsivo em 375, 768 e 1280 px, sem rolagem horizontal da página. Tabela larga rola dentro da própria caixa.
- Acessível: `label` ligado ao campo, botão é `<button>`, link é `<a>`, imagem com `alt`, foco visível, texto com contraste legível sobre o roxo escuro.
- Marca: fundo escuro `brand-900` no login, laranja `accent` para a ação principal, Cinzel só para a marca e títulos de destaque.

### 3. Troca de telas

- Registre a rota da tela nova em `app.routes.ts` ou no `*.routes.ts` do papel (`admin` ou `client`), com o guard do papel.
- Ligue ida e volta: menu lateral em `shell`, botão "Voltar", link de cada linha de tabela.
- Teste o caminho clicando: login, painel, tela nova, voltar. Rota inexistente cai em `login` (`**`), isso não conta como disponível.

### 4. Auditoria

Passe por cada tela usada e marque o que falta:

| Item | Pergunta |
|---|---|
| Disponível | a rota existe, abre sem erro no console e o guard do papel permite? |
| Alcançável | existe um link ou menu que leva até ela, e outro que volta? |
| Estados | tem carregando, vazio, erro e sucesso, onde faz sentido? |
| Texto | algum texto fixo no HTML que deveria estar no `content.json`? |
| Estilo | alguma cor ou classe fora dos tokens do `telas.design.json`? |
| Responsivo | quebra em 375 px? |
| Acesso | campo sem label, imagem sem alt, ícone sem nome? |
| Dado | a tela mostra um campo que o DTO em `especificacao/back/` não tem? |

Verificação obrigatória: `npm run build` em `frontend/` sem erro. Se mexeu em componente que tem `.spec.ts`, rode também `npm test -- --watch=false --browsers=ChromeHeadless`. O que falhar vai para o relatório, não é escondido.

### 5. Entrega

Responda com este quadro, em português simples:

1. O que ficou pronto, por tela, em uma linha.
2. Lacunas: tabela com tela, o que falta, e quem resolve (visual, rota, texto, contrato).
3. O que depende de API: campo ou estado que só fecha quando o backend existir. Isso vira pedido de contrato (YAML antes do Java, `openapi-contract-first.mdc`), nunca código nesta skill.
4. O que a pessoa precisa decidir, se algo for gosto.

Dentro da `horadoshow`, grave esse quadro em `openspec/changes/<nome>/lacunas-front.md` e deixe a central seguir o fluxo normal.

## Pesquisa no código

Para achar arquivo, símbolo ou dependência, use o Graphify antes de Glob, Grep ou Read: `graphify query "<pergunta>"`, `graphify path "<A>" "<B>"`, `graphify explain "<conceito>"`. Vale em todas as fases, em especial no Inventário (rotas, links e componentes) e na Auditoria. Leia direto só depois de o grafo indicar o arquivo, ou se `graphify-out/graph.json` não existir. Ao fim de cada fase que criou ou alterou arquivo, rode `graphify update .` uma vez. O prompt de cada terminal que a skill abre leva essa regra. Regra do projeto: `.cursor/rules/graphify.mdc`.

## Fora do escopo

- Ligar tela a serviço, `HttpClient`, autenticação, guards e interceptors
- Qualquer mudança em Java, `especificacao/back/` ou `swagger.yaml`
- Texto de produto novo sem a pessoa pedir: use `[PLACEHOLDER]` e liste nas lacunas
- Biblioteca de componentes nova ou troca de Tailwind
- Commit, push ou deploy: ficam com a `horadoshow`

## Quando não usar

Mudança sem efeito visual (regra de negócio, rota de API, teste de serviço) não passa por aqui.
