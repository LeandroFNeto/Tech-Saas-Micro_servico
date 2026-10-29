---
name: horadoshow
description: >-
  Manda a especificação da tarefa nomeada para a equipe de agentes trabalhar
  em conjunto: programação, testes, qualidade, segurança, revisão e deploy.
  A segurança vem antes da revisão. O deploy usa o modelo mais leve, só encerra o
  trabalho e sobe no GitHub. Cada etapa atualiza openspec/central-tarefas.md e
  encerra. O teste roda a regressão e, se falhar, grava teste-erro.md.
  Use quando o pedido trouxer horadoshow e o nome da tarefa, como
  "horadoshow definir-papeis-ia-desenvolvimento".
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

## Modelos e Equivalências

Cada etapa usa o modelo indicado na tabela abaixo para a ferramenta em uso. Caso o modelo específico não esteja disponível na sua conta/ferramenta, utilize o equivalente da mesma linha:

| Etapa | Modelo na IDE / Usuário | Subagente Antigravity | Cursor | Claude |
|---|---|---|---|---|
| Pesquisa | `Gemini 3.8 Flash Medium` | `flash` | `cursor-grok-4.6-low` | `claude-haiku-5-5` |
| Proposta | `Claude Sonnet 5.5 Medium` / `Gemini 4 Argon` | `inherit` / `pro` | `grok-4.7-high-fast` | `claude-sonnet-5-5` |
| Programação | `Claude Sonnet 5.5 Medium` | `inherit` / `pro` | `claude-sonnet-5-5-high` | `claude-sonnet-5-5` |
| Testes | `Gemini 4 Argon Medium` / `Gemini 3.8 Flash` | `flash` / `inherit` | `gemini-4-argon-medium` | `claude-sonnet-5-5` |
| Qualidade | `Claude Sonnet 5.5 Medium` / `GPT-OSS 120B` | `inherit` / `pro` | `gpt-5.6-sol-medium` | `claude-sonnet-5-5`, em agente novo |
| Segurança | `Claude Opus 5.5 Medium` / `Claude Sonnet 5.5` | `pro` / `inherit` | `claude-opus-5-5-medium` | `claude-opus-5-5` |
| Revisão e spec definitiva | `Claude Opus 5.5 Medium` / `Claude Sonnet 5.5` | `pro` / `inherit` | `claude-opus-5-5-medium` | `claude-opus-5-5` |
| Deploy | `Gemini 3.8 Flash Medium` | `flash_lite` / `flash` | `cursor-grok-4.6-low` | `claude-haiku-5-5` |
| Explicação | `Gemini 3.8 Flash Medium` | `flash` | `cursor-grok-4.6-low` | `claude-haiku-5-5` |

Pesquisa e proposta acontecem antes deste chamado. Esta skill começa na programação.

## Central

`openspec/central-tarefas.md` é o quadro. Uma linha por tarefa.

Ao começar, grave etapa, IA da tabela, local dos arquivos, voltas e estado `em andamento`. Ao terminar, troque para `aprovado`, `reprovado` ou `erro`. Publique a central e encerre. Não fique em espera.

O próximo passo lê essa linha. Sem `aprovado` da etapa anterior, ele não começa.

A programação só começa quando a pessoa grava `aprovado` na linha da tarefa. Um aprovado só no chat não libera.

Publicar a central, no fim de cada etapa: commit só de `openspec/central-tarefas.md` e, se existir, `openspec/changes/<nome>/teste-erro.md`. Envie esse commit para `main`. Não use `git add` sem caminho. Não inclua código de produto, `graphify-out/` nem `.env`. Se a branch atual tiver commit de produto que a `main` ainda não tem, não empurre a branch. Faça o commit desses arquivos em cima de `origin/main`.

## Pesquisa no código

Para achar arquivo, símbolo ou dependência, use o Graphify antes de Glob, Grep ou Read: `graphify query "<pergunta>"`, `graphify path "<A>" "<B>"`, `graphify explain "<conceito>"`. Leia direto só depois de o grafo indicar o arquivo, ou se `graphify-out/graph.json` não existir. Ao fim da etapa, se criou ou alterou arquivo, rode `graphify update .` uma vez. Regra do projeto: `.agents/rules/graphify.md`.

## Skills acopladas

Antes da programação, leia `.agents/skills/horadoshow/acopladas.md`. Se o assunto da tarefa tiver uma linha, abra esse `SKILL.md` e siga. Sem linha, siga só o pacote da change.

Se a change mexer em elemento visual do front, a skill `diasabatico` cuida do trabalho visual; o restante segue o fluxo padrão.

## Equipe

Um agente por papel, na ordem:

1. **Programação**. Abre a skill do assunto em `acopladas.md`, se houver linha, e cumpre `tasks.md`. Se a API muda, o YAML vem antes do Java.
2. **Testes**. Escreve a prova citada e roda a regressão com `mvn test`. A suíte que já passava tem de continuar passando. Se a tarefa alterou `frontend/`, rode também `npm test -- --watch=false --browsers=ChromeHeadless` nessa pasta. Se algo falhar, grave `openspec/changes/<nome>/teste-erro.md` com o comando, o teste que quebrou e o trecho do erro, marque a central como `erro` e pare. Se passar, apague `teste-erro.md` se existir.
3. **Qualidade**. Confere spec, contrato, código e prova juntos. Se reprovar, devolva para a programação e some 1 em Voltas, se ainda houver volta. Não chame a segurança.
4. **Segurança**. Olhe os arquivos da tarefa:
   - Tela só de texto, layout ou estilo (`frontend/` visual, `especificacao/front/*.content.json`, `telas.design.json`), sem rota, autenticação, senha, token ou dado persistido: marque `aprovado` com a nota `tela` e siga para a revisão.
   - Rota, Java, YAML de API, autenticação, senha, token, webhook ou dado de cliente: execute revisão de segurança:
     - Procure vazamento de dados e acesso indevido só nos arquivos citados em `design.md`, seção Arquivos.
     - Usuário, senha, token e `googleCalendarId` não podem vazar.
     - Se achar vazamento ou acesso indevido, devolva para a programação.
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
- Código completo, para `main`: os arquivos da tarefa, a spec definitiva, a central e o arquivo de erro se ainda existir. Ficam de fora `graphify-out/`, `.env` e credenciais.
- O push leva o nome da branch.

## Voltas

No máximo 2. A primeira leitura é a tentativa. Cada devolução para a programação soma 1 na coluna Voltas. Na terceira reprovação, a central fica `reprovado` e a equipe para para a pessoa decidir.
