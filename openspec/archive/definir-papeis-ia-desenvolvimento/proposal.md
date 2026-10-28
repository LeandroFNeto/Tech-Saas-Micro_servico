# definir-papeis-ia-desenvolvimento

## Problema

Pesquisa, proposta, programação, teste, qualidade e deploy passam pelo mesmo agente. O deploy publica a spec sem uma revisão capaz do que a programação entregou e sem uma passagem de segurança no diff. Não há volta para corrigir antes da spec ficar definitiva.

## O que muda
### Entra

Antes da equipe, dois papéis no nível econômico:

| Papel | Modelo | Faz | Para aí |
|---|---|---|---|
| Pesquisa | `cursor-grok-4.6-low` | explica código, spec e contrato | não cria change, não edita produto, não faz git |
| Proposta | `grok-4.7-high-fast` | escreve `proposal.md`, `design.md` e `tasks.md` | não escreve YAML, Java nem Angular |

`horadoshow <nome-da-tarefa>` manda essa especificação para a equipe trabalhar em conjunto. O nome abre `openspec/changes/<nome>/`. Um agente por papel. O próximo só começa se o anterior passou.

| Ordem | Agente | Modelo | Faz | Para aí |
|---|---|---|---|---|
| 1 | Programação | `claude-sonnet-5-5-high` | cumpre `tasks.md`; YAML antes do Java, se a API mudar | não publica o spec |
| 2 | Testes | `gemini-4-argon-medium` | escreve a prova, roda `mvn test` e grava `teste-erro.md` se a regressão quebrar | não publica o spec; qualidade não começa com erro |
| 3 | Qualidade | `gpt-5.6-sol-medium` | confere spec, contrato, código e prova juntos | se reprovar, volta para a programação e soma uma volta; a segurança não começa |
| 4 | Segurança | `claude-opus-5-5-medium` | procura vazamento de dados e acesso indevido quando a tarefa mexe em rota, autenticação ou dado | tela de texto, layout e estilo não dispara essa passagem; falha devolve para a programação |
| 5 | Revisão | `claude-opus-5-5-medium` | lê a entrega e, se passar, grava a spec definitiva | se reprovar, devolve para a programação; o deploy não começa |
| 6 | Deploy | `cursor-grok-4.6-low` | arquiva e sobe no GitHub | não escreve a spec; `producao` recebe o front e a Vercel atualiza; `main` recebe o código completo e segue para a Hostinger |

No máximo 2 voltas. Cada devolução repete programação, testes e qualidade. A terceira reprovação para e chama a pessoa. O deploy só começa com segurança e revisão aprovadas e com a spec já gravada pela revisão.

O quadro em comum é `openspec/central-tarefas.md`. Cada etapa abre o próprio agente, no modelo da tabela, atualiza a linha e publica só esse quadro na `main`. A pessoa grava `aprovado` nessa linha para a programação começar. O arquivo de erro do teste é `openspec/changes/<nome>/teste-erro.md`.

A lista viva é `.cursor/skills/horadoshow/acopladas.md`. A programação abre a skill da linha cujo assunto bate com a tarefa. Skill nova entra como uma linha nessa lista. Sem linha, segue só o pacote da change.

O domínio novo é `openspec/specs/desenvolvimento/spec.md`, prefixo `DEV`. A consulta GraphQL em `openspec/specs/ia/spec.md` permanece o que já é.

### Altera

- A skill OpenSpec continua dona do modo propor, aplicar e documentar.
- O “aprovado” humano da pasta `openspec/changes/` continua obrigatório antes da programação.
- Quem grava a spec definitiva é a revisão, em `claude-opus-5-5-medium`. O deploy no modelo mais barato só arquiva e sobe no GitHub, depois dessa gravação.
- A branch `producao` guarda o front (`frontend/` e `vercel.json`) e a Vercel publica essa branch. A branch `main` guarda o código completo e é o que sobe na Hostinger.

### Sai

- Nada de rota, DTO ou tela.

## Fora de escopo

- `POST /api/ia-query` e os requisitos `IA-01`, `IA-02` e `IA-03`
- Copiar o slug do modelo para `openspec/specs/`; o spec cita o papel e a skill guarda o modelo
- Push fora do passo de deploy da `horadoshow`
- Pular a aprovação da pasta `openspec/changes/`
- Um agente acumular o papel do seguinte
- O plugin `/claude-security` do Claude Code: ele não roda nesta sessão do Cursor. A passagem daqui é o subagente `security-review`

## Domínios

- spec: `openspec/specs/desenvolvimento/spec.md` (arquivo novo na aplicação)
- spec: `openspec/specs/ia/spec.md` (não muda)
