# Desenvolvimento

Comportamento observável da equipe que implementa uma change. O procedimento está em `.cursor/skills/horadoshow/SKILL.md`. A consulta GraphQL da IA do produto continua em `ia/spec.md`.

## Requisitos

### DEV-01 Pesquisa e proposta antes da equipe

O sistema deve explicar código, spec e contrato na pesquisa, no modelo da etapa, sem criar change, sem editar produto e sem git. A proposta, no modelo da etapa, escreve só `proposal.md`, `design.md` e `tasks.md`.

Contrato: nenhum. Sem rota em `especificacao/back/`.
Prova: teste automatizado ainda não existe. O comportamento é regra de agente.

Cenário: pesquisa não grava a change

- Quando a pessoa pede pesquisa
- Então a resposta explica o código e o spec
- E nenhum arquivo de `openspec/changes/` é criado

Cenário: proposta só com os três arquivos

- Quando a pessoa pede a proposta de uma funcionalidade
- Então nascem `proposal.md`, `design.md` e `tasks.md`
- E não nascem YAML, Java nem Angular

### DEV-02 Programação só com aprovado na central

O sistema deve ler `openspec/central-tarefas.md`. A programação começa quando a pessoa grava `aprovado` na linha da tarefa. Um aprovado só no chat não libera.

Contrato: nenhum.
Prova: teste automatizado ainda não existe.

Cenário: central sem aprovado

- Dado a linha da tarefa em `aguardando aprovado`
- Quando chega `horadoshow` com o nome dessa tarefa
- Então a programação não começa

### DEV-03 Equipe em ordem, um agente por etapa

O sistema deve encaminhar `horadoshow <nome>` para `openspec/changes/<nome>/`, um agente por papel, no modelo da etapa. A ordem é programação, testes, qualidade, segurança, revisão e deploy, cada uma no modelo da coluna da ferramenta em uso (Cursor ou Claude), conforme `.cursor/rules/papeis-ia.mdc`. O próximo só começa com a etapa anterior `aprovado`. A sessão que recebeu o chamado não executa o papel. Quando a change mexe em elemento visual, a parte visual da programação roda no Opus da coluna da ferramenta em uso, conforme a skill de `acopladas.md`; o resto da programação mantém o modelo da etapa.

Contrato: nenhum.
Prova: teste automatizado ainda não existe.

Cenário: parte visual no Opus

- Dado uma change que altera tela, layout ou estilo do front
- Quando a programação começa
- Então a parte visual é feita no Opus
- E a lógica de tela segue no modelo de programação

Cenário: a revisão não começa com a qualidade reprovada

- Dado a qualidade reprovada
- Quando a equipe seguiria para a segurança
- Então a segurança não começa
- E a tarefa volta para a programação se ainda houver volta

### DEV-04 Regressão e arquivo de erro

O sistema deve, na etapa de testes, escrever a prova citada e rodar `mvn test`. A suíte que já passava tem de continuar passando. Tarefa que alterou `frontend/` também roda o teste do front. Falha grava `openspec/changes/<nome>/teste-erro.md`, marca a central como `erro` e não chama a qualidade. Sucesso apaga esse arquivo.

Contrato: nenhum.
Prova: teste automatizado ainda não existe.

Cenário: regressão quebrada

- Quando `mvn test` falha
- Então existe `teste-erro.md` com o comando e a falha
- E a qualidade não começa

### DEV-05 Segurança só com dado ou acesso

O sistema deve chamar `security-review` quando a tarefa mexe em rota, Java, YAML de API, autenticação, senha, token, webhook ou dado de cliente. O achado vale para os arquivos citados em `design.md`. Usuário, senha, token e `googleCalendarId` não podem sair. Tela de texto, layout e estilo marca `aprovado` com a nota `tela` e não chama o subagente. Vazamento ou acesso indevido volta para a programação e não chama a revisão.

Contrato: nenhum.
Prova: teste automatizado ainda não existe.

Cenário: tela sem subagente

- Dado uma tarefa só de texto e layout
- Quando a segurança lê os arquivos
- Então a central fica `aprovado` com a nota `tela`
- E o subagente não é chamado

### DEV-06 Revisão grava a spec e há no máximo duas voltas

O sistema deve, na revisão, no modelo da etapa, gravar o requisito em `openspec/specs/<dominio>/spec.md` e a linha em `openspec/specs/README.md` quando a entrega cumpre a change. Essa gravação é a spec definitiva. Reprovação de qualidade, segurança ou revisão soma 1 em Voltas e volta para programação, testes e qualidade. Na terceira reprovação a central fica `reprovado` e a equipe para.

Contrato: nenhum.
Prova: teste automatizado ainda não existe.

Cenário: duas voltas e pare

- Dado Voltas em 2
- Quando a revisão reprova de novo
- Então a central fica `reprovado`
- E o deploy não começa

### DEV-07 Deploy separa front e código completo

O sistema deve, no deploy, no modelo da etapa, só arquivar a pasta em `openspec/archive/<nome>/` e subir no GitHub depois da spec definitiva, da segurança e da revisão. `producao` recebe `frontend/` e `vercel.json` e a Vercel publica essa branch. `main` recebe o código completo e segue para a Hostinger. `producao` não recebe API, Docker nem `wppconnect-server/`. O deploy não escreve o requisito, não usa `git add` sem caminho e não usa `--force`.

Contrato: nenhum.
Prova: teste automatizado ainda não existe.

Cenário: front na branch da Vercel

- Dado segurança e revisão aprovadas e a spec já gravada
- Quando o deploy sobe o GitHub
- Então `producao` recebe o front
- E `main` recebe o código completo

### DEV-08 Skill do assunto na lista viva

O sistema deve ler `.cursor/skills/horadoshow/acopladas.md` antes da programação. Se o assunto tiver uma linha, a programação abre esse `SKILL.md`. Sem linha, segue só o pacote da change. Skill nova entra como uma linha nessa lista.

Contrato: nenhum.
Prova: teste automatizado ainda não existe.

Cenário: assunto sem skill

- Dado `acopladas.md` sem linha para o assunto
- Quando a programação começa
- Então ela segue `proposal.md`, `design.md` e `tasks.md`

### DEV-09 Skill e regra de modelo nas duas ferramentas

O sistema deve, para toda skill ou regra que escolhe modelo de IA por etapa, trazer uma coluna para o Cursor e outra para o Claude, no mesmo nível de potência. O agente usa a coluna da ferramenta em que está rodando e não mistura colunas. Os modelos vêm da tabela de `.cursor/rules/papeis-ia.mdc`; modelo novo entra nela primeiro. Skill nova ganha também uma linha em `.cursor/skills/horadoshow/acopladas.md`.

Contrato: nenhum.
Prova: teste automatizado ainda não existe. O comportamento é regra de agente.

Cenário: agente no Claude

- Dado uma skill com as colunas Cursor e Claude
- Quando o agente roda no Claude
- Então ele abre cada etapa como subagente ou sessão no modelo da coluna Claude
- E não usa nenhum modelo da coluna Cursor

Cenário: skill nova com uma coluna só

- Dado uma skill nova que escolhe modelo e traz só a coluna do Cursor
- Quando a qualidade confere a change
- Então a qualidade reprova até a coluna Claude existir

### DEV-10 Pesquisa no código pelo Graphify

O sistema deve, para achar arquivo, símbolo ou dependência, consultar o grafo do Graphify (`graphify query`, `path` ou `explain`) antes de Glob, Grep ou Read. O agente lê ou procura direto só depois de o grafo indicar o arquivo, ou quando `graphify-out/graph.json` não existe. Ao fim do turno ou da etapa em que criou ou alterou arquivo, o agente roda `graphify update .` uma vez. A regra vale para todo agente e subagente, e o prompt de cada um que uma skill abre leva essa regra. Toda skill traz a seção "Pesquisa no código". A pasta `graphify-out/` não entra em commit.

Contrato: nenhum.
Prova: teste automatizado ainda não existe. O comportamento é regra de agente (`.cursor/rules/graphify.mdc`, `.cursor/rules/graphify-update.mdc` e `.cursor/rules/papeis-ia.mdc`).

Cenário: agente procura um arquivo

- Dado `graphify-out/graph.json` existente
- Quando o agente precisa achar onde um símbolo é usado
- Então ele roda `graphify query` ou `graphify explain` primeiro
- E só depois lê as linhas que o grafo indicou

Cenário: agente altera arquivo

- Dado um agente que criou ou alterou arquivo na etapa
- Quando a etapa termina
- Então ele roda `graphify update .` uma vez
- E não inclui `graphify-out/` no commit

Cenário: skill abre um agente

- Dado uma skill que abre um agente ou subagente
- Quando o prompt do agente é escrito
- Então ele traz a regra de pesquisa pelo Graphify


### DEV-11 Modo Ponytail ativo por padrão na equipe

O sistema deve aplicar o modo Ponytail (lazy senior dev) imediatamente ao iniciar qualquer papel ou etapa da equipe (programação, testes, qualidade, segurança e revisão). A solução adotada deve ser a menor alteração funcional estritamente necessária que resolve a tarefa com segurança, priorizando o código já existente e a biblioteca padrão, recusando abstrações, wrappers ou código especulativo não solicitados.

Contrato: nenhum.
Prova: teste automatizado ainda não existe. O comportamento é regra de agente (`.agents/rules/papeis-ia.md`, `.agents/skills/horadoshow/SKILL.md` e `.agents/skills/horadoshow/acopladas.md`).

Cenário: subagente inicia etapa da equipe

- Dado qualquer subagente ou papel da equipe de desenvolvimento
- Quando o agente inicia a sua etapa
- Então ele já aplica a postura Ponytail
- E entrega o menor diff funcional sem complexidade desnecessária
