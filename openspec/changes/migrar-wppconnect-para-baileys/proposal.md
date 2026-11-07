# Proposal: Migração WPPConnect para Baileys com Preservação Integral de Comportamento

## Why

O WPPConnect abre uma instância de Chromium (Puppeteer) por sessão, tornando-se o gargalo crítico de consumo de memória RAM no VPS de 8 GB (exigindo `shm_size: 2gb` e limitando a adição de novas empresas clientes). A substituição do motor para a biblioteca Baileys, que conecta diretamente via WebSocket em um único processo leve Node.js, reduz drasticamente o consumo para poucas dezenas de megabytes por sessão.

**Garantia Central Desta Proposta**: A migração **não pode quebrar nem regredir nenhuma funcionalidade** atualmente em produção no WPPConnect. Todos os comportamentos implementados e blindados (trava do proprietário Anti-X9 de 20 min com renovação, captura de mensagens do dono via `onselfmessage`, descarte de mensagens com atraso > 2 min, suporte a JIDs `@lid` e normalização para número móvel sem dígito 9 extra, autenticação do webhook com segredo compartilhado via header `x-webhook-token`, resposta com status `CONNECTED`, `QRCODE` e `DISCONNECTED`, envio de texto e fotos, e tolerância a ecos de mensagens disparadas pelo próprio bot) serão integralmente preservados pelo novo gateway.

## What Changes

- Criação do serviço `baileys-gateway` (Node.js + TypeScript) em container Docker independente, expondo **exatamente a mesma interface HTTP REST** que o backend Java consome do WPPConnect:
  - `POST /api/{session}/{secret}/generate-token`: autenticação da sessão com token Bearer.
  - `POST /api/{session}/start-session`: inicialização do socket Baileys com suporte a webhook.
  - `GET /api/{session}/status-session`: consulta de status padronizada (`CONNECTED`, `QRCODE`, `DISCONNECTED`).
  - `GET /api/{session}/qrcode-session`: retorno do QR Code em PNG.
  - `POST /api/{session}/send-message`: envio de mensagens de texto.
  - `POST /api/{session}/send-image`: envio de arquivos de imagem em base64.
  - `POST /api/{session}/close-session`: encerramento da conexão.
- **Preservação do Webhook e Eventos**: O gateway Baileys despacha requisições HTTP para `POST /webhook/whatsapp` com o payload exato esperado pelo backend Java (`WhatsappWebhookDTO`):
  - Inclusão obrigatória do cabeçalho `x-webhook-token: ${WPPCONNECT_WEBHOOK_SECRET}` (atendendo ao requisito `WPP-16` / `WebhookSecretFilter`).
  - Emissão do evento `onselfmessage` com `fromMe: true` quando o proprietário digita no celular, garantindo a ativação imediata da trava de 20 min Anti-X9 (`WPP-02`).
  - Preservação de timestamp Unix em segundos para que o filtro de mensagens com atraso > 120s funcione com precisão cirúrgica.
  - Suporte a mensagens recebidas com identificadores alternativos `@lid`, mapeando-os para o número correspondente ou repassando no formato que o backend já sabe tratar.
- **Isolamento e Persistência**: Autenticação Multi-File Auth (`useMultiFileAuthState`) persistida no volume Docker dedicado `baileys-auth`, permitindo auto-reconexão e restauração automática das empresas após reinicialização do servidor (`WPP-18`).
- **Virada Segura e Reversível**: O container `wppconnect` e seus dados são mantidos intactos durante o período de homologação, permitindo reversão imediata apenas alterando a URL no `docker-compose.yml`.

## Capabilities

### Modified Capabilities

- `whatsapp`: o provedor subjacente passa a ser o Baileys; todos os requisitos existentes (`WPP-01` a `WPP-16`) mantêm-se 100% satisfeitos e válidos, e ganham a cobertura formal da nova suíte de testes de compatibilidade de gateway (`WPP-17` a `WPP-20`).

## Impact

- **Código Java Backend**: Nenhuma regra de negócio, strategy ou controller é alterada. Apenas `ServicoMensagem` passa a ler dinamicamente `wppconnect.url` em vez de URL fixa no código.
- **Frontend Angular**: Nenhuma alteração. Os status (`CONNECTED`, `QRCODE`, `DISCONNECTED`), polling e visualização de QR Code permanecem idênticos.
- **Infraestrutura**: Redução drástica de consumo de RAM no VPS (de ~2-3 GB para menos de 150 MB).
- **Operação**: Devido à mudança do formato interno de sessão do Chromium/Puppeteer para o protocolo binário do Baileys, haverá um pareamento único via QR Code na virada.
