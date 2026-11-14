# Proposal: Migração WPPConnect para Baileys com Preservação Integral de Comportamento

## Why

O WPPConnect abre uma instância de Chromium (Puppeteer) por sessão, tornando-se o gargalo crítico de consumo de memória RAM no VPS de 8 GB (exigindo `shm_size: 2gb` e limitando a adição de novas empresas clientes). A substituição do motor para a biblioteca Baileys, que conecta diretamente via WebSocket em um único processo leve Node.js, reduz drasticamente o consumo para poucas dezenas de megabytes por sessão.

**Garantia Central Desta Proposta**: A migração **não pode quebrar nem regredir nenhuma funcionalidade** atualmente em produção no WPPConnect. Todos os comportamentos implementados e blindados serão integralmente preservados pelo novo gateway:
- trava do proprietário Anti-X9 de 20 min com renovação, ligada por mensagem do dono de qualquer tipo via `onselfmessage`, desligável por `WPPCONNECT_ON_SELF_MESSAGE`;
- tolerância a ecos do próprio bot, inclusive quando o eco chega antes de o envio terminar;
- descarte de mensagens com atraso > 2 min, sem reenvio do histórico na reconexão;
- JIDs `@lid` com apelido para o número, destino `@lid` por `isLid` ou 14 dígitos, e checagem do número com ou sem o nono dígito antes do envio;
- nome de exibição do cliente no payload, usado pela coleta de contatos com proteção LGPD, e opt-out por mensagem;
- autenticação do webhook com `x-webhook-token` e das rotas da sessão por token, com 401 para o Java renovar;
- status `CONNECTED`, `QRCODE`, `INITIALIZING` (reconectando) e `DISCONNECTED`, QR Code como imagem PNG, restauração no boot e encerramento na exclusão da empresa;
- envio de texto e de fotos pela URL cadastrada, com erro HTTP na validação (sessão, token, número), do qual dependem lembretes, opt-out e reunião;
- logs sem número completo nem texto de mensagem.

## What Changes

- Criação do serviço `baileys-gateway` (Node.js + TypeScript) em container Docker independente, expondo **exatamente a mesma interface HTTP REST** que o backend Java consome do WPPConnect:
  - `POST /api/{session}/{secret}/generate-token`: autenticação da sessão com token Bearer.
  - `POST /api/{session}/start-session`: inicialização do socket Baileys com suporte a webhook.
  - `GET /api/{session}/status-session`: consulta de status padronizada (`CONNECTED`, `QRCODE`, `INITIALIZING` e `DISCONNECTED`).
  - `GET /api/{session}/qrcode-session`: retorno do QR Code em PNG.
  - `POST /api/{session}/send-message`: envio de texto para o destino em dígitos com `isLid`.
  - `POST /api/{session}/send-image`: envio de foto pela URL cadastrada (ou base64) com legenda.
  - `POST /api/{session}/close-session`: encerramento da conexão, com logout e remoção das credenciais.
- **Preservação do Webhook e Eventos**: O gateway Baileys despacha requisições HTTP para `POST /webhook/whatsapp` com o payload exato esperado pelo backend Java (`WhatsappWebhookDTO`):
  - Inclusão obrigatória do cabeçalho `x-webhook-token: ${WPPCONNECT_WEBHOOK_SECRET}` (atendendo ao requisito `WPP-16` / `WebhookSecretFilter`).
  - Emissão do evento `onselfmessage` com `fromMe: true` quando o proprietário digita no celular, garantindo a ativação imediata da trava de 20 min Anti-X9 (`WPP-02`).
  - Preservação de timestamp Unix em segundos para que o filtro de mensagens com atraso > 120s funcione com precisão cirúrgica.
  - Suporte a mensagens recebidas com identificadores alternativos `@lid`, mapeando-os para o número correspondente ou repassando no formato que o backend já sabe tratar.
- **Melhorias que o Baileys permite, só no gateway**: "digitando…" antes de cada resposta, fila de envio por sessão com intervalo aleatório para reduzir o risco de bloqueio do número, envio em duas fases para caber nos 5 s de timeout do Java, descarte de mensagem repetida na reconexão, `@lid` convertido em número pelo mapa do próprio Baileys e nova tentativa curta no webhook quando o Java está fora do ar.
- **Melhorias com ajuste pequeno no Java**:
  - rota de saúde no gateway lida pelo diagnóstico do admin, que passa a mostrar gateway fora do ar e sessões desconectadas;
  - pareamento por código de telefone como alternativa ao QR, com rota nova `POST /whatsapp/{sessao}/codigo-pareamento`; a tela (código como caminho principal e passo a passo) é da change `conexao-whatsapp-por-codigo`.
- **Reconexão automática**: token sem estado (`HMAC-SHA256(SECRET_KEY, sessão)`), webhook gravado em `meta.json`, gravação atômica das credenciais, reconexão por motivo de queda, status `INITIALIZING` e volta sozinho depois de reinício, queda de rede ou celular sem bateria. O limite de 14 dias do celular sem internet fica documentado; o aviso ao cliente é da change `alertas-desconexao-whatsapp`.
- **Isolamento e Persistência**: Autenticação em arquivos com gravação atômica (mesmo layout do Multi-File Auth) persistida no volume Docker dedicado `baileys-auth`, permitindo auto-reconexão e restauração automática das empresas após reinicialização do servidor (`WPP-18`).
- **Virada Segura e Reversível**: O container `wppconnect` e seus dados são mantidos intactos durante o período de homologação, permitindo reversão imediata trocando `WPPCONNECT_URL` no `.env`, com o perfil `backup` para reverter (`--no-deps`). Como o Java usa um único `WPPCONNECT_URL`, a virada é de todas as sessões ao mesmo tempo, com novo QR em cada número.

## Capabilities

### Modified Capabilities

- `whatsapp`: o provedor subjacente passa a ser o Baileys; todos os requisitos existentes (`WPP-01` a `WPP-16`) mantêm-se 100% satisfeitos e válidos, e ganham a cobertura formal dos novos `WPP-17` (paridade do gateway) e `WPP-18` (ciclo de vida da sessão), mais `WPP-19` (envio com digitação, fila e entrega robusta) e `WPP-20` (pareamento por código de telefone).
- `assistente-tutoria`: o diagnóstico passa a ler a saúde do gateway e das sessões. Depende da change `liberdade-conversa-modulo-ia`, que altera o mesmo requisito e deve ser arquivada antes.
- `contatos` e `ia`: sem mudança de requisito; a coleta de contatos, o opt-out e a conversa livre seguem iguais porque o payload do gateway não muda.

## Impact

- **Código Java Backend**: nenhuma regra de negócio ou strategy muda, e `ServicoMensagem`/`ServicoWppConnect` já leem `wppconnect.url`. Entram só a rota e os DTOs do código de pareamento em `ControllerWhatsapp`, `consultarSaude()` e `solicitarCodigoPareamento()` em `ServicoWppConnect` e a leitura da saúde em `DiagnosticoModulosService`, com contrato YAML antes do Java.
- **Frontend Angular**: sem mudança nesta change; a tela do código de telefone é da change `conexao-whatsapp-por-codigo`. O status `INITIALIZING` (reconectando) passa a ser repassado pelo Java (ajuste pequeno em `ServicoWppConnect`, que hoje o converte em `QRCODE`) e o "reconectando" na tela é entregue por aquela change (`conexao-whatsapp-por-codigo`, WPP-22, tarefa 3.4); até lá o painel mostra "Desconectado" durante a reconexão.
- **Infraestrutura**: Redução drástica de consumo de RAM no VPS (de ~2-3 GB para menos de 150 MB).
- **Operação**: Devido à mudança do formato interno de sessão do Chromium/Puppeteer para o protocolo binário do Baileys, haverá um pareamento único via QR Code na virada.
