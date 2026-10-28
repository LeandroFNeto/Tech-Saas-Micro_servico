# Central de tarefas

Arquivo em comum da equipe. Cada agente atualiza a própria linha ao começar e ao terminar, e encerra. A espera fica neste arquivo.

| Tarefa | Etapa | IA | Local | Voltas | Estado |
|---|---|---|---|---|---|
| [definir-papeis-ia-desenvolvimento](archive/definir-papeis-ia-desenvolvimento/) | revisão | `claude-opus-5-5-medium` | `openspec/specs/desenvolvimento/spec.md` | 0 | aprovado |
| [recusar-data-passada-conversa](archive/recusar-data-passada-conversa/) | deploy | `cursor-grok-4.6-low` | `openspec/archive/recusar-data-passada-conversa/` | 0 | aprovado |
| [aplicar-novo-design-front](archive/aplicar-novo-design-front/) | deploy | `claude-haiku-5-5` | `openspec/archive/aplicar-novo-design-front/` | 0 | aprovado |
| [proteger-dados-nos-logs](archive/proteger-dados-nos-logs/) | deploy | `claude-haiku-5-5` | `openspec/archive/proteger-dados-nos-logs/` | 2 | aprovado |

Estado: `em andamento`, `aprovado`, `reprovado`, `erro`, `aguardando aprovado`.

Voltas: no máximo 2. Na terceira reprovação a equipe para.

A programação começa quando a pessoa troca o estado desta linha para `aprovado`.

Erro de teste: `openspec/changes/<tarefa>/teste-erro.md`. Sem esse arquivo, a regressão dessa tarefa passou.
