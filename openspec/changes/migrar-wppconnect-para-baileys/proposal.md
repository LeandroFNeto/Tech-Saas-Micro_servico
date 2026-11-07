# Proposal

## Why

O WPPConnect abre um Chromium (Puppeteer) por sessão e é o maior consumidor de RAM do servidor de 8 GB. Cada empresa nova conectada soma centenas de MB, e o `shm_size` de 2 GB do compose já é um sintoma disso. Trocar o provedor por Baileys, que fala direto por WebSocket em um único processo Node, libera memória para mais empresas por servidor e tira o Chromium da operação. A migração entra neste mês, antes de o limite de sessões virar problema.

## What Changes

- Novo serviço `baileys-gateway` (Node), que expõe a mesma API HTTP que o backend já usa do WPPConnect (`generate-token`, `start-session`, `status-session`, `qrcode-session`, desconectar, envio de mensagem) e chama o webhook com o mesmo formato de payload (`WhatsappWebhookDTO`). O backend Java não muda de lógica; só aponta para o gateway.
- O gateway traduz o formato do Baileys (incluindo contatos `@lid`) para o `from` e o `session` que o webhook espera.
- Estado de autenticação de cada sessão persiste em volume Docker próprio.
- O container `wppconnect` e o `wppconnect-server/` saem do `docker-compose.yml` ao fim da virada, depois da janela de reversão.
- **BREAKING**: as sessões atuais do WPPConnect (`dados-wpp/tokens` e `userDataDir`) não migram. Cada empresa e a atendente da Gamb escaneiam o QR de novo.
- A versão do Baileys fica fixada, sem atualização automática.
- Medição de RAM antes e depois (`docker stats`) registrada como prova do ganho.

## Capabilities

### New Capabilities

Nenhuma. O gateway é troca de implementação por trás do contrato que já existe.

### Modified Capabilities

- `whatsapp`: o requisito de conexão da sessão passa a exigir novo pareamento por QR na virada e a descrever o provedor Baileys; o contrato de webhook, o roteamento e as strategies não mudam.

## Impact

- **Novo:** diretório do gateway Node, serviço no `docker-compose.yml`, volume de autenticação, variáveis de ambiente do gateway.
- **Java:** `ServicoMensagem` tem `http://wppconnect:21465` escrito no código e passa a ler `wppconnect.url`; `ServicoWppConnect` e `application.properties` mantêm as propriedades `wppconnect.*`. Sem mudança em strategies nem em `ControllerWhatsapp`.
- **Operação:** reparear todas as sessões, janela combinada com cada empresa, plano de reversão para o container antigo.
- **Dependência:** `autenticar-webhook-whatsapp` fica como está; o gateway deve enviar o cabeçalho de segredo se essa change for aplicada.
- **Fora de escopo:** refatorar o Java para a interface `ProvedorWhatsapp`, migrar sessões existentes, suporte a mais de um provedor ao mesmo tempo.
