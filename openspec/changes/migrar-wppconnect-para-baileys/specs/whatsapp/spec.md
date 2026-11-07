# Spec Delta: Migração Baileys mantendo Paridade Integral com WPPConnect

## ADDED Requirements

### Requirement: WPP-17 Garantia de paridade funcional de gateway entre WPPConnect e Baileys
O sistema SHALL responder através do gateway Baileys com a mesma interface REST de chamadas de saída e a mesma estrutura de webhook de entrada anteriormente provida pelo WPPConnect, garantindo que controladores, filtros, serviços de mensageria e estratégias de negócio permaneçam inalterados e plenamente funcionais.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`, `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/resources/features/webhook.feature`

#### Scenario: Envio de mensagem com cabeçalho de segredo autenticado
- **GIVEN** que o gateway Baileys recebe uma mensagem do WhatsApp
- **WHEN** o gateway dispara o webhook para o backend Spring Boot
- **THEN** o cabeçalho `x-webhook-token` com o segredo compartilhado configurado é enviado na requisição HTTP
- **AND** o `WebhookSecretFilter` aceita a chamada com status 200

#### Scenario: Notificação de mensagem do proprietário (Anti-X9)
- **GIVEN** que o dono da empresa envia uma mensagem para um cliente através do WhatsApp móvel ou Web
- **WHEN** o socket Baileys detecta a mensagem emitida com `fromMe=true`
- **THEN** o gateway despacha o webhook com `event=onselfmessage`, `fromMe=true` e o destinatário `to`
- **AND** o backend ativa a trava de proprietário de 20 minutos para aquele contato, silenciando o robô

#### Scenario: Mensagem com atraso temporal
- **GIVEN** que o gateway recebe do WhatsApp uma mensagem com timestamp antigo
- **WHEN** a mensagem é entregue no webhook com `timestamp` superior a 120 segundos de atraso
- **THEN** o backend descarta a mensagem como histórica sem acionar nenhuma strategy

### Requirement: WPP-18 Ciclo de vida da sessão e pareamento por QR Code
O sistema SHALL expor o status de sessão compatível com os estados `CONNECTED`, `QRCODE` e `DISCONNECTED`, gerando imagem PNG do QR Code e permitindo auto-reconexão persistente no reinício do container.

Contrato: `especificacao/back/paths/whatsapp-status.yaml`, `especificacao/back/paths/whatsapp-iniciar.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/java/com/example/demo/config/RestauradorSessoesWhatsappTest.java`

#### Scenario: Consulta de sessão em pareamento
- **GIVEN** uma sessão iniciada que ainda não foi pareada no Baileys
- **WHEN** o painel consulta o status da sessão
- **THEN** o gateway responde status `QRCODE` acompanhado da representação base64 ou endpoint PNG
- **AND** a leitura do QR Code pelo aplicativo comuta imediatamente o status para `CONNECTED`

#### Scenario: Restauração de sessão após reinício
- **GIVEN** uma sessão previamente autenticada e gravada no volume persistente
- **WHEN** o serviço `baileys-gateway` é reiniciado
- **THEN** o socket reconecta automaticamente com as credenciais salvas sem exigir novo escaneamento de QR Code
