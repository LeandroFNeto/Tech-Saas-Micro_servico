---
name: painel
description: >-
  Gera a página web de situação do projeto: em que etapa cada tarefa está,
  o que falta em cada change e o próximo passo, a equipe de IA, a paleta de
  cores, as fontes, os componentes e os requisitos das specs. Use quando o
  pedido trouxer painel, ou perguntar em que passo o projeto está, o que
  falta fazer, ou pedir a paleta e as fontes do projeto.
---

# Painel do projeto

Roda no modelo da linha Explicação de `.cursor/rules/papeis-ia.mdc`, na coluna da ferramenta em uso.

## Passos

1. Rode `python -I docs/gerar-painel-projeto.py` na raiz do repositório. Ele grava `docs/painel-projeto.html`.
2. Mostre a página:
   - Claude: publique `docs/painel-projeto.html` com a ferramenta Artifact, no mesmo endereço `https://claude.ai/artifact/CqRELkSbiWW7RNeFz31t4o` (passe esse `url` quando a conversa ainda não publicou o arquivo).
   - Cursor: abra `docs/painel-projeto.html` no navegador.
3. Responda em até cinco linhas: o que está em andamento, o que espera aprovado e o próximo passo de cada change aberta.

## O que o gerador lê

| Bloco da página | Fonte |
|---|---|
| Onde estamos | `openspec/central-tarefas.md` |
| O que falta fazer | `openspec/changes/*/proposal.md` (Problema) e `tasks.md` (caixas marcadas) |
| Equipe e modelos | tabela Modelos de `.cursor/rules/papeis-ia.mdc` |
| Paleta, fontes e componentes | `especificacao/front/telas.design.json` (`tema`) |
| Requisitos | títulos `### <ID>` de `openspec/specs/*/spec.md` |

A página não guarda informação própria. Para mudar o que aparece, mude a fonte e gere de novo. Não edite `painel-projeto.html` à mão.

## Pesquisa no código

Para achar arquivo, símbolo ou dependência, use o Graphify antes de Glob, Grep ou Read: `graphify query "<pergunta>"`, `graphify path "<A>" "<B>"`, `graphify explain "<conceito>"`. Leia direto só depois de o grafo indicar o arquivo, ou se `graphify-out/graph.json` não existir. Ao fim, se criou ou alterou arquivo (inclusive o gerador do painel), rode `graphify update .` uma vez. O prompt de cada agente que a skill abre leva essa regra. Regra do projeto: `.cursor/rules/graphify.mdc`.
