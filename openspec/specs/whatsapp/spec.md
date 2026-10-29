# WhatsApp

Comportamento observável do webhook, da conexão da sessão e do roteamento do bot. Mensagem filtrada também responde 200, para o WPPConnect não reenviar o evento.

## Requisitos

### WPP-01 Aceitar mensagem de chat e rotear pelo ramo

O sistema deve receber `WhatsappWebhookDTO`, localizar a empresa pela sessão e entregar o texto à strategy do `ramoDeAtuacao`. O ramo `LOCACAO`, `locacao` e `Locação` resolvem para `LocacaoStrategy`.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/resources/features/webhook.feature`, `src/test/resources/features/roteamento-strategy.feature`

Cenário: chat válido de locação

- Dado uma empresa de ramo `LOCACAO` na sessão informada
- Quando chega POST `/webhook/whatsapp` com `type=chat`, `fromMe=false` e texto
- Então a API responde 200
- E a factory resolve o ramo `LOCACAO`
- E a `LocacaoStrategy` processa a mensagem

Cenário: ramo sem strategy

- Dado uma empresa cujo ramo não está no mapa da factory (exemplo: `CLINICA`)
- Quando chega um chat válido nessa sessão
- Então a API responde 200
- E o observador registra erro contendo "Ramo de atuação não suportado"

### WPP-02 Filtrar ruído sem tratar como atendimento

O sistema deve responder 200 e não consultar a factory quando o evento não é um chat de cliente: mensagem do próprio dono (Anti-X9), grupo, tipo inválido, QR Code, status de conexão, presença e ack.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/resources/features/webhook.feature`, `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`

Cenário: mensagem do dono

- Quando o webhook chega com `fromMe=true`
- Então a API responde 200
- E o observador registra filtro contendo "Anti-X9"
- E a factory não é consultada

Cenário: grupo ou tipo que não é chat

- Quando o remetente é grupo (`@g.us`) ou o tipo não é chat (exemplo: `image`)
- Então a API responde 200
- E o observador registra filtro contendo "ignorada"
- E a factory não é consultada

Cenário: QR Code

- Quando o evento é `qrcode` com o código da sessão
- Então a API responde 200
- E o QR é guardado para aquela sessão
- E a factory não é consultada

Cenário: conexão, presença e ack

- Quando o evento é `status-find`, `onpresencechanged` ou `onack`
- Então a API responde 200
- E a factory não é consultada

### WPP-03 Identificar o cliente no chat privado

O sistema deve atender chat 1:1 mesmo quando o payload real do WPPConnect traz `id` string, `sender` objeto e texto em `content`. Chat com JID `@lid` e `isGroupMsg=false` entra no atendimento. Se `type` vier nulo e houver texto, o chat também entra.

Na primeira mensagem, com o estado do usuário em início, o sistema envia o menu principal e avança o estado para menu principal.

O número `5511988887777` é normalizado para `551188887777` antes de consultar o estado (o nono dígito extra depois do DDD cai).

Contrato: `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`

Cenário: primeira mensagem de um chat privado

- Dado uma empresa `LOCACAO` e o estado do remetente em início
- Quando chega um chat 1:1 com texto
- Então a API responde 200
- E a mensagem enviada de volta contém o menu ("Digite 1, 2 ou 3")
- E o estado do remetente passa a menu principal

### WPP-04 Consultar e iniciar a sessão do painel

O sistema deve consultar o status e iniciar a sessão no WPPConnect sem devolver o token interno. Aceita `x-admin-token` ou `x-cliente-token`. Empresa inexistente responde 404. Iniciar é POST; GET em `/iniciar` responde 405.

Contrato: `especificacao/back/paths/whatsapp-status.yaml`, `especificacao/back/paths/whatsapp-iniciar.yaml` (`SessaoStatusResponseDTO`)
Prova: `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`

Cenário: status com token admin

- Dado a empresa da sessão
- Quando o painel envia GET `/whatsapp/{sessao}/status` com token admin
- Então a API responde 200
- E o corpo traz sessão, status e o QR quando houver

Cenário: iniciar com token de cliente

- Dado a empresa da sessão
- Quando o painel envia POST `/whatsapp/{sessao}/iniciar` com token de cliente
- Então a API responde 200
- E a sessão é iniciada no WPPConnect

Cenário: sem token

- Quando GET `/whatsapp/{sessao}/status` chega sem token de painel
- Então a API responde 401
- E o WPPConnect não é consultado

Cenário: empresa inexistente

- Quando GET `/whatsapp/{sessao}/status` cita uma sessão sem empresa
- Então a API responde 404
- E o WPPConnect não é consultado

Cenário: GET em iniciar

- Quando o painel envia GET `/whatsapp/{sessao}/iniciar`
- Então a API responde 405
- E a sessão não é iniciada

### WPP-05 Recusar data e horário que já passaram na conversa de locação

O sistema deve recusar, no fuso `America/Sao_Paulo`, data anterior a hoje na pesquisa (`ESPERANDO_DATA`) e na reserva (`RESERVA_ESPERANDO_DATA`), e horário de hoje que já passou na reserva por hora (`RESERVA_ESPERANDO_HORARIO`). A recusa acontece antes do Google Agenda, a conversa permanece no mesmo passo e o cliente pode enviar outro valor. Hoje com horário ainda válido segue o fluxo normal. Formato inválido e data futura ocupada mantêm as mensagens que já existiam.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`

Cenário: data passada na pesquisa

- Dado uma empresa `LOCACAO` com agenda configurada e o cliente em `ESPERANDO_DATA`
- Quando o cliente envia a data de ontem no formato DD-MM-AAAA
- Então o bot responde "Essa data já passou. Envie uma data de hoje em diante, no formato DD-MM-AAAA, por exemplo 16-09-2026."
- E o Google Agenda não é consultado
- E o estado continua `ESPERANDO_DATA`

Cenário: data passada na reserva

- Dado uma empresa `LOCACAO` com reserva automática e o cliente em `RESERVA_ESPERANDO_DATA`
- Quando o cliente envia a data de ontem
- Então o bot responde "Essa data já passou. Envie uma data de hoje em diante, no formato DD-MM-AAAA, por exemplo 16-09-2026."
- E o Google Agenda não é consultado
- E o estado continua `RESERVA_ESPERANDO_DATA`

Cenário: horário de hoje já passado na reserva por hora

- Dado uma empresa com locação por hora e o cliente em `RESERVA_ESPERANDO_HORARIO` com a data de hoje na reserva
- Quando o cliente envia um horário anterior à hora atual
- Então o bot responde "Esse horário de hoje já passou. Envie um horário que ainda não passou, no formato HH:MM, por exemplo 18:00."
- E o horário não é gravado na reserva
- E o estado continua `RESERVA_ESPERANDO_HORARIO`

Cenário: hoje com horário ainda válido continua o fluxo

- Dado o cliente em `ESPERANDO_DATA` ou `RESERVA_ESPERANDO_DATA`
- Quando envia a data de hoje
- Então o Google Agenda é consultado para essa data
- E, na reserva por hora, o bot pede o horário e o estado passa a `RESERVA_ESPERANDO_HORARIO`
- Quando, com a data de hoje na reserva, o cliente envia um horário que ainda não passou
- Então o horário é gravado
- E o bot pede o nome do responsável e o estado passa a `RESERVA_ESPERANDO_NOME`

### WPP-06 Atendimento sem dado pessoal nos logs

O sistema deve registrar o atendimento no log sem número de telefone completo, texto de mensagem, nome de cliente final, e-mail completo, token, senha nem identificador da agenda; o número aparece mascarado com os 4 últimos dígitos e o texto, só pelo tamanho; erros registram a classe da exceção e o código HTTP, nunca a mensagem; o detalhe técnico de cada evento do webhook só aparece em nível DEBUG.

Contrato: nenhum (não há rota nem DTO de API novo)
Prova: `src/test/resources/features/webhook.feature`, `src/test/java/com/example/demo/util/MascaraLogTest.java`, `src/test/java/com/example/demo/servico/observador/ObservadorwhatsappTest.java`, `src/test/java/com/example/demo/servico/observador/ObservadorGoogleAgendaTest.java`, `src/test/java/com/example/demo/dto/WhatsappWebhookDTORaioXTest.java`, `src/test/java/com/example/demo/servico/ServicoWppConnectLogTest.java`, `src/test/java/com/example/demo/strategy/LocacaoStrategyLogTest.java`

Cenário: mensagem recebida e resposta enviada

- Dado uma empresa `LOCACAO` na sessão informada e a saída de log capturada
- Quando chega POST `/webhook/whatsapp` com `type=chat`, remetente `5511955553333@c.us` e texto
- Então a API responde 200
- E o log contém o número mascarado `***3333` e o tamanho do texto
- E o log não contém o número completo nem o texto da mensagem recebida ou da resposta enviada

Cenário: mensagem de grupo com participante

- Dado o nível DEBUG ligado
- Quando chega um evento com id `false_<grupo>@g.us_<hash>_<participante>@c.us`
- Então o resumo técnico do evento traz id, tipo, `isGroupMsg`, `fromMe`, número mascarado e tamanho do texto
- E não contém o número do grupo, o número do participante, `body` nem `content`

Cenário: erro HTTP do WPPConnect

- Quando o envio ou a consulta ao WPPConnect falha com erro HTTP cujo corpo traz nome ou segredo
- Então o log registra a sessão, o número mascarado, a classe da exceção e o código HTTP
- E não registra a mensagem da exceção, o corpo da resposta, o token nem o nome do cliente

### WPP-07 Confirmar a reserva automática ao cliente só depois do registro

O sistema deve, na conversa de locação com reserva automática, tratar o "sim" da confirmação (`sim`, `s`, `confirmar` ou `1` em `RESERVA_CONFIRMACAO`) como pedido, não como reserva feita: muda o estado para `RESERVA_REGISTRANDO` antes de publicar `EventoReservaConfirmada`, responde "Anotei seu pedido para <data>. Estou registrando na agenda e já confirmo." e deixa a confirmação final para o resultado da agenda (AGD-02). Em `RESERVA_REGISTRANDO`, qualquer mensagem, inclusive `0`, `menu`, `voltar` e um segundo "sim", recebe "Ainda estou registrando sua reserva, já te respondo.", sem publicar outro evento e sem sair do estado.

Na solicitação de reserva, a empresa é atendida como reserva manual ("Recebemos sua mensagem. Logo um atendente vai te responder.") quando a reserva automática está desligada ou quando a reserva automática está ligada sem `googleCalendarId`; neste último caso o log registra o id da empresa e o motivo.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`

Cenário: "sim" na confirmação

- Dado uma empresa `LOCACAO` de diária, com reserva automática e agenda, e o cliente em `RESERVA_CONFIRMACAO` com data e nome
- Quando o cliente envia "sim"
- Então o estado passa a `RESERVA_REGISTRANDO` antes da publicação
- E `EventoReservaConfirmada` é publicado uma vez com data, nome, número e `idPedido`
- E o bot responde "Anotei seu pedido para <data>. Estou registrando na agenda e já confirmo."
- E o bot não responde "Reserva confirmada" nesse momento

Cenário: mensagem durante o registro

- Dado o cliente em `RESERVA_REGISTRANDO`
- Quando ele envia "sim", `0`, `menu` ou `voltar`
- Então o bot responde "Ainda estou registrando sua reserva, já te respondo."
- E nenhum evento novo é publicado
- E o estado continua `RESERVA_REGISTRANDO`

Cenário: empresa sem agenda

- Dado uma empresa com reserva automática ligada e sem `googleCalendarId` (ausente ou em branco)
- Quando o cliente pede a reserva
- Então o bot responde "Recebemos sua mensagem. Logo um atendente vai te responder."
- E não pede data nem nome

### WPP-08 Reservar por hora, em uma ou várias datas, na conversa

O sistema deve, na conversa de locação por hora com reserva automática e agenda, guiar a reserva assim: o cliente informa a data (até 180 dias à frente, sem repetir data); se a data está aberta, o bot lista os horários livres (inícios possíveis da janela de EMP-07, menos as ocupações do Google) e o estado passa a `RESERVA_ESPERANDO_HORARIO`; o cliente escolhe um horário; o bot pergunta "Quer reservar mais alguma data?" (`RESERVA_MAIS_DATAS`). As datas seguintes usam o mesmo horário e o mesmo nome. Cada data nova é validada contra a janela do horário fixo e a agenda; se não comporta, só ela cai, com o motivo. Em `RESERVA_MAIS_DATAS` o cliente responde SIM, NÃO ou "TIRAR dd-MM-yyyy", e pode tirar uma data também na confirmação. O pedido tem no máximo 10 datas.

Data fechada (dia especial, feriado ou dia da semana fechado) recebe o motivo e as próximas 3 datas abertas. Horário fora do passo da janela ou fora dela é recusado com a lista de inícios válidos. Data sem horário livre avisa que não há horário. Na confirmação, o bot mostra "📅 Datas:" quando há mais de uma e publica um só `EventoReservaConfirmada` com todas as datas, o horário e a marca de reserva por hora, que o observador registra como em AGD-03. O nome do cliente tem no máximo 80 caracteres, sem quebra de linha.

O bot atende como reserva manual quando a reserva automática está desligada ou quando está ligada sem `googleCalendarId` (WPP-07); locação por hora com a reserva automática ligada e agenda não é mais reserva manual.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`, `src/test/java/com/example/demo/servico/ServicoFuncionamentoTest.java`

Cenário: data aberta lista os horários livres

- Dado uma empresa por hora com reserva automática e agenda
- Quando o cliente informa uma data aberta daqui a 10 dias
- Então o bot lista os horários livres
- E o estado passa a `RESERVA_ESPERANDO_HORARIO`

Cenário: data fechada

- Dado a data como dia especial fechado ou feriado fechado
- Quando o cliente informa essa data
- Então o bot informa o motivo e sugere as próximas 3 datas abertas

Cenário: mais uma data

- Dado o cliente com uma data e o horário "14:00" escolhidos
- Quando ele responde SIM e informa outra data aberta que comporta o horário
- Então a data entra no pedido, ordenada, e o bot pergunta de novo

Cenário: tirar uma data

- Dado o pedido com duas datas
- Quando o cliente envia "tirar" com uma delas
- Então ela sai do pedido e o estado continua `RESERVA_MAIS_DATAS`

Cenário: limites

- Quando o cliente informa uma data além de 180 dias, uma data repetida ou a 11ª data
- Então o bot recusa e explica o limite

Cenário: confirmação publica um só evento

- Dado o pedido com duas datas, o horário e o nome
- Quando o cliente envia "sim" em `RESERVA_CONFIRMACAO`
- Então um só `EventoReservaConfirmada` sai com as duas datas e a marca de reserva por hora

### WPP-09 Consultar a agenda e pesquisar data de empresa por hora

O sistema deve, para empresa com locação por hora, responder "Agenda dos próximos dias" com um status por dia (`✅ Livre`, `❌ Lotado` ou `🚫 Fechado`, conforme AGD-04) e responder "Pesquisar data" com os horários livres da data, com "Todos os horários desta data já estão reservados" quando não sobra nenhum ou com "Não abrimos nesta data" e o motivo quando a janela está fechada. A empresa de diária segue com "Livre" e "Alugado" por dia inteiro.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`

Cenário: agenda dos próximos dias

- Dado uma empresa por hora com um dia lotado, um fechado e um livre
- Quando o cliente escolhe "Agenda dos próximos dias"
- Então a lista traz "❌ Lotado", "🚫 Fechado" e "✅ Livre"
- E a agenda não usa a consulta de dias ocupados da diária

Cenário: pesquisar data com horários livres

- Dado uma data aberta com os horários 14:00 e 15:00 livres
- Quando o cliente pesquisa essa data
- Então o bot responde com "14:00, 15:00"

Cenário: pesquisar data lotada ou fechada

- Quando o cliente pesquisa uma data sem horário livre, ou uma data fechada por feriado
- Então o bot responde que está tudo reservado, ou informa que não abre e o motivo
