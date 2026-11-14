# Design: Gateway Baileys com Preservação Estrita do Comportamento do WPPConnect

## Context

O backend Spring Boot integra-se ao WhatsApp por duas vias:
1. **Chamadas de saída**: `ServicoWppConnect` (token, início, status, QR e encerramento de sessão) e `ServicoMensagem` (texto e imagem) chamam a API REST do gateway em `wppconnect.url`.
2. **Entrada de eventos**: o gateway posta em `POST /webhook/whatsapp` o `WhatsappWebhookDTO`, tratado por `ControllerWhatsapp` na ordem QR → conexão → mensagem antiga → mensagem do dono → ruído → contato e opt-out → lembrete → strategy.

O gateway atual é o fork em `wppconnect-server/` (derivado do WPPConnect Server, Apache 2.0; copyright e licença do WPPConnect Team em `wppconnect-server/NOTICE`), com customizações que fazem parte do comportamento e precisam ser reproduzidas: `onSelfMessage` ligado por `WPPCONNECT_ON_SELF_MESSAGE` (change `ativar-trava-do-dono-no-gateway`), cabeçalho `x-webhook-token` em toda chamada ao webhook (change `autenticar-webhook-whatsapp`), destino `@lid` por `isLid` ou por 14 dígitos ou mais (commit `e71b3e1`), checagem do número antes do envio e `ignore: ['status@broadcast']`, `readMessage: false`, `allUnreadOnStart: false`, `autoClose: 0` e `startAllSession: true`.

A suíte do backend (JUnit, MockMvc e Cucumber) protege o lado Java; a paridade do gateway é provada por testes de contrato do próprio `baileys-gateway` usando os mesmos payloads dos testes Java.

## Goals / Non-Goals

### Goals
- **Troca sem mudar o comportamento existente**: contratos YAML, DTOs, strategies e telas atuais ficam como estão. A troca do motor é `WPPCONNECT_URL` no `.env`, com o perfil `backup` para reverter (`--no-deps`).
- **Aproveitar o Baileys onde o risco é baixo**: no gateway, "digitando…", fila de envio por sessão, descarte de mensagem repetida, `@lid` convertido em número e nova tentativa curta no webhook; com ajuste pequeno no Java, rota de saúde lida pelo diagnóstico e pareamento por código de telefone no painel (seção 5).
- **Manter tudo o que já foi aplicado**: WPP-01 a WPP-16, a coleta de contatos e o opt-out (`contatos`), os lembretes (WPP-10/11), a atendente e a sessão de teste da Gamb (WPP-12 a WPP-15) e o diagnóstico (`assistente-tutoria`).
- **Menos memória**: um processo Node.js com WebSocket, sem Chromium.
- **Reversão imediata** para o WPPConnect durante o piloto.

### Non-Goals
- Mudar regra de negócio, strategy ou contrato HTTP do backend.
- Migrar a sessão do Puppeteer: cada número pareia de novo por QR Code na virada.
- Reproduzir eventos que o Java ignora (`onack`, `onpresencechanged`, reações, enquetes, etiquetas, chamadas): podem não ser emitidos.

## Decisions & Architecture

```
[WhatsApp] <== WebSocket ==> [baileys-gateway (Node.js)]
                                  |                     \
             REST de saída        |                      \  Webhook de entrada
  generate-token, start-session,  |                       \ POST /webhook/whatsapp
  status-session, qrcode-session, |                        \ x-webhook-token
  send-message, send-image,       |                         \ onmessage / onselfmessage / qrcode
  close-session                   v                          v
                         [Backend Java (Spring Boot)] <------+
```

### 1. API REST compatível (o que o Java envia e lê hoje)

- `POST /api/:session/:secret/generate-token`: compara `secret` com `SECRET_KEY` em tempo constante; devolve `{ "status": "success", "token": "<token>" }`. O Java lê só o campo `token`. Segredo errado responde `401`. **O token não depende de estado**: é `HMAC-SHA256(SECRET_KEY, sessão)`, validado em tempo constante e só valendo para a própria sessão. Assim o token que o Java guarda em memória continua válido depois que o gateway reinicia (o WPPConnect também gera o token a partir do segredo); com token só em memória, a primeira mensagem de cada sessão depois de um reinício receberia 401 e se perderia, porque o Java não repete o envio.
- Todas as rotas abaixo exigem `Authorization: Bearer <token>` da sessão. Token ausente, inválido ou de outra sessão responde **HTTP 401**: o Java descarta o token e pede outro ao receber `401` (`consultarStatus`, `iniciarSessao`) e ao ver "401" na falha de envio (`ServicoMensagem`).
- `POST /api/:session/start-session` com `{ "webhook": "<url>", "waitQrCode": false }`: guarda a URL do webhook da sessão (com `WPPCONNECT_WEBHOOK_URL` como reserva) e abre o socket com autenticação em arquivos com gravação atômica (mesmo layout do `useMultiFileAuthState`). Só aceita webhook cujo host seja o de `WPPCONNECT_WEBHOOK_URL` ou esteja em `BAILEYS_HOSTS_WEBHOOK_PERMITIDOS` (lista opcional); outro host responde `400`. É idempotente: sessão já conectada devolve `CONNECTED` sem abrir outro socket, e sessão aguardando QR devolve o QR atual. O Java chama essa rota no boot para toda empresa ativa (`RestauradorSessoesWhatsapp`) e o painel também a chama.
- `GET /api/:session/status-session`: `{ "status": "CONNECTED" | "QRCODE" | "INITIALIZING" | "DISCONNECTED", "qrcode": "<png-base64>" | null }`. O Java aceita também `INCHAT` e `ISLOGGED`, mas o gateway usa só os quatro valores.
- `GET /api/:session/qrcode-session`: bytes PNG com `Content-Type: image/png` quando há QR; sem QR, responde JSON (o Java trata JSON como "sem QR").
- **QR Code sempre como imagem PNG em base64**, com ou sem o prefixo `data:image/png;base64,`, em `start-session`, `status-session` e no evento `qrcode` do webhook. O Baileys entrega o QR como texto (`2@...`); o gateway renderiza a imagem antes de expor. O Java prefixa `data:image/png;base64,` em valor sem prefixo, então texto cru quebraria o QR no painel.
- `POST /api/:session/send-message` com `{ "phone": "<só dígitos>", "message": "<texto>", "isGroup": false, "isLid": <bool> }`: o Java manda só os dígitos, nunca `@c.us`. O gateway monta o destino como o fork faz hoje:
  - `isLid=true`, `@lid` no valor ou 14 dígitos ou mais → `<digitos>@lid`, sem checagem de número;
  - caso contrário, consulta `onWhatsApp` para achar o JID real (com ou sem o nono dígito); número inexistente responde `400`.
  - Sessão desconectada responde `404`. Erro de validação responde 4xx e o envio aceito responde 2xx (envio em duas fases, seção 5.1). O Java depende disso para `tentarEnviarMensagemWPP` devolver `false` (lembretes, opt-out e reunião) e para desfazer o registro anti-eco.
- `POST /api/:session/send-image` com `{ "phone", "isGroup", "isLid", "path", "filename", "caption"? }`: `path` hoje é a **URL http(s) da foto** cadastrada (galeria de até 5 fotos, uma a cada 2 s, controlada pelo Java); o gateway baixa a URL e envia como imagem com a legenda. Também aceita `data:<mime>;base64,`. O container do gateway precisa alcançar as URLs das fotos. O download resolve o host uma vez, recusa IP interno e conecta nesse IP já validado (Host e SNI originais preservados), o que fecha a troca de DNS entre a checagem e a conexão.
- `POST /api/:session/close-session`: chamado quando a empresa é excluída (`ControllerEmpresa`). Encerra o socket, faz logout do aparelho e apaga `/auth-data/:session`, para a sessão não voltar no boot seguinte.

### 2. Webhook de entrada (formato que o `WhatsappWebhookDTO` já lê)

- Destino: URL do `start-session`, com `WPPCONNECT_WEBHOOK_URL` como reserva. Cabeçalho `x-webhook-token` com `WPPCONNECT_WEBHOOK_SECRET` (reserva `SECRET_KEY`) em toda chamada. Sem segredo configurado, o gateway não sobe.
- Só `messages.upsert` do tipo `notify` vira webhook. O histórico sincronizado na conexão (`append`, `messaging-history.set`) não é postado: equivale a `allUnreadOnStart: false`, e o filtro de 120 s do Java fica como segunda barreira.
- **Conversas aceitas**: só `@s.whatsapp.net` e `@lid` viram evento. `status@broadcast`, `@broadcast`, grupo (`@g.us`), canal (`@newsletter`), comunidade e Meta AI (`@bot`) são ignorados no gateway; o Java continua descartando grupo e status como segunda barreira. Conversa com o próprio número ("você com você") e `fromMe` com `to` de grupo também não geram evento.
- **Normalização do JID**: o Baileys entrega `<num>@s.whatsapp.net` e, em aparelhos vinculados, `<num>:<dispositivo>@s.whatsapp.net`. O gateway remove o `:dispositivo` e converte para `<num>@c.us` em `from`, `to` e `sender.id`; o Java extrai os dígitos até o `@` e não trata o sufixo, então `:12` viraria dígitos errados na trava e no contato. `@lid` também perde o sufixo de dispositivo.
- **Quais mensagens viram evento**: só texto e mídia reais. Reação, edição, apagar para todos, enquete, sincronização, chamada e mensagem sem conteúdo (inclusive as do dono) são ignoradas: no WPPConnect eram eventos separados que o Java ignora, e virar `onselfmessage` ligaria a trava por engano. Texto embrulhado em mensagem efêmera, resposta ou visualização única é desembrulhado antes de definir o `type`.
- O gateway não marca mensagens como lidas (`readMessage: false`).
- Campos do payload por mensagem:
  - `event`: `onmessage` (cliente) ou `onselfmessage` (`key.fromMe=true`);
  - `session`, `id` (string serializada `true|false_<jid>_<id>`), `fromMe`, `timestamp` em segundos (`messageTimestamp`, que o Baileys pode entregar como objeto `Long` e o gateway converte);
  - `from`: JID do remetente (`<num>@c.us` ou `<lid>@lid`); em `onselfmessage`, `from` é o número conectado e `to` é o cliente;
  - `to`: JID do destinatário;
  - `type`: `chat` para `conversation` e `extendedTextMessage`; `image`, `video`, `audio`/`ptt`, `document`, `sticker`, `location`, `vcard` para os demais;
  - `body`: texto, ou legenda da mídia;
  - `isGroupMsg`: `true` para `@g.us`;
  - `sender`: `{ "id": "<num>@c.us", "pushname": "<nome>" }`. Quando o chat é `@lid` e o Baileys informa o número (`key.senderPn`, único campo existente na versão fixa 6.7.24; `remoteJidAlt` é da 7.x). Em mensagem do dono (`fromMe`), `senderPn` é o número do próprio dono e é ignorado, e nenhum par `@lid`/número é aprendido dela, `from` fica com o `@lid` e `sender.id` com o `@c.us`: é assim que o Java registra o apelido e a trava vale nos dois identificadores (WPP-02/03);
  - `notifyName`/`pushname`: `pushName` do Baileys. Alimenta o cadastro de contatos (`contatos`), que guarda o nome cifrado. Em mensagem do dono (`fromMe`) o `pushName` é o nome do próprio dono e **nunca** vai no payload, para não ser gravado no cliente; `pushName` vazio vai ausente, e o Java só atualiza o nome quando ele vem preenchido.
- **Mensagem do dono de qualquer mensagem real** (texto, foto, áudio, figurinha) gera `onselfmessage`: no Java, mensagem do dono é tratada antes do filtro de tipo, então hoje uma foto enviada pelo dono também liga a trava de 20 min. Reação, edição e apagar do dono não contam (ver "Quais mensagens viram evento"). Desligável por `WPPCONNECT_ON_SELF_MESSAGE=false`, como no fork.
- **Eco do bot**: o gateway guarda o id de cada mensagem que ele mesmo enviou (retornado pelo `sendMessage`) e descarta o eco por esse id, sem postar. O Baileys costuma entregar o envio do próprio bot como `append`, que já não é postado; se algum eco chegar como `notify`, o id o barra. O `RegistroEnviosBot` do Java (texto, 2 min) fica como segunda barreira para o eco que escapar. A validação de quais mensagens do celular chegam como `notify` e quais como `append` é feita no aparelho real (tarefa 5.3).
- **Conexão e QR**: novo QR gera `{ "event": "qrcode", "session", "qrcode": "<png-base64>" }`; mudanças de conexão geram `{ "event": "status-find", "session", "status": "inChat" | "qrReadSuccess" | "desconnectedMobile" | ... }`, que o Java ignora.

### 3. Ciclo de vida e persistência
- Credenciais em `/auth-data/:session`, no volume `baileys-auth`. Junto delas o gateway grava `meta.json` com a URL do webhook da sessão, para a reconexão no boot não depender de o Java chamar `start-session` (a variável `WPPCONNECT_WEBHOOK_URL` fica como reserva). A gravação das credenciais e do `meta.json` é atômica (arquivo temporário e renomear), com a última versão boa guardada, para um desligamento brusco não corromper a sessão.
- No boot, o gateway reconecta as sessões com credenciais salvas (equivale a `startAllSession: true`), e o Java chama `start-session` de cada empresa ativa; as duas coisas juntas não abrem socket duplicado.
- Aguardando QR, o gateway renova o QR até o pareamento ou o `close-session`, sem desistir sozinho (equivale a `autoClose: 0`).
- **Reconexão por motivo de queda** (códigos de `DisconnectReason` do Baileys):
  - queda de rede ou do servidor (`connectionClosed`, `connectionLost`, `timedOut`, `unavailableService`): reconecta com espera crescente de 2 s até 60 s, sem desistir nunca; vale para o gateway parado por um tempo, para o celular sem bateria ou sem rede (o aparelho vinculado segue conectado sem o celular) e para o servidor reiniciado;
  - `restartRequired` (logo depois do pareamento): reconecta na hora;
  - `connectionReplaced` (outra instância com as mesmas credenciais, como o `wppconnect` de reserva no ar junto): não reconecta em laço, registra o conflito no log e a sessão fica `DISCONNECTED`;
  - `loggedOut` (cliente desvinculou o aparelho, ou o WhatsApp desvinculou após 14 dias do celular sem internet), `badSession` e `multideviceMismatch`: apaga as credenciais, o status vai a `DISCONNECTED` e o próximo pareamento (QR ou código) gera sessão nova.
- **Sessão pareada**: as credenciais valem como pareadas com `creds.me.id` e (`registered` ou `account`). Pareamento por QR grava `account` e deixa `registered=false`; código pedido e não digitado grava só `me` e não conta como pareado.
- **Status durante a reconexão**: enquanto reconecta com credenciais válidas, `status-session` devolve `INITIALIZING` e o painel passa a mostrar "reconectando" só quando a change `conexao-whatsapp-por-codigo` (WPP-22, tarefa 3.4) estiver aplicada; até lá a tela atual mostra "Desconectado" durante a reconexão, sem botão. Hoje o Java converte `INIT...` em `QRCODE` (`ServicoWppConnect.normalizarStatus`), o que mostraria "aguardando leitura" sem QR; por isso entra um ajuste pequeno no Java (tarefa 4.2f) para repassar `INITIALIZING` como estado próprio, com o contrato de `SessaoStatusResponseDTO` atualizado antes; `QRCODE` só aparece quando há pareamento a fazer. `start-session` com credenciais válidas só dispara a reconexão: nunca gera QR novo nem apaga credenciais. O cliente não precisa fazer nada para o gateway voltar depois de reinício, queda de rede ou celular sem bateria.
- **Limite do WhatsApp**: o aparelho vinculado vale enquanto o celular se conectar à internet ao menos a cada 14 dias; passado isso o WhatsApp desvincula e é preciso parear de novo. Aviso ao cliente de sessão caída fica na change `alertas-desconexao-whatsapp`, fora desta migração; aqui o diagnóstico do admin já mostra a sessão desconectada.
- Mensagens que o cliente final manda enquanto o gateway ou o celular estão fora chegam depois como mensagens atrasadas; o filtro de 120 s do Java as descarta e o bot não responde (mantido de propósito).
- Sessões da Gamb (atendente e teste de módulos) são sessões comuns e seguem as mesmas regras.

### 4. Logs do gateway (WPP-06)
- Nível `info` no gateway e `warn` no logger do Baileys. Número só mascarado (`***1234`), sem texto de mensagem, token, segredo ou QR. O fork atual faz `console.log` do número em `statusConnection.ts` e usa nível `silly`; isso não é reproduzido.

### 5. Melhorias que o Baileys permite

#### 5.1 Só no gateway (o Java não muda)
- **Envio em duas fases**: o Java corta a chamada ao gateway em 5 s (`RestTemplate` de `DemoApplication` e de `ServicoMensagem`). Por isso o `send-message` e o `send-image` validam na hora (token, sessão conectada, número existente, foto baixada) e respondem 2xx assim que o envio entra na fila; os erros de validação seguem com 400, 401 e 404. A digitação e o envio acontecem depois da resposta. Se o envio aceito falhar porque o socket caiu, o gateway tenta mais uma vez quando a sessão reconectar em até 60 s; depois descarta e registra no log, sem dado pessoal. O registro anti-eco do Java vale 2 min e a fila com digitação e nova tentativa pode passar disso; por isso o gateway descarta o eco pelo id da mensagem enviada, que não expira com a fila.
- **"Digitando…"**: antes de cada envio, `sendPresenceUpdate('composing')` por 40 ms por caractere, entre 0,8 s e 3 s, e depois `paused`. Para foto, 1 s fixo. Desligável por `BAILEYS_SIMULAR_DIGITACAO=false`.
- **Fila por sessão em memória**: os envios de uma sessão saem um por vez, com intervalo aleatório de 1 a 2,5 s entre eles, o que reduz o risco de bloqueio do número. A galeria do Java já espera 2 s entre fotos e a ordem é mantida. Fila com mais de 50 envios responde 503 (o Java trata como falha). Uma instância só, como nas demais premissas; se um dia houver mais de uma, a fila deixa de ser única por sessão.
- **Mensagem repetida**: o gateway guarda os ids das mensagens postadas nos últimos 10 min (até 5000) e não reposta o mesmo id, que o Baileys pode reentregar na reconexão.
- **`@lid` convertido em número**: o gateway usa `key.senderPn` e guarda os pares `@lid`/número vistos nos eventos no `MapaRecente` (`src/recentes.ts`: em memória, até 5000 por sessão, o mais antigo sai primeiro, validade de 24 h), para preencher `sender.id` com o número em mais casos (WPP-03). O mapa `signalRepository.lidMapping` e `remoteJidAlt` só existem na 7.x; o gateway os consulta quando a versão fixa os informar, e na 6.7.24 ficam sem efeito. Isso reduz o mesmo cliente virar dois contatos (um pelo `@lid`, outro pelo número); sem número disponível, vale o `@lid`, como hoje.
- **Nova tentativa no webhook**: com erro de rede ou 5xx do Java, o gateway tenta mais 3 vezes (1 s, 2 s e 4 s). Com 401 ou outro 4xx, não repete. O filtro de 120 s do Java barra o que chegar velho. Se o Java processou mas a resposta se perdeu, o cliente pode receber resposta em dobro; risco aceito.

#### 5.2 Com ajuste pequeno no Java
- **Rota de saúde**: `GET /api/health` no gateway, com o segredo enviado no cabeçalho `Authorization: Bearer <SECRET_KEY>` (nunca na URL) e comparado em tempo constante, devolve `{ "status": "ok", "sessoes": { "conectadas": n, "aguardandoQr": n, "desconectadas": ["<sessao>", ...], "nomesConectadas": ["<sessao>", ...] } }`, só com nomes de sessão, sem número. `ServicoWppConnect.consultarSaude()` lê essa rota e o `DiagnosticoModulosService` mostra o WhatsApp como:
  - `INDISPONIVEL` quando o gateway não responde;
  - `DEGRADADO` quando alguma sessão de empresa ativa não está entre `nomesConectadas` (aguardando QR, reconectando ou desconhecida do gateway), com a quantidade e os nomes das sessões; gateway antigo sem o campo `nomesConectadas` é tolerado e vale só `desconectadas`;
  - `OPERACIONAL` com todas conectadas.
  Sessão em `INITIALIZING` (reconectando) conta como desconectada na saúde, então o diagnóstico pode mostrar `DEGRADADO` por alguns segundos durante uma reconexão e voltar a `OPERACIONAL` sozinho.
  Gateway sem a rota (404, caso do WPPConnect na reversão) fica `OPERACIONAL` com a mensagem "gateway sem rota de saúde", como hoje.
- **Pareamento por código de telefone**: alternativa ao QR para quem só tem o celular.
  - Gateway: `POST /api/:session/request-pairing-code` com `{ "phone": "<só dígitos>" }`, só com a sessão aguardando pareamento; chama `requestPairingCode` e devolve `{ "code": "ABCD-EFGH" }`. Sessão conectada responde 409; mais de 3 pedidos em 10 min na mesma sessão respondem 429. O gateway espera até 3 s pelo primeiro QR; se a sessão ainda estiver iniciando, responde 409 "sessão ainda iniciando", e falha do Baileys ao gerar o código responde 502. O 409 não consome o limite de 3 pedidos em 10 min (o pedido nunca saiu); o 502 consome, porque o pedido chegou ao WhatsApp. O Java converte 502 em 503.
  - Java: `POST /whatsapp/{sessao}/codigo-pareamento` com `CodigoPareamentoRequestDTO { telefone }` e resposta `CodigoPareamentoResponseDTO { sessao, codigo }`, com a mesma autorização de `/whatsapp/{sessao}/iniciar` (admin ou cliente da própria empresa). O telefone é o número que vai ser conectado, com DDI 55 e 12 ou 13 dígitos (senão 400); não é gravado e só aparece mascarado no log. Gateway sem a rota (WPPConnect) responde 503 com "pareamento por código indisponível".
  - Contrato: `especificacao/back/paths/whatsapp-codigo-pareamento.yaml`, schemas dos dois DTOs e `$ref` em `swagger.yaml`, antes do Java.
  - Tela: fica na change `conexao-whatsapp-por-codigo` (código de telefone como caminho principal, passo a passo, QR como alternativa). Esta migração entrega a rota do gateway, a rota e os DTOs do Java e o contrato YAML; o QR continua funcionando na tela atual até aquela change.

### 6. Gestão de risco e reversão
- O serviço `wppconnect` fica no `docker-compose.yml` com `profiles: ["backup"]`, com `dados-wpp/userDataDir` e `dados-wpp/tokens` intactos. Os dois nunca sobem juntos com a mesma sessão (`connectionReplaced`).
- O `baileys-gateway` tem `restart: unless-stopped`, `healthcheck` na rota de saúde e o Java com `depends_on` dele, para voltar sozinho depois de queda ou reinício do servidor.
- O `baileys-gateway` fica só na rede `bot-network`, sem publicar porta no host: o `wppconnect` hoje publica `21465:21465`, o que deixa a API do gateway alcançável de fora protegida só pelo `SECRET_KEY`.
- Reversão: parar o `baileys-gateway`, subir o perfil `backup` (`docker compose --profile backup up -d wppconnect`), gravar `WPPCONNECT_URL=http://wppconnect:21465` no `.env` e recriar o Java com `docker compose up -d --no-deps bot-java`. As mudanças do Java (saúde e código de pareamento) toleram o WPPConnect: a saúde volta ao texto de hoje e o código responde 503, com o QR funcionando.
- Virada empresa por empresa não é possível com um único `WPPCONNECT_URL`; a virada é de todas as sessões de uma vez, com novo QR em cada número. Começar pela sessão de teste da Gamb em ambiente local.

## Risks / Trade-offs

- [Baileys é engenharia reversa do protocolo] → versão fixada no `package.json` e atualização só com a suíte de contrato verde.
- [Versão fixa 6.7.24 (a 6.7.21 está deprecada por vulnerabilidade) sem `remoteJidAlt` e sem `signalRepository.lidMapping`, que são da 7.x] → só `senderPn` preenche `sender.id`; sem número, vale o `@lid`. Subir para a 7.x é decisão futura, com a suíte de contrato verde.
- [SSRF pela URL da foto, cadastrada pelo cliente] → `BAILEYS_HOSTS_FOTO_PERMITIDOS` vazio por padrão (lista opcional), IP resolvido uma vez, validado e usado na conexão (sem troca de DNS), IP interno, loopback e link-local recusados, só http/https sem credenciais, sem redirecionamento, só `image/*`, limite de tamanho e de tempo.
- [Dois gateways no ar com a mesma sessão (Baileys e `wppconnect`)] → respondem em dobro e disputam a sessão (`connectionReplaced`); nunca subir os dois juntos.
- [`send-image` passa dos 5 s do Java] → o download e a validação podem estourar o corte do Java, que registra falha, mas a foto entra na fila e é enviada mesmo assim; o limite de 4 s no download reduz o risco.
- [Webhook com nova tentativa por até 60 s] → o Java pode processar e a resposta se perder, gerando resposta em dobro ao cliente; risco aceito.
- [Versão embutida do WhatsApp Web no Baileys pode ser recusada (405)] → plano B: `BAILEYS_BUSCAR_VERSAO_WA=true` busca a versão atual ao conectar.
- [Painel sem estado de reconexão] → até a change `conexao-whatsapp-por-codigo` (WPP-22) estar aplicada, a tela mostra "Desconectado" durante reconexões.
- [Segredo no caminho de `POST /api/:session/:secret/generate-token`] → contrato herdado do WPPConnect, mantido por compatibilidade com o Java; exceção aceita porque a rota só é alcançável entre containers da `bot-network`, o gateway não loga URL e não publica porta no host. A rota nova de saúde usa cabeçalho Bearer.
- [Webhook apontado para fora] → `start-session` e o `meta.json` no boot só aceitam o host de `WPPCONNECT_WEBHOOK_URL` mais `BAILEYS_HOSTS_WEBHOOK_PERMITIDOS`; o cabeçalho `x-webhook-token` não vai para outro destino.
- [Sessão em `INITIALIZING` conta como desconectada na saúde] → o diagnóstico pode mostrar `DEGRADADO` por alguns segundos na reconexão; é aceito.
- [`@lid` sem número exposto] → `from` vai com `@lid` e sem `sender.id` de número; o Java atende pelo `@lid` como hoje.
- [Eco antes do retorno do envio] → coberto pelo registro antes do envio no Java; o gateway não segura o webhook.
- [Eco tardio além da janela de 2 min do `RegistroEnviosBot`, por fila e nova tentativa] → o gateway descarta o eco pelo id da mensagem que ele enviou; o texto no Java é só segunda barreira.
- [`notify` × `append` do Baileys difere do WPPConnect] → quais mensagens do celular do dono chegam como `notify` não é garantido pela documentação; validar no aparelho real (5.3) que a mensagem do dono liga a trava e que o envio do bot não liga.
- [Tipo de mensagem novo do Baileys virar `onselfmessage` por engano] → lista fechada de tipos reais (texto e mídia); o resto é ignorado e coberto por teste de contrato.
- [Envio aceito que falha depois] → uma nova tentativa na reconexão e log; o Java não fica sabendo, ao contrário de hoje, em troca de não estourar os 5 s de timeout com digitação e fila.
- [Fila e lista de ids em memória] → reinício do gateway perde envios na fila e pode repostar uma mensagem; o filtro de 120 s do Java limita o efeito.
- [Código de pareamento usado por terceiro] → só com autorização da empresa da sessão, limite de 3 pedidos em 10 min e código válido só enquanto a sessão aguarda pareamento.
- [Credenciais corrompidas por desligamento brusco] → gravação atômica com a última versão boa guardada.
- [Cliente desvinculado após 14 dias sem internet no celular] → vira `DISCONNECTED` e exige novo pareamento; o aviso proativo ao cliente fica na change `alertas-desconexao-whatsapp`.
- [Credenciais em volume] → volume só do gateway, fora do backup público; exclusão de empresa apaga a pasta.
