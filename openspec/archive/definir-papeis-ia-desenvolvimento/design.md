# Design

## Decisão

`horadoshow` não implementa sozinha. Ela entrega a especificação da tarefa para seis agentes, nesta ordem:

1. **Programação**, `claude-sonnet-5-5-high`. Lê `.cursor/skills/horadoshow/acopladas.md`. Se o assunto tiver linha, abre esse `SKILL.md` e cumpre `tasks.md`. Sem linha, segue só o pacote da change. Se a API muda, o YAML em `especificacao/back/` vem antes do Java.
2. **Testes**, `gemini-4-argon-medium`. Escreve a prova citada e roda `mvn test`. A regressão que já passava tem de continuar passando. Falha gera `openspec/changes/<nome>/teste-erro.md` e estado `erro` na central. Sucesso apaga esse arquivo.
3. **Qualidade**, `gpt-5.6-sol-medium`. Confere o conjunto: requisito, contrato, código e prova. Se reprovar, devolve para a programação e soma uma volta. Não libera a segurança.
4. **Segurança**, `claude-opus-5-5-medium`. Roda o subagente `security-review` quando a tarefa mexe em rota, Java, YAML de API, autenticação, senha, token, webhook ou dado de cliente. O achado vale só para os arquivos citados em `design.md`. Tela de texto, layout e estilo marca `aprovado` com a nota `tela` e não chama o subagente.
5. **Revisão**, `claude-opus-5-5-medium`. Lê o que a programação, os testes e a qualidade entregaram. Se passar, grava o requisito em `openspec/specs/<dominio>/spec.md` e a linha em `openspec/specs/README.md`. Essa é a spec definitiva. Se reprovar, devolve o que falhou para a programação. Não chama o deploy.
6. **Deploy**, `cursor-grok-4.6-low`. Só encerra, com a spec já gravada e com segurança e revisão aprovadas. Move a pasta para `openspec/archive/<nome>/`. Sobe o front (`frontend/` e `vercel.json`) na branch `producao`, que a Vercel publica. Sobe o código completo na branch `main`, que segue para a Hostinger. Não escreve o requisito e não altera código de produto.

No máximo 2 voltas. A terceira reprovação deixa a central em `reprovado` e a equipe para.

Pesquisa (`cursor-grok-4.6-low`) e proposta (`grok-4.7-high-fast`) acontecem antes desse chamado. Elas produzem a pasta que a `horadoshow` entrega. Cada etapa usa só o modelo da tabela. Na volta, os mesmos modelos repetem o próprio papel.

A pessoa chama `horadoshow <nome>`, com o nome em kebab-case. Sem o “aprovado” da pasta, a programação não começa.

## Arquivos

- Contrato: nenhum. Nenhuma rota em `especificacao/back/`.
- Java: nenhum.
- Front: nenhum. Sem `*.content.json` nem em `telas.design.json`.
- Regra: `.cursor/rules/papeis-ia.mdc` (papéis, ordem e a parada entre agentes).
- Skill: `.cursor/skills/horadoshow/SKILL.md`. Manda a especificação para a equipe.
- Lista: `.cursor/skills/horadoshow/acopladas.md`. Uma linha por skill definida.
- Central: `openspec/central-tarefas.md`. Erro de teste: `openspec/changes/<nome>/teste-erro.md`.
- Spec: `openspec/specs/desenvolvimento/spec.md` e uma linha em `openspec/specs/README.md`.
- Prova: cenário no spec para a ordem programação → testes → qualidade → segurança → revisão → deploy, com a volta da segurança ou da revisão para a programação. Teste automatizado ainda não existe: o comportamento é regra de agente, não da API.

## Alinhamento

- Stack de `00-architecture.md`: sem código de produto nesta change.
- DTO nomeado; sem entidade JPA e sem `Map` no payload: não há payload.
- YAML em `especificacao/back/` antes do Java: a programação da equipe segue essa ordem quando a tarefa tiver API.

## Riscos

- Uma etapa roda no modelo de outra. A skill nomeia o modelo e a regra recusa a troca.
- A revisão ou a segurança reprova e o deploy começa mesmo assim. A regra devolve o trabalho para a programação.
- A segurança aplica um patch sozinha. Ela só devolve o achado; quem corrige é a programação.
- O deploy grava a spec ou sobe no GitHub com segurança ou revisão pendente. Quem grava a spec é a revisão. O deploy só arquiva e faz push.
- O deploy barato altera código de produto. O papel dele é arquivar, commitar e fazer push.
- A volta passa de 2. A terceira reprovação para e chama a pessoa.
- A API entra na branch `producao`. Essa branch fica só com o front, para a Vercel. O código completo vai para `main`, para a Hostinger.
- Dois agentes editam o mesmo arquivo. Cada um recebe a spec e devolve o próprio papel; o próximo lê o resultado do anterior.
