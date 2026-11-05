## MODIFIED Requirements

### Requirement: WPP-02 Filtrar ruído, mensagens antigas e silenciar o bot por intervenção do dono (Anti-X9 com trava de 20 min)

O sistema deve responder 200 e não consultar a factory de atendimento quando o evento não é um chat de cliente a ser processado:
1. Mensagem enviada pelo próprio número conectado (`fromMe=true`, entregue pelo gateway no evento `onselfmessage`) que tenha sido escrita por uma pessoa da empresa e não enviada pelo próprio bot: o sistema ativa a trava de 20 minutos para o cliente/contato alvo e silencia o bot (Anti-X9). Cada nova mensagem da pessoa renova os 20 minutos. A trava vale para o cliente destinatário da mensagem, também quando ele aparece com mais de um identificador (`@lid` e número), e nunca para o número da empresa. A mensagem enviada pelo bot que volta no webhook com `fromMe=true` é reconhecida por até 2 minutos, mesmo quando chega antes de o envio terminar, em qualquer ambiente, e não liga a trava.
2. Chat de cliente enquanto a trava de 20 minutos do proprietário estiver ativa: o sistema não responde, mantendo o atendimento manual do dono sem interferência do robô.
3. Mensagem antiga recebida pelo webhook: mensagens cujo `timestamp` possua atraso superior a 2 minutos (120 segundos) em relação ao relógio do sistema são descartadas.
4. Ruídos gerais: grupo (`@g.us`), tipo inválido, QR Code, status de conexão, presença e ack.

O gateway de WhatsApp deve entregar ao webhook as mensagens escritas pelo dono; sem isso a trava não pode funcionar.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`, `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`
Prova: `src/test/resources/features/webhook.feature`, `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/java/com/example/demo/dto/WhatsappWebhookDTOTest.java`, `src/test/java/com/example/demo/servico/GerenciadorSessaoTest.java`, `src/test/java/com/example/demo/servico/ServicoMensagemTest.java`, `wppconnect-server/src/tests/config.test.ts`

#### Scenario: mensagem do dono ativa trava de 20 minutos

- **WHEN** o webhook chega com o evento `onselfmessage` e `fromMe=true` em conversa com cliente
- **THEN** a API responde 200
- **AND** a trava de 20 minutos é ativada para aquele contato
- **AND** o observador registra filtro contendo "Anti-X9"
- **AND** a factory não é consultada

#### Scenario: evento onselfmessage não é tratado como conexão

- **WHEN** o webhook chega com `event=onselfmessage` e sem o campo `fromMe`
- **THEN** a mensagem é tratada como do dono e a trava de 20 minutos é ativada para o destinatário
- **AND** o evento `onack` continua sendo ignorado como ruído de conexão e não liga a trava

#### Scenario: eco do bot não liga a trava

- **GIVEN** que o bot acabou de enviar uma resposta ao cliente
- **WHEN** o webhook chega com `fromMe=true` para o mesmo contato e com o mesmo conteúdo da resposta enviada
- **THEN** a API responde 200
- **AND** a trava de 20 minutos não é ativada
- **AND** o observador registra filtro contendo "Eco do bot ignorado"
- **AND** a próxima mensagem do cliente é atendida normalmente

#### Scenario: eco do bot que chega antes de o envio terminar

- **GIVEN** que o bot começou a enviar uma resposta e o gateway ainda não confirmou o envio
- **WHEN** o eco da resposta chega ao webhook com `fromMe=true`
- **THEN** a trava de 20 minutos não é ativada

#### Scenario: dono escreve para cliente identificado por @lid

- **GIVEN** que o cliente é identificado pelo bot como `@lid` e pelo número do celular
- **WHEN** a pessoa da empresa escreve para esse cliente e depois o cliente responde
- **THEN** a trava de 20 minutos vale para o cliente, qualquer que seja o identificador recebido
- **AND** a trava não é gravada no número da empresa
- **AND** a factory não é consultada

#### Scenario: nova mensagem do dono renova a trava

- **GIVEN** que a pessoa da empresa escreveu ao cliente há 15 minutos
- **WHEN** ela escreve de novo
- **THEN** a trava passa a valer por 20 minutos a partir da nova mensagem

#### Scenario: cliente envia mensagem durante a pausa do proprietário

- **GIVEN** que o dono respondeu ao cliente há menos de 20 minutos
- **WHEN** o cliente envia uma nova mensagem
- **THEN** a API responde 200
- **AND** o observador registra filtro contendo "proprietário atendeu nos últimos 20 minutos"
- **AND** a factory não é consultada

#### Scenario: gateway entrega a mensagem do dono

- **GIVEN** a configuração padrão do gateway
- **WHEN** a pessoa da empresa digita uma mensagem no celular ou no WhatsApp Web
- **THEN** o gateway posta no webhook o evento `onselfmessage` com `fromMe=true`, `to` do cliente e o texto

#### Scenario: mensagem antiga descartada

- **WHEN** o webhook chega com timestamp com mais de 2 minutos de atraso
- **THEN** a API responde 200
- **AND** o observador registra filtro contendo "Mensagem antiga descartada"
- **AND** a factory não é consultada

#### Scenario: grupo ou tipo que não é chat

- **WHEN** o remetente é grupo (`@g.us`) ou o tipo não é chat (exemplo: `image`)
- **THEN** a API responde 200
- **AND** o observador registra filtro contendo "ignorada"
- **AND** a factory não é consultada

#### Scenario: QR Code

- **WHEN** o evento é `qrcode` com o código da sessão
- **THEN** a API responde 200
- **AND** o QR é guardado para aquela sessão
- **AND** a factory não é consultada

#### Scenario: conexão, presença e ack

- **WHEN** o evento é `status-find`, `onpresencechanged` ou `onack`
- **THEN** a API responde 200
- **AND** a factory não é consultada
