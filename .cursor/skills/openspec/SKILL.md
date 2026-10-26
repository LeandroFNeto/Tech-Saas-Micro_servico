---
name: openspec
description: >-
  Planeja e aplica mudanças no OpenSpec deste repositório (openspec/specs e
  openspec/changes). Use quando o pedido for funcionalidade nova, tela nova,
  change, proposta, spec de domínio, ou quando o usuário aprovar uma pasta
  em openspec/changes para implementar.
---

# OpenSpec

Leia `openspec/specs/README.md` e o `spec.md` do domínio antes de escrever. A stack está em `openspec/specs/00-architecture.md`.

Três donos, sem copiar conteúdo entre eles:

| Dono | Pasta |
|---|---|
| Comportamento | `openspec/specs/<dominio>/spec.md` |
| HTTP | `especificacao/back/` |
| Tela | `especificacao/front/` |

## Quando agir

1. Funcionalidade nova ou tela nova → mode **propor**. Pare. Não escreva YAML, Java nem Angular.
2. Usuário aprova a pasta `openspec/changes/<nome>/` (“aprovado”, “pode implementar”, “pode codar”) → modo **aplicar**.
3. Pedido para documentar comportamento que já existe e já tem prova → modo **documentar**. Não abra `changes/`.
4. Bug que só restaura um cenário já escrito no spec, pergunta ou ajuste que não muda comportamento → não use esta skill.

Nome da change: kebab-case, verbo e domínio, em português. Exemplo: `criar-aviso-admin`.

## Modo propor

Crie só estes três arquivos.

`openspec/changes/<nome>/proposal.md`

```markdown
# <nome>

## Problema

## O que muda
### Entra
### Altera
### Sai

## Fora de escopo

## Domínios
- spec: `openspec/specs/<dominio>/spec.md`
```

`openspec/changes/<nome>/design.md`

```markdown
# Design

## Decisão

## Arquivos
- Contrato:
- Java:
- Front:
- Prova:

## Alinhamento
- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`

## Riscos
```

`openspec/changes/<nome>/tasks.md`

```markdown
# Tarefas

- [ ] Contrato YAML, se a API muda, e `$ref` em `especificacao/back/swagger.yaml`
- [ ] Java cumpre o contrato
- [ ] Front: `*.content.json` e `telas.design.json`, se a tela muda
- [ ] Prova: cenário Cucumber ou teste JUnit citado no requisito
- [ ] Atualizar `openspec/specs/<dominio>/spec.md` com o requisito novo
- [ ] Mover esta pasta para `openspec/archive/<nome>/`
```

Preencha com o comportamento pedido. Aponte rotas e DTOs que já existem; não cole o schema. Termine pedindo revisão. Não implemente na mesma resposta.

## Modo aplicar

Siga `tasks.md` na ordem. Se Java e YAML divergirem, corrija o Java.

Ao atualizar o spec, use o requisito neste formato. O prefixo continua o do domínio (`AUTH`, `EMP`, `WPP`, `AGD`, `AVI`, `IA`). Pegue o próximo número livre.

```markdown
### <PREFIXO>-<NN> <título>

O sistema deve <comportamento observável>.

Contrato: `especificacao/back/paths/<arquivo>.yaml`
Prova: `src/test/resources/features/<arquivo>.feature`

Cenário: <nome>
- Dado ...
- Quando ...
- Então ...
```

Se a prova ainda não existir, escreva isso na linha `Prova:` em vez de citar um teste que não há.

Depois de a prova passar, mova `openspec/changes/<nome>/` para `openspec/archive/<nome>/`.

## Modo documentar

Escreva ou complete `openspec/specs/<dominio>/spec.md` só com comportamento que o contrato e um teste já mostram. Atualize a tabela em `openspec/specs/README.md`. Não abra `changes/` e não altere Java nem YAML para “fazer o spec ficar verdade”.
