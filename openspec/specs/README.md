# Contratos de comportamento

Esta pasta é a fonte da verdade do que o sistema faz. Cada domínio fica numa subpasta com `spec.md`: requisito, cenário e ponteiro para o contrato. O arquivo não copia DTO, campo de tela nem estilo.

| Domínio | Spec | Prova principal |
|---|---|---|
| Stack e fronteiras | `00-architecture.md` | — |
| Login e senha | `auth/spec.md` | `src/test/resources/features/auth.feature` |
| Cadastro e perfil | `empresa/spec.md` | `src/test/resources/features/empresas.feature` |
| Webhook, sessão e bot | `whatsapp/spec.md` | `webhook.feature`, `roteamento-strategy.feature`, `LocacaoStrategyTest`, `MascaraLogTest`, `ObservadorwhatsappTest`, `ObservadorGoogleAgendaTest`, `WhatsappWebhookDTORaioXTest`, `ServicoWppConnectLogTest`, `LocacaoStrategyLogTest` |
| Google Agenda | `agenda/spec.md` | `observer-agenda.feature` |
| Comunicados do painel | `avisos/spec.md` | `ControllerAvisoTest` |
| Consulta em grafo para a IA | `ia/spec.md` | `ConsultaEmpresaGrafoTest`, `ia-query.feature` |

## O que cada dono guarda

| Dono | Pasta | Guarda |
|---|---|---|
| Comportamento | `openspec/specs/` | Requisito e cenário observável |
| HTTP | `especificacao/back/` | Rota, status e DTO |
| Tela | `especificacao/front/` | Texto e design |
| Prova | `src/test/resources/features/` ou teste JUnit citado | O cenário executável |

Cada requisito cita `Contrato:` e `Prova:`. Se a prova ainda não existe, o requisito diz isso em vez de inventar um teste.

O perfil que o cliente edita é `EMP-02` em `empresa/spec.md`, com o texto em `especificacao/front/cliente-perfil.content.json`. Os arquivos `especificacao/back/paths/profile.yaml` e `profile-empresas.yaml` são descritores Spring Data REST (ALPS). Eles não formam um domínio desta pasta.

## Feature nova

Não começa nesta pasta e não gera código direto. Passa por `openspec/changes/<nome>/`, nesta ordem:

1. `proposal.md` — por que a mudança existe e o que muda (entra, altera, sai).
2. `design.md` — como implementar, alinhado a `00-architecture.md` e ao spec do domínio.
3. `tasks.md` — contrato, código, prova, atualização deste spec e arquivamento.

Só depois de aplicada a mudança o requisito entra no `spec.md` do domínio. O histórico vai para `openspec/archive/`.

Correção que só restaura um cenário já escrito aqui não abre pasta em `changes/`.

Como escrever a change e como fundir o requisito de volta neste índice: `.cursor/skills/openspec/SKILL.md`.
