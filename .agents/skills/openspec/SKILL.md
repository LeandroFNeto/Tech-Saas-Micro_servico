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

## Pesquisa no código

Para achar arquivo, símbolo ou dependência, use o Graphify antes de ler ou buscar diretamente: `graphify query "<pergunta>"`, `graphify path "<A>" "<B>"`, `graphify explain "<conceito>"`. Leia direto só depois de o grafo indicar o arquivo, ou se `graphify-out/graph.json` não existir. Ao fim, se criou ou alterou arquivo, rode `graphify update .` uma vez. O prompt de cada agente que a skill abre leva essa regra. Regra do projeto: `.agents/rules/graphify.md`.

## Quando agir

1. Funcionalidade nova ou tela nova → modo **propor**. Pare. Não escreva YAML, Java nem Angular.
2. Usuário aprova a pasta `openspec/changes/<nome>/` ("aprovado", "pode implementar", "pode codar") → modo **aplicar**.
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

<Comportamento observável do sistema em terceira pessoa.>

Contrato: <rota OpenAPI em especificacao/back/, ou "nenhum">
Prova: <cenário de feature Cucumber ou classe de teste JUnit>

Cenário: <título>

- Dado <contexto>
- Quando <evento>
- Então <resultado esperado>
```

Depois de implementar e testar:

1. Atualize o `spec.md` do domínio.
2. Atualize `openspec/specs/README.md` se o domínio for novo.
3. Se tudo passou e foi revisado, mova a pasta para `openspec/archive/<nome>/`.
4. Atualize o painel: `python -I docs/gerar-painel-projeto.py`.
5. Se `graphify-out/graph.json` existir, execute `graphify update .`.

## Modo documentar

Use quando um comportamento já está implementado e coberto por testes, mas ainda não está no spec. Não abra pasta em `changes/`. Escreva o requisito direto no `spec.md` do domínio, aponte o teste que já passa como prova e registre no topo o que foi documentado.
