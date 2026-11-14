# Tasks: Migração para Baileys Garantindo Paridade Total

## 1. Fundação e Estrutura do Gateway
- [x] 1.1 Mapeamento e matriz de paridade em `docs/mapeamento-funcoes-migracao-baileys.md`, conferidos contra `ServicoWppConnect`, `ServicoMensagem`, `ControllerWhatsapp`, `WhatsappWebhookDTO`, `RestauradorSessoesWhatsapp`, `ControllerEmpresa` e o fork `wppconnect-server/` (`config.ts`, `util/functions.ts`, `middleware/statusConnection.ts`, `util/createSessionUtil.ts`)
- [x] 1.2 Criar `baileys-gateway/` com `package.json`, `@whiskeysockets/baileys` em versão fixa, TypeScript, gerador de PNG de QR, logger `pino` e `Dockerfile`
- [x] 1.3 Configurar `baileys-gateway` no `docker-compose.yml` com volume `baileys-auth` e as variáveis `SECRET_KEY`, `WPPCONNECT_WEBHOOK_URL`, `WPPCONNECT_WEBHOOK_SECRET` e `WPPCONNECT_ON_SELF_MESSAGE`, com os mesmos valores de reserva do serviço `wppconnect`; o gateway não sobe sem segredo do webhook; `restart: unless-stopped`, `healthcheck` na rota de saúde e `depends_on` do Java no gateway
- [x] 1.4 Manter o serviço `wppconnect` sob `profiles: ["backup"]`, com `dados-wpp/userDataDir` e `dados-wpp/tokens` intactos, e deixar o `baileys-gateway` sem porta publicada no host

## 2. API REST Compatível
- [x] 2.1 `POST /api/:session/:secret/generate-token` com comparação em tempo constante, token sem estado `HMAC-SHA256(SECRET_KEY, sessão)` que sobrevive ao reinício do gateway, resposta `{ status, token }` e 401 para segredo errado; demais rotas com `Bearer` da sessão e 401 para token ausente, inválido ou de outra sessão
- [x] 2.2 `POST /api/:session/start-session` lendo `webhook` do corpo (reserva `WPPCONNECT_WEBHOOK_URL`), idempotente para sessão conectada ou aguardando QR, com autenticação em arquivos com gravação atômica (mesmo layout) e webhook restrito ao host de `WPPCONNECT_WEBHOOK_URL` mais `BAILEYS_HOSTS_WEBHOOK_PERMITIDOS`
- [x] 2.3 `GET /api/:session/status-session` e `GET /api/:session/qrcode-session` com `CONNECTED`, `QRCODE`, `INITIALIZING` e `DISCONNECTED`, QR em PNG base64 e bytes `image/png` (JSON quando não há QR)
- [x] 2.4 `POST /api/:session/send-message` com `phone` só em dígitos: `@lid` por `isLid`, sufixo ou 14 dígitos ou mais; senão `onWhatsApp` para o JID real; 400 para número inexistente, 404 para sessão desconectada, 2xx ao entrar na fila (3A.1)
- [x] 2.5 `POST /api/:session/send-image` baixando `path` http(s) ou lendo `data:` base64, com `caption`, mesmo destino e mesmos códigos de erro do 2.4
- [x] 2.6 `POST /api/:session/close-session` encerrando o socket, fazendo logout e apagando `/auth-data/:session`

## 3. Webhook e Comportamentos WPP-01 a WPP-16
- [x] 3.1 Despachar o webhook com `x-webhook-token` (WPP-16), só para `messages.upsert` do tipo `notify`, só de conversas `@s.whatsapp.net` e `@lid` (ignorando `status@broadcast`, `@broadcast`, grupo, canal, comunidade e `@bot`) e sem marcar como lida
- [x] 3.2 Montar o payload com `event`, `session`, `id`, `fromMe`, `timestamp` em segundos (convertendo o `Long` do Baileys), `from`, `to`, `type` (`chat` para texto, inclusive desembrulhado de efêmera, resposta e visualização única; tipo da mídia nos demais), `body` (texto ou legenda), `isGroupMsg`, `sender { id, pushname }` e `notifyName`, com todo JID normalizado para `<num>@c.us` sem o `:dispositivo`; `pushName` do dono nunca vai no payload
- [x] 3.3 `onselfmessage` para toda mensagem real (texto ou mídia) com `fromMe=true`, com `to` do cliente; reação, edição, apagar, enquete, sincronização e chamada não geram evento; o eco do próprio envio é descartado pelo id da mensagem enviada pelo gateway; desligável por `WPPCONNECT_ON_SELF_MESSAGE=false` (WPP-02)
- [x] 3.4 `@lid`: `from` com o `@lid` e `sender.id` com o número `@c.us` quando o Baileys informar (`senderPn` na versão fixa 6.7.24; `remoteJidAlt` é da 7.x) (WPP-02/03)
- [x] 3.5 Eventos `qrcode` (PNG base64) e `status-find` no formato que `ControllerWhatsapp` já trata
- [x] 3.6 Ciclo de vida: reconexão no boot, QR renovado até parear ou fechar, reconexão por motivo de queda (rede e servidor com espera de 2 s a 60 s sem desistir; `restartRequired` na hora; `connectionReplaced` sem laço; `loggedOut`, `badSession` e `multideviceMismatch` apagam as credenciais), `INITIALIZING` durante a reconexão e `start-session` que nunca gera QR novo com credenciais válidas
- [x] 3.9 Persistência: `meta.json` com a URL do webhook ao lado das credenciais, gravação atômica com a última versão boa guardada e reconexão no boot sem depender do Java
- [x] 3.7 Logs sem número completo, texto, token, segredo ou QR, com logger do Baileys em `warn` (WPP-06)
- [x] 3.8 Ignorar a conversa com o próprio número ("você com você") e `fromMe` com `to` de grupo, sem webhook

## 3A. Melhorias só no gateway (WPP-19)
- [x] 3A.1 Envio em duas fases: `send-message` e `send-image` validam na hora (token, sessão, número, download da foto) e respondem 2xx ao entrar na fila; envio aceito que falha com o socket caído é tentado de novo na reconexão em até 60 s e depois descartado com log sem dado pessoal
- [x] 3A.2 Fila por sessão em memória, um envio por vez, intervalo aleatório de 1 a 2,5 s, 503 acima de 50 envios na fila
- [x] 3A.3 "Digitando…" com `sendPresenceUpdate('composing')` por 40 ms por caractere (0,8 s a 3 s; foto 1 s) e `paused` depois, desligável por `BAILEYS_SIMULAR_DIGITACAO=false`
- [x] 3A.4 Lista dos ids postados nos últimos 10 min (até 5000) para não repostar mensagem reentregue, e lista dos ids enviados pelo próprio gateway para descartar o eco, sem depender da janela de 2 min do Java
- [x] 3A.5 `sender.id` preenchido pelos pares `@lid`/número vistos nos eventos (e pelo mapa `lidMapping` quando a versão fixa o informar; é da 7.x), guardados no `MapaRecente` (`src/recentes.ts`: em memória, validade de 24 h, teto de 5000 entradas), preferindo o número ao `@lid` para não duplicar o contato
- [x] 3A.6 Nova tentativa do webhook em erro de rede ou 5xx (1 s, 2 s, 4 s), sem repetir em 4xx
- [x] 3A.7 `GET /api/health` com `Authorization: Bearer <SECRET_KEY>` (sem segredo na URL), comparação em tempo constante e só contagens e nomes de sessão, inclusive `nomesConectadas`

## 4. Backend Java
- [x] 4.1 Conferir que o Java não precisa mudar: `ServicoMensagem` e `ServicoWppConnect` já leem `wppconnect.url` e enviam o destino só em dígitos com `isLid`
- [x] 4.2 Validar que `RestauradorSessoesWhatsapp` restaura as sessões no gateway Baileys sem socket duplicado
- [x] 4.2a `ServicoWppConnect.consultarSaude()` (rota `/api/health` com cabeçalho Bearer) e leitura no `DiagnosticoModulosService`: indisponível sem resposta, degradado com sessão de empresa ativa fora de `nomesConectadas` (aguardando QR, reconectando ou desconhecida; nomes, sem número; campo ausente em gateway antigo tolerado; `INITIALIZING` conta como desconectada e pode dar `DEGRADADO` por segundos na reconexão), operacional com a mensagem "gateway sem rota de saúde" em 404; verificar com `DiagnosticoModulosServiceTest`
- [x] 4.2b Contrato `especificacao/back/paths/whatsapp-codigo-pareamento.yaml`, schemas `CodigoPareamentoRequestDTO` e `CodigoPareamentoResponseDTO` e `$ref` em `swagger.yaml`, antes do Java
- [x] 4.2c `POST /whatsapp/{sessao}/codigo-pareamento` em `ControllerWhatsapp` e `ServicoWppConnect.solicitarCodigoPareamento()`, com a autorização de iniciar, 400 para telefone fora de 12 ou 13 dígitos com DDI 55, 403, 409, 429 e 503 repassados, telefone não gravado e mascarado no log; verificar com `ControllerWhatsappTest`
- [x] 4.2d Gateway: `POST /api/:session/request-pairing-code` só com a sessão aguardando pareamento, 409 conectada, 429 acima de 3 pedidos em 10 min
- [x] 4.2f Status `INITIALIZING`: contrato de `SessaoStatusResponseDTO` (enum de status) atualizado antes; `ServicoWppConnect.normalizarStatus` e `dispararStartSession` repassam `INITIALIZING` em vez de `QRCODE`, sem QR; verificar com `ControllerWhatsappTest` e `ServicoWppConnectTest`
- [x] 4.2e Tela do código de telefone: movida para a change `conexao-whatsapp-por-codigo` (código como caminho principal, passo a passo, canvas e Angular); esta change entrega 4.2b a 4.2d
- [x] 4.3 Rodar a suíte completa (`mvn test` e `scripts/auditar-kirito.ps1 -ComRegressao`) com 100% de aprovação

## 5. Testes de Contrato e Homologação
- [x] 5.1 Testes de contrato no `baileys-gateway` que transformam eventos do Baileys nos mesmos payloads usados em `ControllerWhatsappTest` e `webhook.feature` (cliente, dono com texto e com foto, eco, `@lid` com número, grupo, `status@broadcast`, histórico, QR), e dos casos do Baileys: JID com `:dispositivo`, reação, edição e apagar do dono e do cliente, texto efêmero, resposta e visualização única, canal, comunidade, `@bot` e `@broadcast`, conversa com o próprio número, `fromMe` em grupo, eco descartado por id (inclusive após 2 min), `pushName` do dono fora do payload e `timestamp` em `Long`
- [x] 5.2 Testes de contrato da API REST: token e 401, `start-session` repetido, QR em PNG, destino `@lid` e por `onWhatsApp`, 400 e 404 no envio, `send-image` por URL, `close-session` apagando credenciais, resposta antes do envio, ordem da fila e 503, digitação, ids repetidos, nova tentativa do webhook, saúde e código de pareamento
- [ ] 5.3 Ponta a ponta local com a sessão de teste da Gamb: QR no painel, `CONNECTED`, mensagem do cliente atendida, nome do contato gravado, opt-out com "SAIR", dono digitando liga a trava, o dono escrevendo num chat @lid, reação do dono não liga, envio do bot não liga, conferir quais mensagens do celular chegam como `notify` ou `append`, número com `:dispositivo` gravado certo e foto da galeria chega
- [ ] 5.4 Reiniciar o container, parar o servidor por alguns minutos, derrubar a rede do celular e conferir a restauração sem novo QR, o mesmo token valendo depois do reinício e o status `INITIALIZING` durante a reconexão (WPP-18), reiniciando só o gateway, sem reiniciar o Java, depois de parear por QR
- [ ] 5.4a Conferir numa conversa real o "digitando…", a ordem das fotos da galeria e o código de pareamento pedido pela rota do Java; conferir o diagnóstico com o gateway parado e com uma sessão desconectada, o código pedido e não digitado seguido de reconexão, e a latência do `send-image` dentro dos 5 s do Java
- [ ] 5.5 Conferir nos logs do gateway que não aparece número completo nem texto

## 6. Documentação e Plano de Virada
- [x] 6.1 Documentar o gateway e a virada em `docs/baileys-gateway.md`, citando `wppconnect-server/NOTICE` (copyright e licença Apache 2.0 do WPPConnect Team, já que os cabeçalhos dos fontes do fork foram removidos)
- [ ] 6.2 Virar todas as sessões juntas trocando `WPPCONNECT_URL` (um só endereço para todas), avisar cada empresa do novo pareamento por QR, só depois de a tela de `conexao-whatsapp-por-codigo` (WPP-22, tarefa 3.4) estar aplicada (ou aceitando que a tela mostre "Desconectado" durante reconexões), e manter o perfil `backup` pronto para reverter (reversão: parar o `baileys-gateway` com `docker compose stop baileys-gateway` para não responder em dobro, subir o perfil `backup`, gravar `WPPCONNECT_URL=http://wppconnect:21465` no `.env` e rodar `docker compose up -d --no-deps bot-java`)
- [ ] 6.3 Revisar WPP-04, WPP-16 e WPP-21 da spec principal (WPP-21 passa a valer só no perfil `backup`) e sincronizar `openspec/specs/whatsapp/spec.md` com WPP-17 a WPP-20, à mão, convertendo o formato do delta (`### WPP-NN`, "O sistema deve", Contrato/Prova, Cenário com Dado/Quando/Então/E) (os títulos da spec principal não têm o prefixo `Requirement:`), e `openspec/specs/assistente-tutoria/spec.md` com o diagnóstico depois de arquivada a `liberdade-conversa-modulo-ia`; arquivar a change
