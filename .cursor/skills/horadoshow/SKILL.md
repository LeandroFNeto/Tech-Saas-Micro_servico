---
name: horadoshow
description: >-
  Manda a especificação da tarefa nomeada para a equipe de agentes trabalhar
  em conjunto: programação, testes, qualidade, segurança, revisão e deploy.
  A segurança vem antes da revisão, as duas no Opus. O deploy usa o modelo
  mais barato, só encerra o trabalho e sobe no GitHub.
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

Cada etapa usa só o modelo desta tabela. Na volta, o mesmo modelo repete o próprio papel.

| Etapa | Modelo |
|---|---|
| Pesquisa | `cursor-grok-4.6-low` |
| Proposta | `grok-4.7-high-fast` |
| Programação | `claude-sonnet-5-5-high` |
| Testes | `gemini-4-argon-medium` |
| Qualidade | `gpt-5.6-sol-medium` |
| Segurança | `claude-opus-5-5-medium` |
| Revisão | `claude-opus-5-5-medium` |
| Deploy | `cursor-grok-4.6-low` |

Pesquisa e proposta acontecem antes deste chamado. Esta skill começa na programação.

## Equipe

Um agente por papel, no modelo da tabela. O próximo só começa se o anterior passou. O agente atual não assume o papel seguinte.

1. **Programação**, `claude-sonnet-5-5-high`. Cumpre `tasks.md`. Se a API muda, o YAML vem antes do Java.
2. **Testes**, `gemini-4-argon-medium`. Escreve e roda a prova citada.
3. **Qualidade**, `gpt-5.6-sol-medium`. Confere spec, contrato, código e prova juntos. Reprova ou libera a segurança.
4. **Segurança**, `claude-opus-5-5-medium`. Lance um subagente `security-review`, em primeiro plano, com este texto:

```text
Full Repository Path: <caminho absoluto deste repositório>
Diff: branch changes
```

Se o subagente achar falha, devolva a lista para a programação. Não aplique patch e não chame a revisão.
5. **Revisão**, `claude-opus-5-5-medium`. Lê o que a equipe entregou. Se reprovar, devolve o que falhou para a programação. Não edita produto e não chama o deploy.
6. **Deploy**, `cursor-grok-4.6-low`. Só encerra: grava o requisito em `openspec/specs/`, move a pasta para `openspec/archive/<nome>/`, faz o commit e sobe no GitHub. Não revisa e não altera código de produto. A spec fica definitiva no projeto.

A volta da segurança ou da revisão repete programação, testes e qualidade, cada um no próprio modelo. A segurança roda de novo antes da revisão.

## Parada

- Sem o “aprovado” da pessoa na pasta, a programação não começa.
- Qualidade reprovada: não chame a segurança.
- Segurança com falha: volte para a programação. Não chame a revisão.
- Revisão reprovada: volte para a programação. Não chame o deploy.
- O deploy não sobe no GitHub com segurança ou revisão pendente.
