# Design: Gateway Baileys com Preservação Estrita do Comportamento do WPPConnect

## Context

O backend Spring Boot integra-se ao WhatsApp através de duas vias estritas:
1. **Chamadas de Saída**: O `ServicoWppConnect` e `ServicoMensagem` chamam endpoints HTTP REST do gateway (`generate-token`, `start-session`, `status-session`, `qrcode-session`, `send-message`, `send-image`, `close-session`).
2. **Entrada de Eventos (Webhook)**: O gateway posta em `POST /webhook/whatsapp` com o DTO `WhatsappWebhookDTO`.

O repositório já conta com 702 testes no Backend (incluindo BDD Cucumber e MockMvc) e 200 specs no Frontend que protegem o comportamento do sistema contra qualquer regressão.

## Goals / Non-Goals

### Goals
- **Compatibilidade 100% Drop-in**: O backend Java e o painel Angular não devem notar a troca de motor; contratos YAML de API e schemas permanecem inalterados.
- **Preservação dos Recursos Blindados do WhatsApp**:
  - `WPP-02`: Anti-X9 e trava do proprietário de 20 min disparada por `onselfmessage` com `fromMe: true`.
  - `WPP-02`: Descarte de mensagens com atraso de relógio superior a 120s.
  - `WPP-02`: Ignorar ecos do próprio bot sem acionar a trava.
  - `WPP-03`: Suporte a contatos `@lid` e números móveis com ou sem dígito 9.
  - `WPP-04`: Status padronizados (`CONNECTED`, `QRCODE`, `DISCONNECTED`).
  - `WPP-06`: Logs estritamente sanitizados sem dados pessoais ou segredos.
  - `WPP-16`: Autenticação obrigatória do webhook via cabeçalho `x-webhook-token`.
- **Economia Significativa de Recursos**: Processo único em Node.js com WebSocket puro, sem instanciar Chromium ou consumir memória gráfica virtual.
- **Transição Segura e Reversível**: Possibilidade de rollback para o WPPConnect a qualquer momento durante a fase piloto.

### Non-Goals
- Não alterar as strategies de negócio (`LocacaoStrategy`, `AtendenteGambStrategy`, etc.).
- Não migrar arquivos de cookies/sessão antigos do Puppeteer (incompatíveis tecnicamente com as chaves criptográficas do protocolo Baileys; requer novo pareamento por QR Code).

## Decisions & Architecture

```
[Cliente WhatsApp] <=== WebSocket ===> [baileys-gateway (Node.js)]
                                              |
      +---------------------------------------+---------------------------------------+
      | (Chamadas REST de Saída)                                                      | (Webhook de Entrada)
      | - /generate-token                                                             | - POST /webhook/whatsapp
      | - /start-session                                                              | - Header: x-webhook-token
      | - /status-session                                                             | - Event: onmessage / onselfmessage
      | - /send-message                                                               | - Payload: WhatsappWebhookDTO
      v                                                                               v
[Backend Java (Spring Boot)] <--------------------------------------------------------+
```

### 1. Mapeamento Estrito de Endpoints REST
O `baileys-gateway` responderá exatamente aos paths:
- `POST /api/:session/:secret/generate-token`: Valida `secret` contra `WPPCONNECT_SECRET_KEY` em tempo constante e devolve `{ token: "<jwt-ou-hash>", status: "success" }`.
- `POST /api/:session/start-session`: Cria o socket `makeWASocket` com `useMultiFileAuthState`. Guarda o webhook configurado.
- `GET /api/:session/status-session`: Retorna `{ session, status: "CONNECTED" | "QRCODE" | "DISCONNECTED", qrcode: "<base64>" }`.
- `GET /api/:session/qrcode-session`: Retorna buffer PNG do QR Code atual.
- `POST /api/:session/send-message`: Envia texto com corpo `{ phone: "<numero>@c.us", message: "<texto>" }`.
- `POST /api/:session/send-image`: Envia mídia com corpo `{ phone: "<numero>@c.us", path: "data:<mime>;base64,<dados>", filename: "..." }`.

### 2. Emissão de Eventos e Tradução no Webhook
Quando o socket Baileys recebe um evento:
- **Mensagem Recebida (`messages.upsert`)**:
  - Se for enviada pelo próprio número conectado (`key.fromMe == true`):
    - O gateway monta `WhatsappWebhookDTO` com `event: "onselfmessage"`, `fromMe: true`, `session: sessionName`, `from: senderNumber`, `to: targetContactNumber`, `body: textMessage`, `timestamp: messageTimestampInSeconds`.
    - Isso aciona no Java o comportamento do **Anti-X9** (`WPP-02`), ativando a trava de 20 minutos no cliente destinatário.
  - Se for enviada por cliente (`key.fromMe == false`):
    - O gateway monta `WhatsappWebhookDTO` com `event: "onmessage"`, `fromMe: false`, `session: sessionName`, `from: senderJid` (normalizado para `@c.us` ou mantendo `@lid`), `body: textMessage`, `timestamp: messageTimestampInSeconds`.
- **Cabeçalho de Autenticação**:
  - Toda chamada HTTP `POST` do gateway para `WPPCONNECT_WEBHOOK_URL` inclui o header:
    `x-webhook-token: ${WPPCONNECT_WEBHOOK_SECRET}`.
- **Eventos de Conexão e QR Code**:
  - Ao gerar QR Code: emite evento `qrcode` com campo `qrcode: "<string>"`.
  - Ao conectar: emite evento de status para persistência.

### 3. Persistência de Sessões
- O estado de autenticação de cada sessão reside em `/auth-data/:session` montado no volume Docker persistente `baileys-auth`.
- Na inicialização do container, o gateway percorre as pastas de sessões já pareadas e executa o auto-reconectamento em background (`WPP-18`).

### 4. Gestão de Risco e Plano de Reversão
- O serviço `wppconnect` original é mantido com `profiles: ["backup"]` no `docker-compose.yml`.
- Se qualquer instabilidade ocorrer no piloto, basta reverter as variáveis `WPPCONNECT_URL` e `WPPCONNECT_WEBHOOK_URL` no `docker-compose.yml` para apontar de volta ao WPPConnect, cujos tokens legados em `dados-wpp` permanecerão intactos.
