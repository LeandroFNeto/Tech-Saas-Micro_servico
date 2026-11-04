# Design

## Context

O webhook localiza a empresa pela sessão do WhatsApp e entrega o texto à strategy do `ramoDeAtuacao` por `FactoryModulo` (WPP-01). `AssistenteTutoriaService` já tem o modo Suporte e o modo `TESTE_VIRTUAL`. A change `ajustes-visuais-chat-flutuante-tutoriais` torna o Suporte independente de dados de empresa e o publica por endpoint. O registro na Google Agenda já existe em `ServicoReserva` e `ServicoGoogleagenda` para as empresas. Motivação em proposal.md.

## Goals / Non-Goals

**Goals:**
- Reaproveitar o bot de Suporte no número de WhatsApp da Gamb sem duplicar prompt nem escopo.
- Deixar o cliente marcar reunião com a equipe pelo WhatsApp, com os responsáveis convidados por e-mail.
- Dar aos módulos novos um bot de teste em WhatsApp real, restrito a quem a Gamb autoriza.

**Non-Goals:**
- Não mudar o roteamento das empresas clientes nem as strategies existentes.
- Não separar cliente de prospect: todo contato no número da Gamb é tratado como cliente.
- Não criar armazenamento novo além do campo de e-mails: o registro de módulos fica no código.
- Não expor o bot de teste a clientes.

## Decisions

- **Duas empresas internas da Gamb, uma por sessão**: o roteamento atual parte da empresa. A Gamb cadastra duas empresas internas, com ramos reservados `ATENDENTE_GAMB` e `TESTE_MODULOS`, e `FactoryModulo` entrega cada ramo à sua strategy. Alternativa: um roteamento paralelo ao das empresas, descartada por duplicar o webhook.
- **`AtendenteGambStrategy` com dois caminhos**: menu inicial com "Tirar uma dúvida" e "Marcar reunião". Dúvida chama o método de Suporte do `AssistenteTutoriaService`, com o mesmo prompt e escopo do painel; a strategy não monta o prompt e não lê dado de empresa. Reunião segue o fluxo abaixo. O estado da conversa fica no `GerenciadorSessao`, como nas empresas.
- **Fluxo de reunião**: motivo (`AJUSTE` ou `NOVA_INOVACAO`), data, horário, confirmação do resumo. O evento é criado na Google Agenda da empresa interna `ATENDENTE_GAMB` pelo `ServicoReserva`, com o motivo no título e o número do cliente mascarado na descrição. Horário já ocupado na agenda volta ao cliente com outras opções, pela disponibilidade que `AGD-04` já calcula.
- **Responsáveis da reunião**: campo `emailsResponsaveisReuniao` na empresa interna, uma lista de e-mails editada no painel admin. Cada reunião criada convida todos os e-mails da lista no momento da criação. Mudar a lista nunca cancela reunião: as reuniões futuras já marcadas só têm os convidados atualizados (entram os e-mails novos, saem os removidos) e o evento, a data e o horário ficam como estão. Lista vazia: o bot não oferece reunião, avisa que o agendamento está indisponível e mantém a dúvida funcionando. E-mail inválido é rejeitado na edição.
- **Reunião confirmada no ato**: horário livre vira evento criado e convite enviado na hora. O bot responde "reunião marcada" com data e horário e não promete nada além disso.
- **`TesteModulosStrategy` com lista de números autorizados**: lê os números de uma configuração do admin. Número fora da lista recebe uma resposta fixa e nenhum módulo roda. Alternativa: sem lista, descartada porque o bot executa módulos ainda sem revisão.
- **Registro de módulos experimentais**: interface `ModuloExperimental` com nome, descrição e resposta, e um registro que o Spring monta com os beans. O comando `/modulos` lista e `/modulo <nome> <texto>` executa. O registro vive em memória.
- **Simulador do painel**: a lógica de `responderTesteVirtual` fica no serviço e vira o primeiro módulo experimental. O endpoint autenticado continua aceitando o modo.
- **Limite diário**: contador por número na sessão da atendente; ao estourar, resposta fixa e nenhuma chamada à IA.
- **Segredos e logs**: o número do contato e o conteúdo das mensagens passam pela mesma máscara de log do webhook (`MascaraLog`). O e-mail dos responsáveis não aparece em log.

## Risks / Trade-offs

- [Pergunta de cliente sobre os próprios dados no número da Gamb] → o Suporte não tem dado de empresa; a resposta orienta a usar o painel.
- [Dois clientes disputando o mesmo horário] → a disponibilidade da Google Agenda é conferida antes de criar o evento.
- [Google Agenda da Gamb desconectada] → o bot informa que o agendamento está indisponível e não cria nada.
- [Reunião marcada automaticamente por qualquer número] → o limite diário e a exigência de resumo confirmado reduzem abuso; reuniões ficam visíveis na agenda para a equipe cancelar.
- [Webhook sem autenticação (anterior a esta change): `from` e `session` forjados contornam a lista de autorizados e o limite diário] → aceito aqui e tratado na change `autenticar-webhook-whatsapp`.
- [Sem teto de reuniões por número: um número real fecha até cerca de 6 por dia] → aceito; a equipe cancela na agenda.
- [Módulo experimental com efeito colateral] → o contrato do módulo exige resposta simulada e proíbe escrita em dados de empresas reais.
- [Lista de autorizados vazia] → o bot de teste responde que não há número autorizado e nada executa.
- [Dependência da outra change] → só aplicar depois que o Suporte sem dado de empresa estiver implementado.
