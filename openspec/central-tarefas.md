# Central de tarefas

Arquivo em comum da equipe. Cada agente atualiza a própria linha ao começar e ao terminar, e encerra. A espera fica neste arquivo.

| Tarefa | Etapa | IA | Local | Voltas | Estado |
|---|---|---|---|---|---|
| [definir-papeis-ia-desenvolvimento](archive/definir-papeis-ia-desenvolvimento/) | revisão | `claude-opus-5-5-medium` | `openspec/specs/desenvolvimento/spec.md` | 0 | aprovado |
| [recusar-data-passada-conversa](changes/recusar-data-passada-conversa/) | programação | `claude-sonnet-5-5-high` | `src/main/java/com/example/demo/strategy/LocacaoStrategy.java` | 0 | aprovado |

Estado: `em andamento`, `aprovado`, `reprovado`, `erro`, `aguardando aprovado`.

Voltas: no máximo 2. Na terceira reprovação a equipe para.

A programação começa quando a pessoa troca o estado desta linha para `aprovado`.

Erro de teste: `openspec/changes/<tarefa>/teste-erro.md`. Sem esse arquivo, a regressão dessa tarefa passou.
