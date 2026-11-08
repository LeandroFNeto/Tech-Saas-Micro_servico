# Central de tarefas

Arquivo em comum da equipe. Cada agente atualiza a própria linha ao começar e ao terminar, e encerra. A espera fica neste arquivo.

| Tarefa | Etapa | IA | Local | Voltas | Estado |
|---|---|---|---|---|---|
| [definir-papeis-ia-desenvolvimento](archive/definir-papeis-ia-desenvolvimento/) | deploy | `claude-opus-5-5-medium` | `openspec/specs/desenvolvimento/spec.md` | 0 | aprovado |
| [recusar-data-passada-conversa](archive/recusar-data-passada-conversa/) | deploy | `cursor-grok-4.6-low` | `openspec/archive/recusar-data-passada-conversa/` | 0 | aprovado |
| [aplicar-novo-design-front](archive/aplicar-novo-design-front/) | deploy | `claude-haiku-5-5` | `openspec/archive/aplicar-novo-design-front/` | 0 | aprovado |
| [proteger-dados-nos-logs](archive/proteger-dados-nos-logs/) | deploy | `claude-haiku-5-5` | `openspec/archive/proteger-dados-nos-logs/` | 2 | aprovado |
| [exibir-previsao-tempo-reservas](archive/exibir-previsao-tempo-reservas/) | deploy | `claude-opus-5-5` | `openspec/archive/exibir-previsao-tempo-reservas/` | 3 | aprovado |
| [corrigir-reserva-automatica](archive/corrigir-reserva-automatica/) | deploy | `claude-haiku-5-5` | `openspec/archive/corrigir-reserva-automatica/` | 1 | aprovado |
| [remover-x-cliente-token](archive/remover-x-cliente-token/) | deploy | `claude-haiku-5-5` | `openspec/archive/remover-x-cliente-token/` | 0 | aprovado |
| [lembrar-confirmacao-reserva](archive/lembrar-confirmacao-reserva/) | deploy | `claude-haiku-5-5` | `openspec/archive/lembrar-confirmacao-reserva/` | 0 | aprovado |
| [aprimorar-painel-admin](archive/aprimorar-painel-admin/) | deploy | `antigravity` | `openspec/archive/aprimorar-painel-admin/` | 0 | aprovado |
| [fechar-pendencias-reserva-por-hora](archive/fechar-pendencias-reserva-por-hora/) | deploy | `gemini-3.8-flash` | `openspec/archive/fechar-pendencias-reserva-por-hora/` | 0 | aprovado |
| [definir-niveis-admin](archive/definir-niveis-admin/) | deploy | `antigravity` | `openspec/archive/definir-niveis-admin/` | 0 | aprovado |
| [trava-proprietario-mensagens-antigas](archive/trava-proprietario-mensagens-antigas/) | deploy | `antigravity` | `openspec/archive/trava-proprietario-mensagens-antigas/` | 0 | aprovado |
| [modulo-ia-conversa-humanizada](archive/modulo-ia-conversa-humanizada/) | deploy | `antigravity` | `openspec/archive/modulo-ia-conversa-humanizada/` | 0 | aprovado |
| [modulo-assistente-tutoria-testes](archive/modulo-assistente-tutoria-testes/) | deploy | `antigravity` | `openspec/archive/modulo-assistente-tutoria-testes/` | 0 | aprovado |
| [assistente-cliente-sandbox-modulos](archive/assistente-cliente-sandbox-modulos/) | deploy | `antigravity` | `openspec/archive/assistente-cliente-sandbox-modulos/` | 0 | aprovado |
| [bot-whatsapp-gamb-suporte-teste](archive/bot-whatsapp-gamb-suporte-teste/) | deploy | `claude-haiku-5-5` | `openspec/archive/bot-whatsapp-gamb-suporte-teste/` | 1 | aprovado |
| [ajustar-edicao-empresa-recursos-e-exclusao](archive/ajustar-edicao-empresa-recursos-e-exclusao/) | deploy | `antigravity` | `openspec/archive/ajustar-edicao-empresa-recursos-e-exclusao/` | 0 | aprovado |
| [ia-cliente-refletir-base-de-conhecimento](archive/ia-cliente-refletir-base-de-conhecimento/) | deploy | `antigravity` | `openspec/archive/ia-cliente-refletir-base-de-conhecimento/` | 0 | aprovado |
| [autenticar-webhook-whatsapp](archive/autenticar-webhook-whatsapp/) | deploy | `antigravity` | `openspec/archive/autenticar-webhook-whatsapp/` | 0 | aprovado |
| [ajustes-visuais-chat-flutuante-tutoriais](archive/ajustes-visuais-chat-flutuante-tutoriais/) | deploy | `antigravity` | `openspec/archive/ajustes-visuais-chat-flutuante-tutoriais/` | 0 | aprovado |
| [apresentacao-posicionamento-e-contato](archive/apresentacao-posicionamento-e-contato/) | deploy | `antigravity` | `openspec/archive/apresentacao-posicionamento-e-contato/` | 0 | aprovado |
| [bot-comercial-site-login-apresentacao](archive/bot-comercial-site-login-apresentacao/) | deploy | `antigravity` | `openspec/archive/bot-comercial-site-login-apresentacao/` | 0 | aprovado |
| [gamb-whatsapp-datas-livres-e-tom-humano](archive/gamb-whatsapp-datas-livres-e-tom-humano/) | deploy | `antigravity` | `openspec/archive/gamb-whatsapp-datas-livres-e-tom-humano/` | 0 | aprovado |
| [migrar-wppconnect-para-baileys](changes/migrar-wppconnect-para-baileys/) | programacao | `gemini-3.8-flash` | `openspec/changes/migrar-wppconnect-para-baileys/` | 0 | aprovado |
| [fechar-pendencias-de-seguranca](changes/fechar-pendencias-de-seguranca/) | revisao | `claude-opus-5-5` | `openspec/changes/fechar-pendencias-de-seguranca/` | 2 | reprovado |

Estado: `em andamento`, `aprovado`, `reprovado`, `erro`, `aguardando aprovado`.

Voltas: no máximo 2. Na terceira reprovação a equipe para.

A programação começa quando a pessoa troca o estado desta linha para `aprovado`.

Erro de teste: `openspec/changes/<tarefa>/teste-erro.md`. Sem esse arquivo, a regressão dessa tarefa passou.
