# corrigir-reserva-automatica

## Problema

A reserva automática é uma opção, não um padrão: todo cliente nasce com ela desligada, porque às vezes o dono prefere fechar a reserva por conta própria. Por enquanto só o admin liga ou desliga, no ajuste da empresa, até os planos existirem e a liberação ficar automática por plano. Quem a liga espera que, ao dizer "sim" no fim da conversa, a reserva fique registrada na agenda. Hoje isso não acontece:

- Nenhum código publica `EventoReservaConfirmada`. O bot responde "Reserva confirmada" por conta própria e volta ao início.
- `ServicoReserva.agendarNoGoogle` só escreve no log. Mesmo que fosse chamado, não cria o evento.
- Se a agenda da empresa não está cadastrada, o método retorna sem erro, e o observador ainda avisaria "Reserva confirmada com sucesso".
- A disponibilidade é checada na pesquisa de data, não na confirmação. Duas pessoas podem confirmar o mesmo dia.
- Se o Google falhar, só o cliente final é avisado. O dono não fica sabendo.

O resultado é um cliente final convencido de que reservou, um espaço sem registro e um dono que descobre no dia. Quem não usa a reserva automática não é afetado: o bot só responde "um atendente vai te responder".

## O que muda

### Entra

- A confirmação final ("sim") publica `EventoReservaConfirmada` com data, horário, nome e o número original do cliente, e responde "Anotei seu pedido, estou registrando na agenda". Enquanto registra, a conversa fica no estado `RESERVA_REGISTRANDO` e outras mensagens recebem "Ainda estou registrando sua reserva".
- O evento passa a ser criado de verdade no Google Agenda do cliente: título "Reserva - <nome>", descrição com a origem (bot) e o WhatsApp do cliente, diária como dia inteiro. Reserva por hora fica na change `reserva-por-hora`; até lá, empresa com locação por hora segue como reserva manual.
- Antes de criar, o sistema consulta de novo a disponibilidade da data. Se alguém reservou no meio tempo, o cliente final recebe "essa data acabou de ser reservada" e a conversa volta a pedir outra data, sem perder o nome já informado.
- A mensagem de sucesso só sai depois que o Google confirma. Em falha temporária, o sistema tenta mais 2 vezes, sem duplicar o evento se a primeira tentativa tiver chegado ao Google, e, se continuar falhando, avisa o cliente final com honestidade: "não consegui registrar agora; o responsável vai confirmar com você".
- Reserva automática ligada sem agenda cadastrada deixa de ser silencioso: o bot trata como reserva manual desde o início da conversa ("um atendente vai te responder") e o log registra o motivo.
- A reserva automática sai sempre desativada em empresa nova (`false`), e o valor só muda pelo admin.
- O PUT do cliente deixa de alterar `permiteReservaAutomatica`: valor diferente do atual responde 400 ("A reserva automática é liberada pelo administrador.") e nada é gravado. A tela do perfil mostra a opção só para leitura, com o aviso de que o admin libera.
- O admin não consegue ligar a reserva automática sem agenda cadastrada, no cadastro nem na edição: o POST e o PUT do admin respondem 400 com "Cadastre a agenda do Google antes de ligar a reserva automática.". A agenda continua opcional no cadastro, para empresa que não a usa.

### Altera

- `EMP-02`: `permiteReservaAutomatica` nasce `false`, só o PUT do admin altera, e ligar exige agenda cadastrada.
- Futuro (fora desta change): quando os planos entrarem, o plano passa a liberar a opção sem o ajuste do admin.
- `AGD-01`: o observador deixa de assumir sucesso. O evento passa a carregar o horário.

### Sai

A resposta "Reserva confirmada" montada dentro de `LocacaoStrategy`.

## Fora de escopo

- Reserva por hora: sobreposição de horários, duração e evento com hora (change `reserva-por-hora`).
- Lembrete, sincronização da agenda, tabela de reservas e contatos (change `lembrar-confirmacao-reserva`, que vem depois).
- Aviso ao dono quando o registro falha. O painel mostra isso na change `lembrar-confirmacao-reserva` (situação `FALHA_AGENDA`). Até lá, só o log registra.
- Apagar ou mover eventos já criados.
- Sinal ou pagamento da reserva.
- Empresas com reserva manual: nada muda para elas.

## Domínios

- spec: `openspec/specs/agenda/spec.md` (AGD-02 registrar a reserva automática no Google com resultado real)
- spec: `openspec/specs/whatsapp/spec.md` (WPP-06 confirmar ao cliente só depois do registro)
- spec: `openspec/specs/empresa/spec.md` (EMP-02, regra da reserva automática)

Vem depois de `proteger-dados-nos-logs` e antes de `lembrar-confirmacao-reserva`. Também melhora `exibir-previsao-tempo-reservas`, porque as reservas do bot passam a existir na agenda.
