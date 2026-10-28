---
name: horadoshow
description: >-
  Manda a especificação da tarefa nomeada para a equipe de agentes trabalhar
  em conjunto: programação, testes, qualidade, segurança, revisão e deploy.
  A segurança vem antes da revisão, as duas no Opus. O deploy usa o modelo
  mais barato, só encerra o trabalho e sobe no GitHub. Cada etapa atualiza
  openspec/central-tarefas.md e encerra. O teste roda a regressão e, se
  falhar, grava teste-erro.md.
  Use quando o pedido trouxer horadoshow e o nome da tarefa, como
  "horadoshow definir-papeis-ia-desenvolvimento".
disable-model-invocation: true
---

# horadoshow

Manda a especificação para a equipe trabalhar em conjunto. O nome da tarefa vem na mesma frase, depois de `horadoshow`. Sem esse nome, pare e peça o nome. Não invente a tarefa.

## Nome

O nome é kebab-case e abre `openspec/changes/<nome>/`.

Exemplo: `horadoshow definir-papeis-ia-desenvolvimento`.

## Pacote

Entregue a cada agente estes arquivos:

- `proposal.md`, `design.md` e `tasks.md` da pasta
- o `spec.md` do domínio citado na proposta
- `openspec/specs/00-architecture.md`

## Modelos

Cada etapa usa só o modelo desta tabela, na coluna da ferramenta em que o agente está rodando: Cursor ou Claude. Não mistura colunas. Em dúvida sobre a ferramenta, pergunte uma vez. Na volta, o mesmo modelo repete o próprio papel.

| Etapa | Cursor | Claude |
|---|---|---|
| Pesquisa | `cursor-grok-4.6-low` | `claude-haiku-5-5` |
| Proposta | `grok-4.7-high-fast` | `claude-sonnet-5-5` |
| Programação | `claude-sonnet-5-5-high` | `claude-sonnet-5-5` |
| Testes | `gemini-4-argon-medium` | `claude-sonnet-5-5` |
| Qualidade | `gpt-5.6-sol-medium` | `claude-sonnet-5-5`, em agente novo |
| Segurança | `claude-opus-5-5-medium` | `claude-opus-5-5` |
| Revisão e spec definitiva | `claude-opus-5-5-medium` | `claude-opus-5-5` |
| Deploy | `cursor-grok-4.6-low` | `claude-haiku-5-5` |

As duas colunas têm o mesmo nível de potência por etapa. No Claude, "abrir um agente" é um subagente em primeiro plano (`Agent` com o `model` da coluna) ou uma sessão nova nesse modelo; no Cursor é um agente novo no modelo da coluna. Cada agente do Claude começa sem o contexto do anterior, só com o pacote e a linha da central.

Pesquisa e proposta acontecem antes deste chamado. Esta skill começa na programação.

## Central

`openspec/central-tarefas.md` é o quadro. Uma linha por tarefa.

Ao começar, grave etapa, IA da tabela, local dos arquivos, voltas e estado `em andamento`. Ao terminar, troque para `aprovado`, `reprovado` ou `erro`. Publique a central e encerre. Não fique em espera.

O próximo passo lê essa linha. Sem `aprovado` da etapa anterior, ele não começa.

A programação só começa quando a pessoa grava `aprovado` na linha da tarefa. Um aprovado só no chat não libera.

Publicar a central, no fim de cada etapa: commit só de `openspec/central-tarefas.md` e, se existir, `openspec/changes/<nome>/teste-erro.md`. Envie esse commit para `main`. Não use `git add` sem caminho. Não inclua código de produto, `graphify-out/` nem `.env`. Se a branch atual tiver commit de produto que a `main` ainda não tem, não empurre a branch. Faça o commit desses arquivos em cima de `origin/main`.

## Agentes

A sessão que recebeu `horadoshow` só encaminha. Ela não programa, não testa, não revisa e não faz deploy.

Para programação, testes, qualidade, revisão e deploy, abra um agente novo com o modelo da tabela. Entregue o pacote, a linha da central e o resultado da etapa anterior. Espere a central mudar e encerre esse agente.

A segurança, quando não for tela, abre um `security-review` no modelo da Segurança.

## Pesquisa no código

Para achar arquivo, símbolo ou dependência, use o Graphify antes de Glob, Grep ou Read: `graphify query "<pergunta>"`, `graphify path "<A>" "<B>"`, `graphify explain "<conceito>"`. Leia direto só depois de o grafo indicar o arquivo, ou se `graphify-out/graph.json` não existir. Ao fim da etapa, se criou ou alterou arquivo, o agente roda `graphify update .` uma vez. O prompt de cada agente que a `horadoshow` abre (programação, testes, qualidade, segurança, revisão e deploy) leva essa regra, e o subagente `security-review` também. Se a cópia de trabalho for outra pasta do repositório, o grafo e o update valem para essa pasta. Regra do projeto: `.cursor/rules/graphify.mdc`.

## Skills acopladas

Antes da programação, leia `.cursor/skills/horadoshow/acopladas.md`. Se o assunto da tarefa tiver uma linha, abra esse `SKILL.md` e siga. Sem linha, siga só o pacote da change.

Skill nova, inclusive uma de design, entra como uma linha nessa lista. O conteúdo dela não é copiado para cá. Toda skill nova que escolhe modelo traz as duas colunas, Cursor e Claude (`.cursor/rules/papeis-ia.mdc`).

Se a skill acoplada fixar o modelo de uma parte da programação, vale para essa parte, na coluna da ferramenta em uso. A `diasabatico` roda o trabalho visual (HTML, Tailwind e `especificacao/front/`) no Opus; o resto da programação segue a tabela de modelos.

## Equipe

Um agente por papel, no modelo da tabela. O próximo só começa se o anterior passou.

1. **Programação**. Abre a skill do assunto em `acopladas.md`, se houver linha, e cumpre `tasks.md`. Se a API muda, o YAML vem antes do Java.
2. **Testes**. Escreve a prova citada e roda a regressão com `mvn test`. A suíte que já passava tem de continuar passando. Se a tarefa alterou `frontend/`, rode também `npm test -- --watch=false --browsers=ChromeHeadless` nessa pasta. Se algo falhar, grave `openspec/changes/<nome>/teste-erro.md` com o comando, o teste que quebrou e o trecho do erro, marque a central como `erro` e pare. Se passar, apague `teste-erro.md` se existir.
3. **Qualidade**. Confere spec, contrato, código e prova juntos. Se reprovar, devolva para a programação e some 1 em Voltas, se ainda houver volta. Não chame a segurança.
4. **Segurança**. Olhe os arquivos da tarefa.

Tela só de texto, layout ou estilo (`frontend/` visual, `especificacao/front/*.content.json`, `telas.design.json`), sem rota, autenticação, senha, token ou dado persistido: marque `aprovado` com a nota `tela` e siga para a revisão. Não chame o subagente.

Rota, Java, YAML de API, autenticação, senha, token, webhook ou dado de cliente: lance um subagente `security-review`, em primeiro plano, com este texto:

```text
Full Repository Path: <caminho absoluto deste repositório>
Diff: branch changes
Custom Instructions: Procure vazamento de dados e acesso indevido só nos arquivos citados em design.md, seção Arquivos. Ignore o resto da branch. Usuário, senha, token e googleCalendarId não podem sair. Ignore tela, texto e estilo.
```

Se achar vazamento ou acesso indevido, devolva a lista para a programação. Não aplique patch e não chame a revisão.
5. **Revisão**. Lê o que a equipe entregou. Se passar, grava o requisito definitivo em `openspec/specs/<dominio>/spec.md` e a linha em `openspec/specs/README.md`. Essa gravação é o que torna a spec definitiva. Se reprovar, devolve o que falhou para a programação. Não chama o deploy.
6. **Deploy**. Só encerra: confere que a spec definitiva já está gravada, move a pasta para `openspec/archive/<nome>/` e sobe no GitHub nas branches abaixo. Não escreve o requisito e não altera código de produto.

## Git

| Branch | O que entra | Quem publica |
|---|---|---|
| `producao` | o front: `frontend/` e `vercel.json` | Vercel |
| `main` | o código completo: API, front, WhatsApp, Docker e esta central | Hostinger |

`producao` não recebe API, Docker nem `wppconnect-server/`. `main` é a branch do código completo. O deploy sobe cada uma no próprio destino, depois da segurança, da revisão e da spec gravada.

O deploy não usa `git add` sem caminho e não usa `--force`.

- Front, para `producao`: só `frontend/` e `vercel.json`.
- Código completo, para `main`: os arquivos da tarefa, a spec definitiva, a central e o arquivo de erro se ainda existir. Ficam de fora `graphify-out/`, `.env` e credencial.
- O push leva o nome da branch. Sem nome, não envia.

## Voltas

No máximo 2. A primeira leitura é a tentativa. Cada devolução para a programação soma 1 na coluna Voltas. Na terceira reprovação, a central fica `reprovado` e a equipe para para a pessoa decidir. Não há quarta leitura.

A volta repete programação, testes e qualidade, cada um no próprio modelo. Se a tarefa tiver dado ou acesso, a segurança roda de novo antes da revisão. Tela continua sem o subagente.

| Nível | Onde | O que reprova |
|---|---|---|
| alto | Segurança | vazamento de dados ou acesso indevido |
| médio | Revisão | o código não cumpre o requisito |
| baixo | Tela | texto, layout e estilo; não dispara a segurança |

## Parada

- Sem `aprovado` gravado pela pessoa na central, a programação não começa.
- Teste em `erro`, ou `teste-erro.md` presente: não chame a qualidade.
- Qualidade reprovada: volte para a programação, se ainda houver volta. Não chame a segurança.
- Segurança com vazamento ou acesso indevido: volte para a programação, se ainda houver volta. Não chame a revisão.
- Tela: a segurança não roda o subagente.
- Revisão reprovada: volte para a programação, se ainda houver volta. Não chame o deploy.
- Voltas em 2 e nova reprovação: pare e chame a pessoa.
- O deploy não sobe no GitHub sem a spec definitiva gravada pela revisão, nem com segurança ou revisão pendente.
- `producao` não recebe código que não seja o front.
