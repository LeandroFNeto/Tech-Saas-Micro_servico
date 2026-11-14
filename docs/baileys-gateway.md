# Baileys Gateway

Gateway do WhatsApp em Node.js + TypeScript (`baileys-gateway/`) que substitui o WPPConnect (`wppconnect-server/`) sem mudar o backend: mesma API REST de saída, mesmo webhook de entrada. Comportamento: change `migrar-wppconnect-para-baileys` (WPP-17 a WPP-20). Tabela rota a rota: `docs/mapeamento-funcoes-migracao-baileys.md`.

## O que mudou para quem opera

| Antes (WPPConnect) | Agora (Baileys) |
|---|---|
| Um Chromium por empresa | Um processo Node.js, um WebSocket por empresa |
| `shm_size` alto e muita RAM | dezenas de MB por sessão |
| Sessão em `dados-wpp/userDataDir` | Credenciais no volume `baileys-auth` (`/auth-data/<sessão>`) |
| Porta 21465 publicada | Sem porta no host; só a rede `bot-network` |

Cada número pareia de novo por QR Code (ou código de telefone) na virada: a sessão do Puppeteer não migra.

## Subir e conferir

```
docker compose up -d --build baileys-gateway bot-java
docker compose ps
docker compose logs --tail 50 baileys-gateway
```

O `bot-java` só sobe depois do `baileys-gateway` ficar saudável (`healthcheck` na rota de saúde). O gateway reinicia sozinho (`restart: unless-stopped`) e reconecta as sessões com credenciais salvas, sem o Java chamar `start-session` e sem QR novo.

Teste local sem Docker:

```
cd baileys-gateway
npm ci
npm test
npm run build
SECRET_KEY=um-segredo-local BAILEYS_AUTH_DIR=./auth-data PORT=21465 node dist/index.js
```

## Variáveis

| Variável | Padrão | Uso |
|---|---|---|
| `SECRET_KEY` | obrigatória (`WPP_SECRET_KEY` no compose) | Gera o token de cada sessão e protege `generate-token` (segredo no caminho, contrato herdado do WPPConnect) e `health` (cabeçalho Bearer). Sem ela o gateway não sobe. |
| `WPPCONNECT_WEBHOOK_SECRET` | `SECRET_KEY` | Valor do cabeçalho `x-webhook-token` em toda chamada ao Java. |
| `WPPCONNECT_WEBHOOK_URL` | `http://bot-java:8080/webhook/whatsapp` | Reserva quando o `start-session` não traz o webhook. |
| `WPPCONNECT_ON_SELF_MESSAGE` | `true` | `false` desliga o `onselfmessage` (trava do dono) em emergência. |
| `BAILEYS_SIMULAR_DIGITACAO` | `true` | "Digitando…" antes de cada envio. |
| `BAILEYS_HOSTS_FOTO_PERMITIDOS` | vazio | Lista opcional de hosts das fotos da galeria. Vazio: qualquer host público; endereços internos são sempre recusados. O download resolve o DNS uma vez e conecta no IP validado. |
| `BAILEYS_HOSTS_WEBHOOK_PERMITIDOS` | vazio | Hosts extras aceitos no webhook do `start-session` e no `meta.json`, além do host de `WPPCONNECT_WEBHOOK_URL`. Outro host responde 400. |
| `BAILEYS_LIMITE_IMAGEM_BYTES` | 5242880 | Tamanho máximo da foto. |
| `BAILEYS_TIMEOUT_DOWNLOAD_MS` | 4000 | Tempo máximo para baixar a foto (o Java corta a chamada em 5 s). |
| `BAILEYS_BUSCAR_VERSAO_WA` | `false` | `true` busca a versão atual do WhatsApp Web ao conectar. Use se o pareamento falhar por versão. |
| `BAILEYS_AUTH_DIR` | `/auth-data` | Pasta das credenciais. |
| `LOG_LEVEL` | `info` | Nível do log do gateway (o logger do Baileys fica em `warn`). |

No Java só muda `WPPCONNECT_URL` (`http://baileys-gateway:21465`).

## API REST (a mesma que o Java já usa)

Rotas da sessão exigem `Authorization: Bearer <token>`. O token é `HMAC-SHA256(SECRET_KEY, sessão)`, sem estado: continua válido depois de reiniciar o gateway e não vale para outra sessão. Token ausente, inválido ou de outra sessão responde 401.

| Rota | Resposta |
|---|---|
| `POST /api/:sessao/:segredo/generate-token` | `{ status, session, token }`; 401 para segredo errado |
| `POST /api/:sessao/start-session` | `{ status, qrcode }` com `CONNECTED`, `QRCODE` ou `INITIALIZING`. Idempotente; nunca gera QR novo nem apaga credenciais com sessão válida |
| `GET /api/:sessao/status-session` | `{ status, qrcode }` com `CONNECTED`, `QRCODE`, `INITIALIZING` (reconectando) ou `DISCONNECTED`. Enquanto a WPP-22 (change `conexao-whatsapp-por-codigo`, tarefa 3.4) não for aplicada, o painel mostra "Desconectado" durante o `INITIALIZING` |
| `GET /api/:sessao/qrcode-session` | bytes `image/png`; sem QR, JSON |
| `POST /api/:sessao/send-message` | 2xx ao entrar na fila; 400 número inexistente ou corpo inválido; 404 sessão desconectada; 503 fila com mais de 50 |
| `POST /api/:sessao/send-image` | igual ao texto; `path` é a URL http(s) da foto ou `data:image/...;base64,`; falha no download responde erro antes da fila |
| `POST /api/:sessao/close-session` | encerra o socket, faz logout e apaga `/auth-data/<sessão>` |
| `POST /api/:sessao/request-pairing-code` | `{ code: "ABCD-EFGH" }`; 409 conectada ou fora do pareamento; 429 acima de 3 pedidos em 10 min |
| `GET /api/health` (cabeçalho `Authorization: Bearer <SECRET_KEY>`, sem segredo na URL) | `{ status, sessoes: { conectadas, aguardandoQr, desconectadas: [nomes], nomesConectadas: [nomes] } }`. Só nomes de sessão, sem número. Reconectando conta como desconectada. O diagnóstico compara as empresas ativas com `nomesConectadas`: qualquer ativa fora dela (aguardando QR, reconectando ou desconhecida do gateway) vira DEGRADADO |

O QR sempre sai como PNG em base64 (`data:image/png;base64,...`), em `start-session`, `status-session` e no evento `qrcode` do webhook, nunca o texto cru do Baileys.

## Webhook de entrada

`POST` na URL do `start-session` (ou `WPPCONNECT_WEBHOOK_URL`) com `x-webhook-token`. Regras:

- Só `messages.upsert` do tipo `notify`, só de conversas `@s.whatsapp.net` e `@lid`. Grupo, status, `@broadcast`, canal, comunidade e `@bot` são ignorados.
- Só texto e mídia reais viram evento. Reação, edição, apagar, enquete, sincronização e chamada são ignorados, do cliente e do dono. Texto embrulhado em mensagem temporária, resposta ou visualização única é desembrulhado.
- Todo JID vai como `<número>@c.us` (sem `:dispositivo`) ou `<id>@lid`. Em chat `@lid` com número conhecido, `from` fica com o `@lid` e `sender.id` com o `@c.us`. Os pares `@lid`/número vistos ficam no `MapaRecente` (`src/recentes.ts`): só em memória, validade de 24 h e teto de 5000 entradas por sessão, descartando o mais antigo.
- Mensagem do dono vira `onselfmessage` (`from` é o número conectado, `to` é o cliente) e o nome do dono nunca vai no payload. Conversa com o próprio número e dono em grupo não geram evento.
- O eco do que o próprio gateway enviou é descartado pelo id da mensagem.
- Id repetido nos últimos 10 min não é reenviado. Erro de rede ou 5xx do Java gera 3 novas tentativas (1 s, 2 s, 4 s); 4xx não repete.
- Eventos `qrcode` e `status-find` seguem o formato que o `ControllerWhatsapp` já trata.

## Ciclo de vida e persistência

- `/auth-data/<sessão>` guarda `creds.json`, as chaves e `meta.json` (URL do webhook). Toda gravação é atômica (arquivo temporário e renomear) e a versão anterior fica em `.bak`; se o arquivo principal estiver corrompido, o gateway lê a cópia.
- No boot, toda sessão com `creds.json` pareado reconecta sozinha. O `start-session` do Java no boot encontra o socket já aberto e não duplica.
- Queda de rede ou servidor (`connectionClosed`, `connectionLost`, `timedOut`, `unavailableService`): reconexão com espera de 2 s, 4 s, 8 s… até 60 s, sem desistir. Durante a espera o status é `INITIALIZING` com credenciais válidas; sessão ainda não pareada fica em `QRCODE`.
- `restartRequired` (logo após parear): reconecta na hora.
- `connectionReplaced`: não reconecta em laço; a sessão fica `DISCONNECTED` com as credenciais preservadas e o log registra o conflito. Acontece se o `wppconnect` de reserva subir junto com a mesma sessão.
- `loggedOut` (inclui os 14 dias do celular sem internet), `badSession` e `multideviceMismatch`: apaga as credenciais e vai a `DISCONNECTED`; o próximo `start-session` gera QR novo.
- Aguardando QR, o gateway renova o QR até parear ou até o `close-session`.

## Envio

O `send-message` e o `send-image` validam na hora (token, sessão conectada, número no WhatsApp, foto baixada) e respondem 2xx ao entrar na fila da sessão, para caber no timeout de 5 s do Java. Depois, um envio por vez, com 1 a 2,5 s entre eles, "digitando…" (40 ms por caractere, 0,8 s a 3 s; foto 1 s) e `paused`. Envio aceito que falha com o socket caído é tentado uma vez quando a sessão reconectar em até 60 s; senão é descartado com log sem dado pessoal.

Destino: `isLid=true`, `@lid` no valor ou 14 dígitos ou mais vai para `<dígitos>@lid` sem conferir; os demais passam por `onWhatsApp` para achar o JID real (com ou sem o nono dígito).

Fotos: o gateway só segue `http` e `https`, recusa endereços internos (IPv4 e IPv6: loopback, link-local, redes privadas e reservadas, IPv4 embutido em IPv6 como `::ffff:`, IPv4-translated/SIIT `::ffff:0:0:0/96`, NAT64 `64:ff9b::/96`, NAT64 local `64:ff9b:1::/48`, 6to4 `2002::/16`, Teredo `2001::/32` e descarte `100::/64`; o IPv4 embutido é validado nesses casos, nos bytes 12-15, e no 6to4 nos bytes 2-5), não segue redirecionamento, exige `Content-Type: image/*` e limita o tamanho. O container precisa alcançar o host das fotos.

## Segurança

- Sem porta publicada no host; só `bot-network`. Container roda como usuário `node`, com imagem de versão fixa.
- Segredos só por variável de ambiente; comparação em tempo constante; o gateway não sobe sem `SECRET_KEY`.
- Nome de sessão só aceita letras, números, `_`, `-` e `.` (impede escapar da pasta de credenciais).
- Log só com número mascarado (`***1234`) e tamanho do texto. Sem texto, token, segredo ou QR.
- O volume `baileys-auth` tem credenciais de WhatsApp: fica fora de backup público e a exclusão da empresa apaga a pasta (`close-session`).

## Virada e reversão

Na branch `transicao` o servidor WPPConnect (`wppconnect-server`) e o perfil `backup` do compose foram removidos. A reversão ao WPPConnect só existe na branch anterior (`fix/revisao-conversa-livre-ia`) e no commit que preserva o fork, antes da remoção.

1. Suba o `baileys-gateway`, pare o `wppconnect` (`docker compose stop wppconnect`) e aponte `WPPCONNECT_URL` para `http://baileys-gateway:21465`.
2. Antes de virar, aplique a WPP-22 (tela "reconectando", tarefa 3.4 de `conexao-whatsapp-por-codigo`) ou aceite que o painel mostre "Desconectado" durante a reconexão (condição da tarefa 6.2). Cada empresa pareia de novo pelo painel (QR ou código de telefone). A sessão de teste da Gamb vai primeiro.
3. O serviço `wppconnect` fica no perfil `backup` do `docker-compose.yml`, com `dados-wpp/userDataDir` e `dados-wpp/tokens` intactos.

Reversão:

```
docker compose stop baileys-gateway
docker compose --profile backup up -d wppconnect
# grave WPPCONNECT_URL=http://wppconnect:21465 no .env, depois:
docker compose up -d --no-deps bot-java
```

Nunca deixe os dois no ar com a mesma sessão (`connectionReplaced`). O Java tolera o WPPConnect: a saúde volta a "gateway sem rota de saúde" e o código de pareamento responde 503 com o QR funcionando.

## Limites conhecidos

- Fila de envio e lista de ids em memória: reinício do gateway perde envios na fila.
- Uma instância só. Com mais de uma, a fila deixa de ser única por sessão.
- O Baileys é engenharia reversa do protocolo: a versão fica fixa no `package.json` e só muda com a suíte de contrato verde.
- Mensagens do cliente enviadas enquanto o gateway estava fora chegam atrasadas e o filtro de 120 s do Java as descarta (de propósito).
- Quais mensagens do celular do dono chegam como `notify` e quais como `append` precisa ser conferido no aparelho real (tarefa 5.3).

## Licença do fork

O código derivado do WPPConnect Server em `wppconnect-server/` segue a Apache License 2.0. Os cabeçalhos de copyright foram removidos dos fontes por regra do projeto; a atribuição ao WPPConnect Team fica em `wppconnect-server/NOTICE` e o texto da licença em `wppconnect-server/LICENSE`.
