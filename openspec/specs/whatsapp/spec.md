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

### WPP-02 Filtrar ruído, mensagens antigas e silenciar o bot por intervenção do dono (Anti-X9 com trava de 20 min)

O sistema deve responder 200 e não consultar a factory de atendimento quando o evento não é um chat de cliente a ser processado:
1. Mensagem enviada pelo próprio número conectado (`fromMe=true`, entregue pelo gateway no evento `onselfmessage`) que tenha sido escrita por uma pessoa da empresa e não enviada pelo próprio bot: o sistema ativa a trava de 20 minutos para o cliente/contato alvo e silencia o bot (Anti-X9). Cada nova mensagem da pessoa renova os 20 minutos. A trava vale para o cliente destinatário da mensagem, também quando ele aparece com mais de um identificador (`@lid` e número), e nunca para o número da empresa. A mensagem enviada pelo bot que volta no webhook com `fromMe=true` é reconhecida por até 2 minutos, mesmo quando chega antes de o envio terminar, em qualquer ambiente, e não liga a trava.
2. Chat de cliente enquanto a trava de 20 minutos do proprietário estiver ativa: o sistema não responde, mantendo o atendimento manual do dono sem interferência do robô.
3. Mensagem antiga recebida pelo webhook: mensagens cujo `timestamp` possua atraso superior a 2 minutos (120 segundos) em relação ao relógio do sistema são descartadas.
4. Ruídos gerais: grupo (`@g.us`), tipo inválido, QR Code, status de conexão, presença e ack.

O gateway de WhatsApp deve entregar ao webhook as mensagens escritas pelo dono; sem isso a trava não pode funcionar.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`, `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`
Prova: `src/test/resources/features/webhook.feature`, `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/java/com/example/demo/dto/WhatsappWebhookDTOTest.java`, `src/test/java/com/example/demo/servico/GerenciadorSessaoTest.java`, `src/test/java/com/example/demo/servico/ServicoMensagemTest.java`, `wppconnect-server/src/tests/config.test.ts`

Cenário: mensagem do dono ativa trava de 20 minutos

- Quando o webhook chega com o evento `onselfmessage` e `fromMe=true` em conversa com cliente
- Então a API responde 200
- E a trava de 20 minutos é ativada para aquele contato
- E o observador registra filtro contendo "Anti-X9"
- E a factory não é consultada

Cenário: evento onselfmessage não é tratado como conexão

- Quando o webhook chega com `event=onselfmessage` e sem o campo `fromMe`
- Então a mensagem é tratada como do dono e a trava de 20 minutos é ativada para o destinatário
- E o evento `onack` continua sendo ignorado como ruído de conexão e não liga a trava

Cenário: eco do bot não liga a trava

- Dado que o bot acabou de enviar uma resposta ao cliente
- Quando o webhook chega com `fromMe=true` para o mesmo contato e com o mesmo conteúdo da resposta enviada
- Então a API responde 200
- E a trava de 20 minutos não é ativada
- E o observador registra filtro contendo "Eco do bot ignorado"
- E a próxima mensagem do cliente é atendida normalmente

Cenário: eco do bot que chega antes de o envio terminar

- Dado que o bot começou a enviar uma resposta e o gateway ainda não confirmou o envio
- Quando o eco da resposta chega ao webhook com `fromMe=true`
- Então a trava de 20 minutos não é ativada

Cenário: dono escreve para cliente identificado por @lid

- Dado que o cliente é identificado pelo bot como `@lid` e pelo número do celular
- Quando a pessoa da empresa escreve para esse cliente e depois o cliente responde
- Então a trava de 20 minutos vale para o cliente, qualquer que seja o identificador recebido
- E a trava não é gravada no número da empresa
- E a factory não é consultada

Cenário: nova mensagem do dono renova a trava

- Dado que a pessoa da empresa escreveu ao cliente há 15 minutos
- Quando ela escreve de novo
- Então a trava passa a valer por 20 minutos a partir da nova mensagem

Cenário: cliente envia mensagem durante a pausa do proprietário

- Dado que o dono respondeu ao cliente há menos de 20 minutos
- Quando o cliente envia uma nova mensagem
- Então a API responde 200
- E o observador registra filtro contendo "proprietário atendeu nos últimos 20 minutos"
- E a factory não é consultada

Cenário: gateway entrega a mensagem do dono

- Dado a configuração padrão do gateway
- Quando a pessoa da empresa digita uma mensagem no celular ou no WhatsApp Web
- Então o gateway posta no webhook o evento `onselfmessage` com `fromMe=true`, `to` do cliente e o texto

Cenário: mensagem antiga descartada

- Quando o webhook chega com timestamp com mais de 2 minutos de atraso
- Então a API responde 200
- E o observador registra filtro contendo "Mensagem antiga descartada"
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

Data fechada (dia especial, feriado ou dia da semana fechado) recebe o motivo e as próximas 3 datas abertas sugeridas de forma contextual (sexta/sábado sugere próximo dia do fim de semana; domingo sugere próximo ou anterior domingo; dias úteis sugerem dia seguinte e mesma semana). Horário fora do passo da janela ou fora dela é recusado indicando o início mais próximo e a lista de inícios válidos. Data sem horário livre avisa que não há horário e sugere datas contextuais abertas. Na confirmação, o bot mostra "📅 Datas:" quando há mais de uma e publica um só `EventoReservaConfirmada` com todas as datas, o horário e a marca de reserva por hora, que o observador registra como em AGD-03. O nome do cliente tem no máximo 80 caracteres, sem quebra de linha.

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


### WPP-10 Envio de lembretes automáticos e sugeridos

O sistema deve disparar lembretes de confirmação de reservas futuras aos clientes nos modos AUTOMATICO e SUGERIDO, respeitando o horário de Brasília (entre 08:00 e 20:00), limites diários e intervalos de segurança anti-bloqueio.

Contrato: `especificacao/back/paths/empresas-reservas-lembrete.yaml`
Prova: `src/test/java/com/example/demo/servico/ServicoLembreteReservaTest.java`

Cenário: disparo agendado
- Dado uma reserva aguardando lembrete no horário e janela configurados
- Quando o ciclo de envio é executado
- Então a mensagem é enviada ao cliente e a reserva passa a LEMBRETE_ENVIADO

### WPP-11 Interpretar respostas ao lembrete

O sistema deve, quando o cliente responder a um lembrete no estado INICIO, interpretar a mensagem com palavras-chave e emojis para confirmar a reserva, solicitar remarcação/cancelamento ou registrar solicitação de não contatar (opt-out).

Contrato: nenhum (fluxo do webhook do WhatsApp)
Prova: `src/test/java/com/example/demo/servico/InterpretadorPorPalavrasTest.java`, `src/test/java/com/example/demo/servico/ServicoRespostaLembreteTest.java`

Cenário: cliente confirma reserva
- Dado um cliente com reserva em LEMBRETE_ENVIADO respondendo "Sim, confirmo"
- Quando o webhook processa a mensagem
- Então a reserva passa a CONFIRMADA_PELO_CLIENTE e o cliente recebe mensagem de confirmação

### WPP-12 Sessão da atendente da Gamb no WhatsApp

O sistema deve entregar a mensagem da empresa interna de ramo reservado `ATENDENTE_GAMB` à `AtendenteGambStrategy`, sem mudar o roteamento nem as strategies das empresas clientes (WPP-01). Todo contato dessa sessão é tratado como cliente. A primeira mensagem, e `0` em qualquer passo, abre o menu "Tirar uma dúvida" (`1`), "Marcar reunião" (`2`) ou "Conhecer a Gamb" (`3`). Na opção `3`, e no texto livre que pergunta do que se trata a Gamb, o que ela oferece ou como ela resolve o problema do cliente (no menu e na dúvida), o bot responde pela base comercial da Gamb: quem é, o que existe hoje e o que está em desenvolvimento, ligando o problema descrito ao que existe hoje, sem informar valor ou faixa de preço, sem prometer prazo, vaga ou garantia, sem citar cliente nem comparar com concorrente, e termina convidando a digitar *2* para marcar reunião. Assunto fora da Gamb recebe a recusa comercial. Na dúvida, o bot responde pelo Suporte de `AssistenteTutoriaService`, só com a base de conhecimento e os avisos ativos, sem enviar ao modelo nem devolver dado de empresa e sem executar ação no sistema. A resposta conversa como uma pessoa da equipe: responde primeiro o que foi perguntado, acompanha o jeito de escrever do cliente e varia a forma de falar, sem roteiro fixo, dentro do mesmo escopo. Mensagem fora do escopo recebe a recusa padrão do Suporte seguida de "Para consultar os dados da sua empresa, use o painel do app.". Mensagem filtrada, recusada ou respondida pelo bot devolve 200 ao webhook. O número sai mascarado e o texto só pelo tamanho no log (WPP-06).

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/strategy/AtendenteGambStrategyTest.java`, `src/test/java/com/example/demo/strategy/GambStrategiesLogTest.java`, `src/test/resources/features/webhook.feature`, `src/test/resources/features/roteamento-strategy.feature`

Cenário: primeira mensagem abre o menu

- Dado uma empresa persistida com a sessão da atendente e ramo `ATENDENTE_GAMB`
- Quando chega POST `/webhook/whatsapp` com `type=chat` e texto "Oi"
- Então a API responde 200
- E a `FactoryModulo` entrega a mensagem à `AtendenteGambStrategy`
- E o bot responde o menu de dúvida e reunião

Cenário: cliente tira dúvida no número da Gamb

- Dado o cliente na opção de dúvida da sessão da atendente
- Quando pergunta como conectar o WhatsApp
- Então o bot responde com o passo a passo da base de conhecimento

Cenário: cliente pede dado de empresa no número da Gamb

- Dado o cliente na opção de dúvida da sessão da atendente
- Quando pede preços, reservas ou dados de uma empresa
- Então o bot responde a recusa padrão do Suporte e orienta a usar o painel
- E nenhum dado de empresa é consultado nem devolvido

Cenário: strategies das empresas não mudam

- Dado as strategies carregadas no contexto Spring
- Quando a `FactoryModulo` busca o ramo "Locação"
- Então a strategy retornada continua a `LocacaoStrategy`

Cenário: cliente pergunta do que se trata a Gamb

- Dado o cliente no menu da sessão da atendente
- Quando ele digita *3* ou escreve "o que vocês fazem?"
- Então o bot responde quem é a Gamb e o que ela oferece hoje
- E a resposta não traz valor, prazo nem nome de cliente
- E termina convidando a digitar *2* para marcar reunião

Cenário: cliente descreve o problema dele

- Dado o cliente na sessão da atendente
- Quando ele escreve "perco reservas porque demoro a responder, como vocês resolvem?"
- Então o bot liga o problema ao assistente no WhatsApp com reserva pela Google Agenda
- E apresenta o que está em desenvolvimento como avaliado caso a caso, sem prometer
- E não informa preço nem prazo

Cenário: pergunta do app continua no Suporte

- Dado o cliente na opção de dúvida
- Quando ele pergunta como conectar o WhatsApp no app
- Então o bot responde pelo Suporte, com o passo a passo, e não pela resposta comercial

Cenário: resposta de dúvida com tom de conversa

- Dado o cliente na opção de dúvida da sessão da atendente
- Quando a pergunta é enviada ao Suporte
- Então o prompt orienta responder primeiro o que foi perguntado, acompanhar o tom do cliente e variar a forma de falar
- E o prompt mantém o escopo: sem dado de empresa e sem ação no sistema

### WPP-13 Reunião com a equipe da Gamb pelo WhatsApp

O sistema deve, na sessão da atendente, deixar o cliente marcar reunião com a equipe informando o motivo (`AJUSTE` ou `NOVA_INOVACAO`), a data (escrita em qualquer formato usual, como `25/12/2026`, `25 de dezembro` ou `amanhã`, de hoje até o limite de dias à frente do funcionamento e em dia aberto; o bot responde sempre no formato DD-MM-AAAA) e o horário (HH:MM, entre os horários livres) e confirmando o resumo. Quando a data é duvidosa (apenas o dia, dia da semana, data sem ano que já passou neste ano ou data impossível), o bot pergunta qual data o cliente quis dizer antes de seguir, aceita *sim* ou a data corrigida e não consulta a agenda enquanto a dúvida não for resolvida. Com o horário livre na Google Agenda da empresa interna, conferido de novo na confirmação, o sistema cria o evento pelo `ServicoReserva` com o motivo no título, o número do cliente mascarado na descrição e todos os `emailsResponsaveisReuniao` daquele momento como convidados, com convite por e-mail, e responde "Reunião marcada para <data> às <horário>.". Com o horário ocupado, o bot oferece os horários livres e não cria evento; sem horário livre na data, pede outra data. Sem responsáveis cadastrados, sem agenda configurada ou com a agenda desconectada ou falhando, o bot informa que o agendamento está indisponível, não cria evento e mantém a dúvida funcionando. Ao mudar a lista de responsáveis (EMP-14), o sistema atualiza só os convidados das reuniões futuras marcadas pelo atendente (entram os e-mails novos, saem os removidos) e não cancela nem remarca reunião; reunião cancelada na agenda é ignorada. O e-mail dos responsáveis não aparece em log.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`, `especificacao/back/paths/empresas-admin.yaml`
Prova: `src/test/java/com/example/demo/strategy/AtendenteGambStrategyTest.java`, `src/test/java/com/example/demo/servico/ServicoReservaReuniaoTest.java`, `src/test/java/com/example/demo/strategy/GambStrategiesLogTest.java`, `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`

Cenário: cliente marca reunião em horário livre

- Dado responsáveis cadastrados e a Google Agenda da Gamb conectada
- Quando o cliente escolhe marcar reunião, informa motivo, data e horário livres e confirma o resumo
- Então o evento é criado com os responsáveis convidados por e-mail
- E o bot responde com a data e o horário da reunião

Cenário: cliente escolhe horário ocupado

- Dado um horário já ocupado na Google Agenda da Gamb
- Quando o cliente informa esse horário, ou o horário fica ocupado antes da confirmação
- Então o bot oferece outros horários
- E nenhum evento é criado

Cenário: agendamento indisponível

- Dado a lista de responsáveis vazia, ou a agenda desconectada
- Quando o cliente escolhe marcar reunião
- Então o bot informa que o agendamento está indisponível e nenhum evento é criado
- E a opção de dúvida continua respondendo

Cenário: lista de responsáveis muda

- Dado reuniões futuras marcadas pelo atendente
- Quando o admin salva uma nova lista de responsáveis
- Então só os convidados dessas reuniões são atualizados
- E título, data e horário ficam como estão e nada é cancelado
- E nenhum e-mail aparece em log

Cenário: cliente escreve a data do jeito que preferir

- Dado o cliente no passo de data da reunião
- Quando ele escreve "25 de dezembro" ou "25.12" e a data cai no futuro dentro do limite
- Então o bot segue para os horários livres daquele dia
- E nenhuma mensagem pede o formato DD-MM-AAAA

Cenário: data duvidosa pergunta antes de seguir

- Dado o cliente no passo de data da reunião
- Quando ele escreve "sábado" ou "dia 25"
- Então o bot pergunta se ele quer dizer a data sugerida, com o dia da semana
- E ao receber "sim", segue para os horários livres daquela data
- E ao receber outra data, usa a data corrigida
- E ao receber "não", volta a pedir a data

### WPP-14 Limite diário de mensagens da atendente

O sistema deve contar as mensagens de cada número por dia (fuso de Brasília) na sessão da atendente, com teto configurado em `gamb.atendente.limite-diario-mensagens` (padrão 30). Acima do teto, o bot responde "Você atingiu o limite de mensagens de hoje. Volte amanhã para continuarmos a conversa." e não chama o modelo nem a agenda. A contagem recomeça no dia seguinte.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/servico/LimiteDiarioMensagensTest.java`, `src/test/java/com/example/demo/strategy/AtendenteGambStrategyTest.java`

Cenário: cliente passa do limite diário

- Dado um número que atingiu o limite diário
- Quando envia outra mensagem à sessão da atendente
- Então o bot responde a mensagem fixa
- E o modelo e a agenda não são chamados

### WPP-15 Sessão de teste de módulos da Gamb

O sistema deve entregar a mensagem da empresa interna de ramo reservado `TESTE_MODULOS` à `TesteModulosStrategy`, que só atende números da lista `gamb.teste-modulos.numeros-autorizados`. `/modulos` lista o nome e a descrição de cada `ModuloExperimental` registrado; `/modulo <nome> <texto>` executa o módulo e devolve a resposta simulada; outro texto recebe a ajuda dos comandos; módulo inexistente ou sem texto não executa nada. Número fora da lista recebe "Este número não está autorizado a usar o bot de teste de módulos." e lista vazia recebe "Não há número autorizado para o bot de teste de módulos. Nenhum módulo foi executado."; em ambos nenhum módulo roda. O módulo experimental responde de forma simulada e não grava em dados de empresas reais. O primeiro módulo registrado é `teste-virtual`, que reaproveita o modo `TESTE_VIRTUAL` do assistente. A mensagem devolve 200 ao webhook e o número sai mascarado no log, sem o texto do comando.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/strategy/TesteModulosStrategyTest.java`, `src/test/java/com/example/demo/strategy/experimental/RegistroModulosExperimentaisTest.java`, `src/test/java/com/example/demo/strategy/GambStrategiesLogTest.java`, `src/test/resources/features/webhook.feature`, `src/test/resources/features/roteamento-strategy.feature`

Cenário: número autorizado lista os módulos

- Dado um número autorizado na sessão de teste
- Quando envia `/modulos`
- Então o bot responde com o nome e a descrição de cada módulo registrado

Cenário: número autorizado executa um módulo

- Dado um módulo experimental registrado
- Quando o número autorizado envia `/modulo <nome> <texto>`
- Então o bot responde com a resposta simulada do módulo, sem gravar em empresa real

Cenário: número não autorizado

- Dado um número fora da lista de autorizados, ou a lista vazia
- Quando envia qualquer mensagem à sessão de teste
- Então a API responde 200
- E o bot responde a mensagem fixa e nenhum módulo é executado
### WPP-16 Autenticar origem do webhook com segredo compartilhado

O sistema deve exigir o cabeçalho `x-webhook-token` em todas as requisições HTTP enviadas para `/webhook/*`. O valor recebido é comparado em tempo constante contra o segredo compartilhado configurado em `wppconnect.webhook-secret` (`WPPCONNECT_WEBHOOK_SECRET`). Caso o cabeçalho esteja ausente, o token seja divergente ou o segredo do sistema esteja vazio/não configurado, a requisição é recusada imediatamente com status HTTP `401 Unauthorized`. A recusa é registrada em log sem expor o segredo e sem executar nenhum módulo nem despachar mensagens.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/security/WebhookSecretFilterTest.java`, `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/resources/features/webhook.feature`

Cenário: token válido aceito
- Dado um webhook configurado com segredo compartilhado
- Quando o provedor posta em `/webhook/whatsapp` com o cabeçalho `x-webhook-token` correto
- Então a requisição é processada normalmente

Cenário: requisição sem token recusada com 401
- Dado um webhook protegido por segredo
- Quando uma requisição chega em `/webhook/whatsapp` sem o cabeçalho `x-webhook-token`
- Então o sistema responde 401
- E nenhum módulo de atendimento é acionado

Cenário: token incorreto recusado com 401 sem vazar segredo no log
- Dado um webhook protegido por segredo
- Quando uma requisição chega com token divergente
- Então o sistema responde 401
- E o log de recusa não expõe o token recebido nem o segredo configurado

### WPP-21 Log do gateway atual sem dado pessoal

O sistema deve manter o gateway WPPConnect em uso registrando a partir do nível `info`, sem número completo, texto de mensagem, objeto de erro bruto nem corpo do webhook, também quando o envio ou o webhook falha. Na falha, o log traz só o tipo do erro, o código e o status HTTP, quando houver. A resposta 500 do gateway ao backend não devolve o objeto do erro.

Contrato: nenhum.
Prova: `wppconnect-server/src/tests/logSemDadoPessoal.test.ts`, `wppconnect-server/src/tests/statusConnectionLog.test.ts`, `wppconnect-server/src/tests/returnErrorLog.test.ts`. Prova operacional: conferência do log do container `wppconnect_server` no deploy.

Cenário: envio para um número
- Dado um pedido de envio do backend ao gateway
- Quando o gateway confere o número e envia
- Então o log não traz o número de destino nem o texto

Cenário: envio que falha
- Dado um envio de texto ou imagem que a biblioteca rejeita com o destino e o texto no erro
- Quando o gateway trata a falha
- Então o log e a resposta 500 não trazem o número nem o texto

Cenário: falha no webhook
- Dado o gateway sem conseguir postar no webhook do backend
- Quando registra a falha
- Então o log traz só o código HTTP ou o tipo da falha, sem o corpo da mensagem
