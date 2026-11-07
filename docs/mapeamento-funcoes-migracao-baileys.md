# Mapeamento de Funções: Migração WPPConnect para Baileys

Este documento lista formalmente todas as funções, rotas, payloads e contratos implementados no ecossistema WPPConnect que devem ser mantidos e garantidos no novo `baileys-gateway`.

---

## 1. Mapeamento de Rotas HTTP Consumidas pelo Java (Saída)

O backend Spring Boot (`ServicoWppConnect` e `ServicoMensagem`) interage exclusivamente com as seguintes rotas REST:

| Rota no Gateway | Método | Consumidor Java | Payload / Parâmetros | Resposta Esperada | Comportamento Obrigatório |
|---|---|---|---|---|---|
| `/api/{session}/{secretKey}/generate-token` | `POST` | `ServicoWppConnect.obterToken` | Path params: `session`, `secretKey` | `{ "status": "Success", "token": "<jwt-token>" }` | Compara `secretKey` em tempo constante com `WPPCONNECT_SECRET_KEY`; se inválido, responde `401`. |
| `/api/{session}/start-session` | `POST` | `ServicoWppConnect.iniciarSessao` | Body: `{ "webhook": "<url>", "waitQrCode": false }` | `{ "status": "CONNECTED" \| "QRCODE", "qrcode": "<base64>" }` | Inicia o socket Baileys com `useMultiFileAuthState`. Se já pareado, conecta direto. |
| `/api/{session}/status-session` | `GET` | `ServicoWppConnect.consultarStatus` | Header: `Bearer <token>` | `{ "status": "CONNECTED" \| "QRCODE" \| "DISCONNECTED", "qrcode": "<base64>" }` | Retorna o status normalizado da conexão. |
| `/api/{session}/qrcode-session` | `GET` | `ServicoWppConnect.buscarQrcodePng` | Header: `Bearer <token>`, Accept: `image/png` | Stream de imagem PNG (bytes) | Retorna a imagem do QR Code quando a sessão aguarda pareamento. |
| `/api/{session}/send-message` | `POST` | `ServicoMensagem.enviarMensagemWPP` | Body: `{ "phone": "<num>", "message": "<txt>", "isGroup": false, "isLid": boolean }` | `{ "status": "success", "response": [...] }` | Dispara mensagem de texto simples para cliente individual ou `@lid`. |
| `/api/{session}/send-image` | `POST` | `ServicoMensagem.enviarImagemWPP` | Body: `{ "phone": "<num>", "path": "<url-ou-base64>", "filename": "...", "caption": "..." }` | `{ "status": "success", "response": [...] }` | Baixa a imagem (se URL) ou envia base64 como imagem de WhatsApp com legenda. |
| `/api/{session}/close-session` | `POST` | `ServicoWppConnect.desconectar` | Header: `Bearer <token>` | `{ "status": true, "message": "Session closed" }` | Encerra e desconecta o socket da empresa. |

---

## 2. Mapeamento de Eventos de Entrada no Webhook Java (`POST /webhook/whatsapp`)

O `baileys-gateway` deve postar no `WPPCONNECT_WEBHOOK_URL` seguindo o schema `WhatsappWebhookDTO`:

| Evento / Situação Baileys | Campo `event` | Campo `fromMe` | Estrutura de Payload | Headers Obrigatórios | Regra de Negócio Acionada no Java |
|---|---|---|---|---|---|
| **Dono enviou mensagem no celular** | `"onselfmessage"` | `true` | `session`, `from` (dono), `to` (cliente), `body`, `timestamp` | `x-webhook-token: <secret>` | **Anti-X9 (WPP-02)**: Ativa trava de 20 min no contato cliente para silenciar o bot. |
| **Cliente enviou mensagem de texto** | `"onmessage"` | `false` | `session`, `from` (cliente `@c.us` ou `@lid`), `body`, `type: "chat"`, `timestamp` | `x-webhook-token: <secret>` | **Roteamento (WPP-01/03)**: Encaminha para a strategy correspondente ao ramo da empresa. |
| **Eco do próprio bot** | `"onselfmessage"` ou `"onmessage"` | `true` | `session`, `from`, `to`, `body` (idêntico ao enviado pelo bot há < 2 min) | `x-webhook-token: <secret>` | **Filtro Anti-Eco (WPP-02)**: Ignora sem acionar a trava de proprietário. |
| **Mensagem com atraso > 120s** | Qualquer | Qualquer | `session`, `timestamp` (< now - 120s) | `x-webhook-token: <secret>` | **Filtro Temporal (WPP-02)**: Descarte silencioso com status 200 para evitar respostas a mensagens antigas. |
| **Novo QR Code gerado** | `"qrcode"` | `false` | `session`, `qrcode: "<string-ou-base64>"` | `x-webhook-token: <secret>` | **Cache de QR (WPP-02)**: Guarda QR em memória para consulta imediata do painel. |
| **Evento de status da conexão** | `"status-find"` | `false` | `session`, `status: "inChat" \| "isLogged" \| "autocloseCalled"` | `x-webhook-token: <secret>` | **Conexão (WPP-02)**: Ignorado como ruído de conexão sem acionar atendimento. |
| **Grupo ou tipo inválido** | Qualquer | Qualquer | `from: "*@g.us"` ou `type != "chat"` | `x-webhook-token: <secret>` | **Filtro de Ruído (WPP-02)**: Ignora grupo e tipos não suportados. |

---

## 3. Requisitos de Segurança e Infraestrutura a Manter

1. **Autenticação do Webhook (WPP-16)**:
   - Todo request disparado pelo gateway Baileys deve obrigatoriamente incluir `x-webhook-token: ${WPPCONNECT_WEBHOOK_SECRET}`.
   - Chamadas sem o cabeçalho ou com valor incorreto recebem `401 Unauthorized` do `WebhookSecretFilter`.
2. **Sanitização de Logs (WPP-06)**:
   - Nenhum número de telefone completo, mensagem de cliente ou token deve aparecer em logs (apenas mascarado `***1234` e tamanho da mensagem).
3. **Persistência de Sessões (WPP-18)**:
   - Pasta de autenticação `/auth-data/{session}` montada no volume Docker `baileys-auth`.
   - Reinicializações de container devem restaurar as conexões sem requerer novo QR Code.
4. **Resolução de Contatos `@lid` (WPP-03 / WPP-19)**:
   - O gateway deve mapear contatos `@lid` para seus números telefônicos sempre que exposto pelo protocolo, ou despachar preservando o sufixo para que o Java utilize o apelido registrado.
