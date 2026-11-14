# Mapeamento de Funções: Migração WPPConnect para Baileys

Lista o que o backend usa hoje do WPPConnect (fork em `wppconnect-server/`, derivado do WPPConnect Server sob Apache 2.0; copyright e licença do WPPConnect Team em `wppconnect-server/NOTICE`) e que o `baileys-gateway` precisa manter igual. Conferido contra `ServicoWppConnect`, `ServicoMensagem`, `ControllerWhatsapp`, `WhatsappWebhookDTO`, `RestauradorSessoesWhatsapp`, `ControllerEmpresa` e o fork (`config.ts`, `util/functions.ts`, `middleware/statusConnection.ts`, `util/createSessionUtil.ts`). Comportamento: change `migrar-wppconnect-para-baileys` (WPP-17 e WPP-18).

---

## 1. Rotas HTTP consumidas pelo Java (saída)

Base: `wppconnect.url` (`WPPCONNECT_URL`). Rotas da sessão exigem `Authorization: Bearer <token>`.

| Rota | Método | Consumidor Java | Corpo / parâmetros | Resposta que o Java lê | Comportamento obrigatório |
|---|---|---|---|---|---|
| `/api/{session}/{secretKey}/generate-token` | `POST` | `ServicoWppConnect.obterToken` | Path: `session`, `secretKey` | `{ "status": "success", "token": "<token>" }` (lê só `token`) | Segredo comparado em tempo constante com `SECRET_KEY`; errado responde `401`. O Java guarda o token por sessão em memória, então o token é sem estado, `HMAC-SHA256(SECRET_KEY, sessão)`, e continua válido depois do reinício do gateway. |
| `/api/{session}/start-session` | `POST` | `ServicoWppConnect.iniciarSessao`, `RestauradorSessoesWhatsapp` (boot) | `{ "webhook": "<url>", "waitQrCode": false }` | `{ "status", "qrcode"? }` | Usa a URL do corpo como webhook da sessão. Idempotente: conectada devolve `CONNECTED` sem novo socket; aguardando QR devolve o QR atual. |
| `/api/{session}/status-session` | `GET` | `ServicoWppConnect.consultarStatus` | — | `{ "status": "CONNECTED" \| "QRCODE" \| "INITIALIZING" \| "DISCONNECTED", "qrcode": "<png-base64>" \| null }` | `401` com token ruim (o Java pede outro e repete uma vez). QR sempre PNG base64, nunca o texto cru do Baileys. |
| `/api/{session}/qrcode-session` | `GET` | `ServicoWppConnect.buscarQrcodePng` | `Accept: image/png` | Bytes PNG (`image/png`) | Sem QR, responde JSON (o Java entende como "sem QR"). Corpo com menos de 32 bytes é ignorado pelo Java. |
| `/api/{session}/send-message` | `POST` | `ServicoMensagem.enviarMensagemWPP`, `tentarEnviarMensagemWPP` | `{ "phone": "<só dígitos>", "message", "isGroup": false, "isLid": bool }` | Só o código HTTP | Destino `@lid` se `isLid`, se o valor tiver `@lid` ou com 14 dígitos ou mais; senão `onWhatsApp` acha o JID real (com ou sem nono dígito). `400` número inexistente, `404` sessão desconectada, erro HTTP em falha. `tentarEnviar...` devolve `false` em erro (lembretes, opt-out, reunião). |
| `/api/{session}/send-image` | `POST` | `ServicoMensagem.enviarImagemWPP` (galeria: até 5 fotos, 2 s entre elas) | `{ "phone", "isGroup", "isLid", "path": "<URL http(s)>", "filename", "caption"? }` | Só o código HTTP | Baixa a foto pela URL cadastrada (aceita também `data:` base64) e envia como imagem com legenda. Mesmo destino e mesmos erros do `send-message`. |
| `/api/{session}/request-pairing-code` | `POST` | `ServicoWppConnect.solicitarCodigoPareamento` (nova, WPP-20) | `{ "phone": "<só dígitos>" }` | `{ "code": "ABCD-EFGH" }` | Só com a sessão aguardando pareamento; `409` conectada; `429` acima de 3 pedidos em 10 min. No WPPConnect não existe (o Java responde 503). |
| `/api/health` | `GET` | `ServicoWppConnect.consultarSaude` (nova, diagnóstico) | Cabeçalho `Authorization: Bearer <SECRET_KEY>` | `{ "status": "ok", "sessoes": { "conectadas", "aguardandoQr", "desconectadas": [...], "nomesConectadas": [...] } }` | Só contagens e nomes de sessão; o diagnóstico compara as empresas ativas com `nomesConectadas` (campo ausente em gateway antigo é tolerado: vale só `desconectadas`). No WPPConnect responde 404 (o diagnóstico mostra "gateway sem rota de saúde"). |
| `/api/{session}/close-session` | `POST` | `ServicoWppConnect.desconectar` (exclusão da empresa em `ControllerEmpresa`) | — | Ignorada | Encerra o socket, faz logout e apaga `/auth-data/{session}` para não reconectar no boot. |

Erro com "401" na mensagem durante o envio faz o Java descartar todos os tokens (`invalidarToken()`).

**Envio em duas fases (WPP-19)**: o Java corta a chamada ao gateway em 5 s. `send-message` e `send-image` validam na hora e respondem 2xx ao entrar na fila da sessão; a digitação ("digitando…" de 0,8 s a 3 s) e o envio vêm depois, um por vez, com 1 a 2,5 s entre envios. Fila acima de 50 responde 503. Envio aceito que falha com o socket caído é tentado de novo na reconexão em até 60 s.

---

## 2. Eventos postados no webhook Java (`POST /webhook/whatsapp`)

Destino: URL do `start-session`, com `WPPCONNECT_WEBHOOK_URL` como reserva. Cabeçalho `x-webhook-token: ${WPPCONNECT_WEBHOOK_SECRET}` (reserva `SECRET_KEY`) em toda chamada. Só `messages.upsert` do tipo `notify`; histórico da conexão não é postado; nunca marca como lida.

Campos lidos pelo `WhatsappWebhookDTO`: `event`, `session`, `id`, `fromMe`, `timestamp` (segundos; alias `t`), `from`, `to`, `type`, `body`, `isGroupMsg`, `sender { id, pushname }`, `notifyName`/`pushname`, `qrcode`.

| Situação | `event` | `fromMe` | Payload | Regra acionada no Java |
|---|---|---|---|---|
| Cliente enviou texto | `onmessage` | `false` | `from` (`@c.us` ou `@lid`), `type: "chat"`, `body`, `timestamp`, `sender`, `notifyName` | Contato registrado com o nome cifrado (`contatos`), opt-out por "SAIR"/"CANCELAR", resposta de lembrete (WPP-11) e strategy do ramo (WPP-01). |
| Cliente em chat `@lid` com número conhecido | `onmessage` | `false` | `from: "<lid>@lid"`, `sender.id: "<num>@c.us"` | Apelido `@lid` ↔ número (WPP-03), guardado no `MapaRecente` (`src/recentes.ts`: só em memória, validade de 24 h, teto de 5000 entradas por sessão); a trava do dono vale nos dois. |
| Cliente enviou mídia | `onmessage` | `false` | `type` da mídia (`image`, `audio`, `ptt`, `video`, `document`, `sticker`, `location`, `vcard`), `body` com a legenda | Descartada como tipo inválido (WPP-02). |
| Dono escreveu no celular ou no Web (qualquer tipo) | `onselfmessage` | `true` | `from` (número conectado), `to` (cliente), `type`, `body`, `timestamp` | Anti-X9 (WPP-02): trava de 20 min no cliente, renovada a cada mensagem. Inclui foto, áudio e figurinha. Desligável por `WPPCONNECT_ON_SELF_MESSAGE=false`. |
| Eco do envio do próprio bot | — (descartado no gateway) | `true` | não é postado | Gateway descarta pelo id da mensagem que ele enviou. Se escapar, o `RegistroEnviosBot` reconhece pelo texto por 2 min; não liga a trava. No Baileys o envio do bot costuma chegar como `append`; confirmar no aparelho real. |
| Reação, edição, apagar, enquete, sincronização, chamada (cliente ou dono) | — | qualquer | não é postado | No WPPConnect eram eventos separados que o Java ignora; no Baileys chegam como mensagem e não podem virar `chat` nem `onselfmessage`. |
| Texto embrulhado (efêmera, resposta, visualização única) | `onmessage` | `false` | `type: "chat"`, `body` desembrulhado | Tratado como texto normal. |
| Canal, comunidade, Meta AI, `@broadcast`, grupo | — | qualquer | não é postado | Só `@s.whatsapp.net` e `@lid` passam; o Java segue descartando grupo como segunda barreira. |
| Conversa com o próprio número; dono em grupo | — | `true` | não é postado | Sem trava e sem contato. |
| JID do Baileys (`<num>:<dispositivo>@s.whatsapp.net`) | qualquer | qualquer | `<num>@c.us`, sem `:dispositivo` | O Java extrai os dígitos até o `@` e não trata o `:dispositivo`; a normalização é do gateway. |
| Mensagem com atraso > 120 s | qualquer | qualquer | `timestamp` antigo | Descarte com 200 (WPP-02). |
| Grupo | `onmessage` | `false` | `from: "*@g.us"`, `isGroupMsg: true` | Descarte como ruído (WPP-02). |
| `status@broadcast` | — | — | não é postado | Ignorado no gateway (`ignore` do fork). |
| Novo QR | `qrcode` | — | `session`, `qrcode: "<png-base64>"` | QR em cache para o painel (WPP-04). |
| Mudança de conexão | `status-find` | — | `session`, `status` | Ignorado como evento de conexão. |

Campos: `timestamp` convertido do `Long` do Baileys para segundos; `pushname` do dono (`fromMe`) nunca vai no payload e `pushName` vazio vai ausente.

Robustez do webhook (WPP-19): ids postados nos últimos 10 min não são repostados e ids enviados pelo gateway descartam o eco; erro de rede ou 5xx do Java gera mais 3 tentativas (1 s, 2 s, 4 s), 4xx não; `sender.id` usa também o mapa de `@lid` do Baileys.

Eventos que o Java já ignora (`onack`, `onpresencechanged`, `onparticipantschanged`, reações, mensagens apagadas, enquetes, etiquetas, chamadas) podem não ser emitidos.

---

## 3. Ciclo de vida, segurança e infraestrutura

1. **Autenticação do webhook (WPP-16)**: sem o cabeçalho certo o `WebhookSecretFilter` responde `401`. O gateway não sobe sem segredo configurado.
2. **Logs (WPP-06)**: número só mascarado (`***1234`) e tamanho do texto; sem texto, token, segredo ou QR. Logger do Baileys em `warn`. O fork atual faz `console.log` do número no envio e usa nível `silly`; isso não é copiado.
3. **Persistência e restauração (WPP-18)**: `/auth-data/{session}` no volume `baileys-auth`, com `meta.json` (URL do webhook) e gravação atômica; reconexão no boot (equivale a `startAllSession`) sem duplicar o socket aberto pelo `RestauradorSessoesWhatsapp`; QR renovado até parear (equivale a `autoClose: 0`); logout pelo celular apaga as credenciais.
   - Reconexão por motivo: queda de rede ou servidor reconecta com espera de 2 s a 60 s sem desistir; `restartRequired` na hora; `connectionReplaced` não reconecta em laço; `loggedOut` (inclui os 14 dias do celular sem internet), `badSession` e `multideviceMismatch` apagam as credenciais.
   - Durante a reconexão `status-session` devolve `INITIALIZING`; `start-session` com credenciais válidas nunca gera QR novo.
   - Mensagens do cliente durante a parada chegam atrasadas e são descartadas pelo filtro de 120 s do Java (mantido).
4. **Rede**: o `baileys-gateway` fica só na `bot-network`, sem publicar porta no host, e precisa alcançar as URLs das fotos cadastradas.
5. **Variáveis**: `SECRET_KEY` (= `WPP_SECRET_KEY`), `WPPCONNECT_WEBHOOK_URL`, `WPPCONNECT_WEBHOOK_SECRET`, `WPPCONNECT_ON_SELF_MESSAGE` e a nova `BAILEYS_SIMULAR_DIGITACAO`. No Java só muda `WPPCONNECT_URL`, além da rota de código de pareamento e da leitura da saúde.
6. **Reversão**: serviço `wppconnect` sob `profiles: ["backup"]`, com `dados-wpp/userDataDir` e `dados-wpp/tokens` intactos. A virada troca todas as sessões juntas e exige novo QR em cada número.
