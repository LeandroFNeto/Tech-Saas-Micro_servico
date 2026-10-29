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

Gera e exibe o resumo completo do projeto.

## Passos

1. Rode `python -I docs/gerar-painel-projeto.py` na raiz do repositório. Ele atualiza `docs/painel-projeto.html`.
2. Apresente os dados:
   - Responda em até cinco linhas: o que está em andamento, o que espera aprovação e o próximo passo de cada change aberta.
   - Forneça o link para [docs/painel-projeto.html](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/docs/painel-projeto.html).
3. Se `graphify-out/graph.json` existir e o gerador for modificado, rode `graphify update .` uma vez.

## O que o gerador lê

| Bloco da página | Fonte |
|---|---|
| Onde estamos | `openspec/central-tarefas.md` |
| O que falta fazer | `openspec/changes/*/proposal.md` (Problema) e `tasks.md` (caixas marcadas) |
| Equipe e modelos | `.agents/rules/papeis-ia.md` |
| Paleta, fontes e componentes | `especificacao/front/telas.design.json` (`tema`) |
| Requisitos | títulos `### <ID>` de `openspec/specs/*/spec.md` |

A página não guarda informação própria. Para mudar o que aparece, altere a fonte correspondente e gere de novo. Não edite `painel-projeto.html` à mão.
