# Contratos de comportamento

Esta pasta é a fonte da verdade do que o sistema faz. Cada domínio fica numa subpasta com `spec.md`: requisito, cenário e ponteiro para o contrato. O arquivo não copia DTO, campo de tela nem estilo.

| Domínio | Spec | Prova principal |
|---|---|---|
| Stack e fronteiras | `00-architecture.md` | — |
| Login e senha | `auth/spec.md` | `src/test/resources/features/auth.feature`, `apresentacao.component.spec.ts`, `JwtAuthenticationFilterTest`, `whatsapp.feature` |
| Cadastro e perfil | `empresa/spec.md` | `src/test/resources/features/empresas.feature`, `ControllerEmpresaTest`, `perfil.component.spec.ts`, `empresa-form.component.spec.ts`, `CalculadoraRiscoDesmarcacaoTest`, `ServicoPrevisaoTempoTest`, `painel.component.spec.ts`, `previsao.service.spec.ts`, `ControllerFuncionamentoTest`, `ServicoFuncionamentoTest`, `CalendarioFeriadosTest`, `funcionamento.component.spec.ts`, `automacao.component.spec.ts`, `automacao.service.spec.ts` |
| Webhook, sessão e bot | `whatsapp/spec.md` | `webhook.feature`, `roteamento-strategy.feature`, `LocacaoStrategyTest`, `MascaraLogTest`, `ObservadorwhatsappTest`, `ObservadorGoogleAgendaTest`, `WhatsappWebhookDTORaioXTest`, `ServicoWppConnectLogTest`, `LocacaoStrategyLogTest`, `ServicoRespostaLembreteTest`, `InterpretadorPorPalavrasTest`, `ServicoLembreteReservaTest` |
| Google Agenda | `agenda/spec.md` | `observer-agenda.feature`, `ServicoReservaAgendarTest`, `ObservadorGoogleAgendaTest`, `ObservadorGoogleAgendaPorHoraTest`, `ServicoGoogleagendaSobreposicaoTest`, `GerenciadorSessaoTest`, `ServicoSincronizacaoAgendaTest`, `CriptografiaCampoTest` |
| Comunicados do painel | `avisos/spec.md` | `ControllerAvisoTest` |
| Consulta em grafo para a IA | `ia/spec.md` | `ConsultaEmpresaGrafoTest`, `ia-query.feature` |
| Equipe de desenvolvimento | `desenvolvimento/spec.md` | teste automatizado ainda não existe |

## O que cada dono guarda

| Dono | Pasta | Guarda |
|---|---|---|
| Comportamento | `openspec/specs/` | Requisito e cenário observável |
| HTTP | `especificacao/back/` | Rota, status e DTO |
| Tela | `especificacao/front/` | Texto e design |
| Prova | `src/test/resources/features/` ou teste JUnit citado | O cenário executável |

Cada requisito cita `Contrato:` e `Prova:`. Se a prova ainda não existe, o requisito diz isso em vez de inventar um teste.

O perfil que o cliente edita é `EMP-02` em `empresa/spec.md`, com o texto em `especificacao/front/cliente-perfil.content.json`. A reserva automática pelo bot cruza três domínios: quem liga fica em `EMP-02`, a conversa até o pedido em `WPP-07` (`whatsapp/spec.md`) e o registro na agenda em `AGD-02` (`agenda/spec.md`). A reserva por hora cruza os mesmos três domínios: o funcionamento e os dias especiais ficam em `EMP-07` e `EMP-08`, a conversa em `WPP-08` e `WPP-09`, e o registro e a disponibilidade em `AGD-03` e `AGD-04`. Os lembretes de confirmação cruzam: configuração e tela em `EMP-09`, `EMP-10` e `EMP-11`, disparo e interpretação em `WPP-10` e `WPP-11`, sincronização e persistência de reservas em `AGD-05` e `AGD-06`. Os arquivos `especificacao/back/paths/profile.yaml` e `profile-empresas.yaml` são descritores Spring Data REST (ALPS). Eles não formam um domínio desta pasta.

## Feature nova

Não começa nesta pasta e não gera código direto. Passa por `openspec/changes/<nome>/`, nesta ordem:

1. `proposal.md` — por que a mudança existe e o que muda (entra, altera, sai).
2. `design.md` — como implementar, alinhado a `00-architecture.md` e ao spec do domínio.
3. `tasks.md` — contrato, código, prova, atualização deste spec e arquivamento.

Só depois de aplicada a mudança o requisito entra no `spec.md` do domínio. O histórico vai para `openspec/archive/`.

Correção que só restaura um cenário já escrito aqui não abre pasta em `changes/`.

Como escrever a change e como fundir o requisito de volta neste índice: `.cursor/skills/openspec/SKILL.md`.
