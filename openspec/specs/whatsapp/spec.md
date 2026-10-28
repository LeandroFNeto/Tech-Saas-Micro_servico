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
