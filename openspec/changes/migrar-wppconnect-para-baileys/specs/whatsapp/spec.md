# Spec Delta: Migração Baileys mantendo Paridade Integral com WPPConnect

## ADDED Requirements

### Requirement: WPP-17 Garantia de paridade funcional de gateway entre WPPConnect e Baileys
O sistema SHALL responder através do gateway Baileys com a mesma interface REST de saída e o mesmo webhook de entrada do WPPConnect atual, incluindo as customizações do fork, para que o backend, a coleta de contatos, o opt-out e os lembretes fiquem inalterados, e sem dado pessoal nos logs do gateway.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`, `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/resources/features/webhook.feature`, testes de contrato em `baileys-gateway/`

#### Scenario: Envio de mensagem com cabeçalho de segredo autenticado
- **GIVEN** que o gateway Baileys recebe uma mensagem do WhatsApp
- **WHEN** o gateway dispara o webhook para o backend Spring Boot
- **THEN** o cabeçalho `x-webhook-token` com o segredo compartilhado configurado é enviado na requisição HTTP
- **AND** o `WebhookSecretFilter` aceita a chamada com status 200

#### Scenario: Notificação de mensagem do proprietário (Anti-X9)
- **GIVEN** que o dono da empresa envia uma mensagem de texto ou mídia para um cliente através do WhatsApp móvel ou Web
- **WHEN** o socket Baileys detecta a mensagem emitida com `fromMe=true`
- **THEN** o gateway despacha o webhook com `event=onselfmessage`, `fromMe=true` e o destinatário `to`
- **AND** o backend ativa a trava de proprietário de 20 minutos para aquele contato, silenciando o robô

#### Scenario: Dono envia foto ou áudio ao cliente
- **GIVEN** que o dono envia uma foto, um áudio ou uma figurinha ao cliente pelo celular
- **WHEN** o gateway detecta a mensagem com `fromMe=true`
- **THEN** o gateway despacha `onselfmessage` com o `type` da mídia e o `to` do cliente
- **AND** o backend liga a trava de 20 minutos, como faz hoje com o WPPConnect

#### Scenario: Envio do dono desligado em emergência
- **GIVEN** que `WPPCONNECT_ON_SELF_MESSAGE` vale `false`
- **WHEN** o dono escreve ao cliente
- **THEN** o gateway não despacha `onselfmessage`

#### Scenario: Eco do próprio bot
- **GIVEN** que o bot enviou um texto ou uma foto com legenda ao cliente pelo gateway
- **WHEN** o gateway recebe de volta a mensagem com `fromMe=true` e o id que ele mesmo enviou
- **THEN** o gateway descarta o eco pelo id, sem despachar webhook
- **AND** se o eco escapar e chegar ao backend, o `RegistroEnviosBot` o reconhece pelo texto e não liga a trava

#### Scenario: Eco do bot depois da janela de 2 minutos
- **GIVEN** que o envio ficou na fila e o eco chega mais de 2 minutos depois do registro no backend
- **WHEN** o gateway recebe o eco com o id da mensagem que ele enviou
- **THEN** o gateway descarta o eco e a trava de 20 minutos não é ligada

#### Scenario: JID com sufixo de dispositivo
- **GIVEN** que o Baileys entrega `5511999990000:12@s.whatsapp.net`
- **WHEN** o gateway monta `from`, `to` e `sender.id`
- **THEN** os campos trazem `5511999990000@c.us`, sem o `:12`
- **AND** a trava e o contato do backend usam os dígitos corretos

#### Scenario: Reação, edição ou apagar do dono não liga a trava
- **GIVEN** que o dono reage com emoji, edita ou apaga uma mensagem pelo celular
- **WHEN** o gateway recebe a mensagem com `fromMe=true` desse tipo
- **THEN** o gateway não despacha webhook
- **AND** a trava de 20 minutos não é ligada

#### Scenario: Mensagem do cliente que não é texto nem mídia
- **GIVEN** que o cliente envia reação, enquete, edição ou apaga uma mensagem
- **WHEN** o gateway recebe o evento
- **THEN** o gateway não despacha webhook como `chat`

#### Scenario: Texto embrulhado em mensagem efêmera, resposta ou visualização única
- **GIVEN** que o cliente envia texto como resposta, em chat com mensagens temporárias ou como visualização única
- **WHEN** o gateway despacha o webhook
- **THEN** o payload traz `type` igual a `chat` e `body` com o texto, desembrulhado

#### Scenario: Canal, comunidade, Meta AI e lista de transmissão
- **WHEN** chega mensagem de `@newsletter`, comunidade, `@bot` ou `@broadcast`
- **THEN** o gateway não despacha webhook

#### Scenario: Grupo é ignorado no gateway
- **WHEN** chega mensagem de `@g.us`, do cliente ou do dono
- **THEN** o gateway não despacha webhook
- **AND** o backend continua descartando grupo como segunda barreira

#### Scenario: Conversa com o próprio número
- **GIVEN** que o dono escreve na conversa "você com você"
- **WHEN** o gateway recebe a mensagem com `fromMe=true` e `to` igual ao número conectado
- **THEN** o gateway não despacha webhook

#### Scenario: Nome do dono nunca vai no payload
- **GIVEN** que o dono envia uma mensagem pelo celular
- **WHEN** o gateway despacha `onselfmessage`
- **THEN** o payload não traz `pushname`, `notifyName` nem `sender.pushname`
- **AND** o nome do dono não é gravado em nenhum contato

#### Scenario: Timestamp em objeto Long
- **GIVEN** que o Baileys entrega `messageTimestamp` como objeto `Long`
- **WHEN** o gateway despacha o webhook
- **THEN** `timestamp` vai como número inteiro em segundos

#### Scenario: Mensagem com atraso temporal
- **GIVEN** que o gateway recebe do WhatsApp uma mensagem com timestamp antigo
- **WHEN** a mensagem é entregue no webhook com `timestamp` superior a 120 segundos de atraso
- **THEN** o backend descarta a mensagem como histórica sem acionar nenhuma strategy

#### Scenario: Histórico da conexão não é reenviado
- **GIVEN** que a sessão reconecta e o WhatsApp sincroniza mensagens antigas
- **WHEN** o Baileys entrega o histórico
- **THEN** o gateway não despacha essas mensagens ao webhook

#### Scenario: Cliente identificado por @lid com número conhecido
- **GIVEN** que o cliente escreve num chat `@lid` e a versão fixa do Baileys informa o número dele (`senderPn`)
- **WHEN** o gateway despacha o webhook
- **THEN** `from` traz o `@lid` e `sender.id` traz o número `@c.us`
- **AND** o backend registra o apelido e a trava do dono vale para os dois identificadores

#### Scenario: Nome do contato chega ao cadastro
- **GIVEN** que o cliente tem nome de exibição no WhatsApp
- **WHEN** o gateway despacha a mensagem dele
- **THEN** o payload traz o nome em `sender.pushname` e `notifyName`
- **AND** o backend registra o contato com o nome, cifrado em repouso

#### Scenario: Status do WhatsApp é ignorado no gateway
- **WHEN** chega uma atualização de `status@broadcast`
- **THEN** o gateway não despacha webhook

#### Scenario: Envio para número com ou sem o nono dígito
- **GIVEN** que o Java pede o envio para um número só com dígitos e `isLid=false`
- **WHEN** o gateway recebe `send-message`
- **THEN** o gateway confere o número no WhatsApp, envia para o JID real e responde 2xx
- **AND** número inexistente responde 400 e sessão desconectada responde 404

#### Scenario: Envio para contato @lid
- **WHEN** o Java pede o envio com `isLid=true` ou com 14 dígitos ou mais
- **THEN** o gateway envia para `<digitos>@lid` sem conferir o número

#### Scenario: Envio de foto pela URL cadastrada
- **WHEN** o Java pede `send-image` com `path` igual à URL http(s) da foto e uma legenda
- **THEN** o gateway baixa a foto e a envia como imagem com a legenda
- **AND** falha no download responde erro HTTP antes de o envio entrar na fila

#### Scenario: Token inválido
- **WHEN** uma rota da sessão recebe token ausente, inválido ou de outra sessão
- **THEN** o gateway responde 401 e o backend pede um novo token

#### Scenario: Logs do gateway sem dado pessoal
- **WHEN** o gateway registra recebimento, envio ou erro
- **THEN** o log traz o número mascarado e o tamanho do texto, sem número completo, texto, token, segredo ou QR

#### Scenario: Foto da galeria baixada com segurança
- **GIVEN** um `send-image` com a URL http(s) de uma foto
- **WHEN** o gateway baixa a foto
- **THEN** o host é resolvido e validado uma vez e a conexão usa esse mesmo IP
- **AND** rede interna, loopback e link-local são recusados, redirect não é seguido e só imagem dentro do limite de tamanho é aceita

### Requirement: WPP-18 Ciclo de vida da sessão e pareamento por QR Code
O sistema SHALL expor o status de sessão nos estados `CONNECTED`, `QRCODE`, `INITIALIZING` (reconectando) e `DISCONNECTED`, com o QR Code em PNG, reconectar sozinho depois de reinício, queda de rede ou celular sem bateria, e apagar as credenciais quando a sessão é encerrada ou desvinculada.

Contrato: `especificacao/back/paths/whatsapp-status.yaml`, `especificacao/back/paths/whatsapp-iniciar.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/java/com/example/demo/config/RestauradorSessoesWhatsappTest.java`, testes de contrato em `baileys-gateway/`

#### Scenario: Consulta de sessão em pareamento
- **GIVEN** uma sessão iniciada que ainda não foi pareada no Baileys
- **WHEN** o painel consulta o status da sessão
- **THEN** o gateway responde status `QRCODE` com o QR em PNG base64, e `qrcode-session` devolve os bytes PNG
- **AND** a leitura do QR Code pelo aplicativo comuta o status para `CONNECTED`

#### Scenario: QR Code no webhook
- **WHEN** o Baileys gera um QR novo
- **THEN** o gateway despacha `event=qrcode` com o QR em PNG base64, nunca o texto cru do QR

#### Scenario: Início repetido da sessão
- **GIVEN** uma sessão já conectada
- **WHEN** o backend chama `start-session` de novo, no boot ou pelo painel
- **THEN** o gateway responde `CONNECTED` sem abrir outro socket

#### Scenario: Restauração de sessão após reinício
- **GIVEN** uma sessão previamente autenticada e gravada no volume persistente
- **WHEN** o serviço `baileys-gateway` é reiniciado
- **THEN** o socket reconecta automaticamente com as credenciais salvas sem exigir novo escaneamento de QR Code

#### Scenario: Exclusão da empresa encerra a sessão
- **WHEN** o backend chama `close-session` ao excluir a empresa
- **THEN** o gateway encerra o socket, desconecta o aparelho e apaga as credenciais da sessão, que não volta no próximo reinício

#### Scenario: Logout pelo celular
- **WHEN** o dono desconecta o aparelho pelo WhatsApp do celular
- **THEN** o gateway apaga as credenciais, o status vai a `DISCONNECTED` e o próximo `start-session` gera QR novo

#### Scenario: Token continua válido depois do reinício do gateway
- **GIVEN** que o Java guardou o token de uma sessão
- **WHEN** o gateway reinicia e o Java envia uma mensagem com o mesmo token
- **THEN** o gateway aceita o token, porque ele é derivado do segredo e da sessão, sem estado em memória
- **AND** o token de outra sessão continua recusado com 401

#### Scenario: Gateway parado por um tempo
- **GIVEN** uma sessão pareada, com credenciais e `meta.json` no volume
- **WHEN** o servidor ou o container fica parado e volta
- **THEN** o gateway reconecta sozinho, sem o Java chamar `start-session` e sem novo QR
- **AND** o webhook da sessão usa a URL gravada no `meta.json`

#### Scenario: Celular sem bateria ou sem rede
- **GIVEN** uma sessão conectada cujo celular perdeu a bateria ou a rede
- **WHEN** o WebSocket cai por motivo de rede ou servidor
- **THEN** o gateway reconecta com espera crescente de 2 s a 60 s, sem desistir
- **AND** a sessão volta a `CONNECTED` sem pareamento novo quando a conexão volta

#### Scenario: Status durante a reconexão
- **GIVEN** uma sessão com credenciais válidas que está reconectando
- **WHEN** o painel consulta o status ou o Java chama `start-session`
- **THEN** o gateway responde `INITIALIZING`, sem QR novo e sem apagar credenciais
- **AND** o backend repassa `INITIALIZING` ao painel como estado próprio, sem convertê-lo em `QRCODE`

#### Scenario: Webhook fora da lista
- **WHEN** o `start-session` traz um webhook com host diferente de `WPPCONNECT_WEBHOOK_URL` e fora de `BAILEYS_HOSTS_WEBHOOK_PERMITIDOS`
- **THEN** o gateway responde 400 sem abrir sessão
- **AND** no boot, a URL do `meta.json` fora da lista é ignorada e a sessão usa o webhook padrão

#### Scenario: Celular 14 dias sem internet
- **GIVEN** que o WhatsApp desvinculou o aparelho depois de 14 dias do celular sem internet
- **WHEN** o gateway recebe `loggedOut`
- **THEN** o gateway apaga as credenciais e o status vai a `DISCONNECTED`, exigindo novo pareamento

#### Scenario: Mesma sessão aberta em outra instância
- **WHEN** o gateway recebe `connectionReplaced`
- **THEN** ele não reconecta em laço, registra o conflito sem dado pessoal e a sessão fica `DISCONNECTED`

#### Scenario: Desligamento brusco durante a gravação
- **WHEN** o gateway é interrompido enquanto grava as credenciais
- **THEN** a gravação atômica preserva a última versão boa e a sessão restaura no boot seguinte

#### Scenario: Mensagem do cliente durante a parada
- **WHEN** o cliente escreve enquanto o gateway estava fora e a mensagem chega com mais de 120 s de atraso
- **THEN** o backend a descarta e o bot não responde

### Requirement: WPP-19 Envio com digitação, fila e entrega robusta no gateway
O sistema SHALL enviar as respostas do bot pelo gateway em fila por sessão, com "digitando…" e intervalo aleatório entre envios, responder ao backend antes do envio terminar para caber no timeout de 5 s, não repostar ao webhook a mesma mensagem duas vezes e tentar de novo o webhook quando o backend falhar.

Contrato: nenhum novo (mesmas rotas `send-message`, `send-image` e o mesmo webhook)
Prova: testes de contrato em `baileys-gateway/`

#### Scenario: Digitando antes da resposta
- **WHEN** o gateway vai enviar um texto ao cliente
- **THEN** o cliente vê "digitando…" por 40 ms por caractere, entre 0,8 s e 3 s, antes da mensagem
- **AND** `BAILEYS_SIMULAR_DIGITACAO=false` desliga a digitação

#### Scenario: Envios da mesma sessão em fila
- **GIVEN** que a mesma sessão tem vários envios pendentes
- **WHEN** o gateway os processa
- **THEN** eles saem na ordem de chegada, um por vez, com intervalo aleatório de 1 a 2,5 s
- **AND** com mais de 50 envios na fila o gateway responde 503

#### Scenario: Resposta ao backend antes do envio
- **WHEN** o backend chama `send-message` ou `send-image` com sessão conectada, token válido e número existente
- **THEN** o gateway responde 2xx assim que o envio entra na fila, antes da digitação e do envio

#### Scenario: Envio aceito com o socket caído
- **GIVEN** que um envio aceito falhou porque o socket caiu
- **WHEN** a sessão reconecta em até 60 s
- **THEN** o gateway tenta o envio mais uma vez; senão descarta e registra no log sem dado pessoal

#### Scenario: Mensagem reentregue na reconexão
- **WHEN** o Baileys entrega de novo uma mensagem já postada nos últimos 10 minutos
- **THEN** o gateway não posta o webhook de novo

#### Scenario: Backend fora do ar
- **WHEN** o webhook falha por erro de rede ou 5xx do backend
- **THEN** o gateway tenta mais 3 vezes, depois de 1 s, 2 s e 4 s
- **AND** com 401 ou outro 4xx não tenta de novo

#### Scenario: @lid convertido pelo mapa do Baileys
- **GIVEN** que a mensagem do cliente chega em chat `@lid` sem o número no evento
- **WHEN** o mapa de `@lid` do Baileys conhece o número
- **THEN** o gateway preenche `sender.id` com o número `@c.us`

### Requirement: WPP-20 Pareamento por código de telefone
O sistema SHALL permitir conectar o WhatsApp da empresa por um código de 8 caracteres digitado no celular, como alternativa ao QR Code, com a mesma autorização de iniciar a sessão, sem gravar o telefone informado.

Contrato: `especificacao/back/paths/whatsapp-codigo-pareamento.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, testes de contrato em `baileys-gateway/`. A tela (código como caminho principal, passo a passo e testes do componente `conexao-whatsapp`) é da change `conexao-whatsapp-por-codigo`.

#### Scenario: Cliente pede o código
- **GIVEN** uma sessão aguardando pareamento
- **WHEN** o cliente autenticado da empresa pede o código informando o telefone com DDI 55
- **THEN** a API responde 200 com o código
- **AND** quando o código é digitado no celular, o status vai a `CONNECTED` pelo mesmo polling do QR

#### Scenario: Telefone inválido
- **WHEN** o telefone informado não tem 12 ou 13 dígitos com DDI 55
- **THEN** a API responde 400 sem chamar o gateway

#### Scenario: Sessão já conectada
- **WHEN** o código é pedido para uma sessão conectada
- **THEN** a API responde 409

#### Scenario: Pedidos em excesso
- **WHEN** a mesma sessão pede mais de 3 códigos em 10 minutos
- **THEN** a API responde 429

#### Scenario: Sessão de outra empresa
- **WHEN** um cliente pede o código para a sessão de outra empresa
- **THEN** a API responde 403, como em iniciar a sessão

#### Scenario: Gateway sem pareamento por código
- **GIVEN** que o backend aponta para o WPPConnect na reversão
- **WHEN** o código é pedido
- **THEN** a API responde 503 com "pareamento por código indisponível" e o QR Code continua funcionando

#### Scenario: Sessão ainda iniciando ou falha do Baileys
- **GIVEN** que a sessão acabou de abrir e o primeiro QR não saiu em 3 s
- **WHEN** o código é pedido
- **THEN** o gateway responde 409 "sessão ainda iniciando", e a falha do Baileys ao gerar o código vira 502 no gateway e 503 na API do Java

#### Scenario: Telefone fora dos logs
- **WHEN** o código é pedido
- **THEN** o telefone não é gravado e só aparece mascarado no log
