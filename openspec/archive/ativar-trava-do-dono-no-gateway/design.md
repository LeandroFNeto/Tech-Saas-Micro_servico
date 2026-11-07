# Design

## Contexto

Ver `proposal.md`. Caminho de uma mensagem digitada pelo dono, como está hoje:

1. WPPConnect (`createSessionUtil.listenMessages`) tem dois ouvintes. `onMessage` dispara só para mensagens recebidas e posta o evento `onmessage`. `onAnyMessage` dispara também para as enviadas pelo próprio número, mas só posta no webhook se `webhook.onSelfMessage` estiver ligado, com o evento `onselfmessage`.
2. `callWebHook` junta `{event, session}` ao payload da mensagem e faz o POST em `/webhook/whatsapp`.
3. `ControllerWhatsapp`: `ehEventoQrcode` → `ehEventoConexao` → `ehMensagemAntiga` → `enviadaPorMim` (trava) → ruído → atendimento.

O bot responde por `ServicoMensagem` → `/send-message`. Toda resposta do bot também passa por `onAnyMessage` e, com o evento ligado, volta ao webhook como `onselfmessage` com `fromMe=true`.

## Goals / Non-Goals

**Goals:**
- A mensagem digitada pela pessoa da empresa liga a trava de 20 minutos, de ponta a ponta, no gateway atual.
- A resposta do bot nunca liga a trava, sem depender da ordem de chegada.
- A prova usa o formato real do payload.

**Non-Goals:**
- Mudar o tempo, persistir a trava ou criar controle no painel.
- Mexer no gateway Baileys.

## Decisões

1. **Ligar `onSelfMessage` no gateway, com chave de ambiente.** `config.ts` lê `WPPCONNECT_ON_SELF_MESSAGE` e usa `true` quando a variável não existe (`!== 'false'`). Alternativa descartada: deixar `false` e tentar inferir o dono por `onack` ou por `sendSeen`; não há sinal confiável sem o evento. O compose repassa a variável para o container.
2. **`onselfmessage` é mensagem do dono, não conexão.** Sai de `ehEventoConexao()`. `enviadaPorMim()` passa a devolver `true` também quando `event` ou `type` é `onselfmessage`, para não depender de o gateway preencher `fromMe`. A ordem do controller fica: QR → conexão (sem `onselfmessage`) → antiga → dono → ruído → atendimento. `onack` continua em conexão: o payload do ack também tem `fromMe`, e tratá-lo como dono ligaria a trava a cada visto.
3. **Registrar antes de enviar.** `ServicoMensagem` chama `registroEnviosBot.registrarTexto/registrarMidia` antes de `postWpp` e, se o envio lançar exceção, remove aquele registro (método novo `esquecer(contato, texto)` ou equivalente). Isso fecha a corrida: quando o eco chegar, o registro já existe. Alternativa descartada: atrasar o processamento do webhook; esconde a corrida em vez de resolver.
4. **Mesmo casamento de eco de antes** (contato + texto normalizado, validade de 2 minutos). Falha segura mantida: sem casamento, é a pessoa.
5. **Diagnóstico.** O log "Dono do celular digitou (Anti-X9)" já existe; ganha o evento no `resumoRaioX` (que hoje só mostra `type`) para a conversa real mostrar `event=onselfmessage`. Sem texto, número mascarado (WPP-06).
6. **Prova com payload real.** Os testes novos montam o JSON como o gateway envia (`event`, `session`, `fromMe`, `from` = número da empresa, `to` = cliente, `body`, `type: chat`, `timestamp`). O teste Jest do gateway garante que `webhook.onSelfMessage` é `true` por padrão e `false` quando a variável vale `false`.
7. **Baileys.** O gateway novo já promete postar `fromMe` (task 3.1 da outra change); esta change fixa o requisito: a mensagem enviada pelo dono precisa sair no webhook com `fromMe=true` e `to` do cliente. Entra como anotação lá, sem editar o código daquela change.

## Riscos / Trade-offs

- [Eco do bot com texto alterado pelo WhatsApp (link com prévia, emoji) não casa] → o bot se silencia por 20 minutos. Mitigação: normalização atual; checar nos logs reais e, se acontecer, casar também por id da mensagem devolvido pelo `send-message`.
- [Volume maior de webhooks: toda resposta do bot gera um POST a mais] → o controller devolve 200 sem tocar no banco; custo desprezível na escala atual.
- [Container antigo continua com a configuração velha até o rebuild] → tarefa de operação explícita e conferência no log do gateway.
- [Trava em memória some no restart] → aceito; o pior caso é o bot responder uma vez após um deploy durante um atendimento manual.

## Plano de Migração

1. Subir a mudança do Java e do gateway juntas (o Java novo aceita os dois formatos; o gateway novo só funciona com o Java novo).
2. Rebuild do container `wppconnect` no VPS.
3. Conferir na conversa real: dono digita no celular, nos logs do Java aparece "Dono do celular digitou (Anti-X9)", o cliente responde e o bot fica quieto; resposta normal do bot não gera o log.
4. Reversão: `WPPCONNECT_ON_SELF_MESSAGE=false` e reiniciar o gateway devolve o comportamento anterior.

## Questões em aberto

- Nenhuma que mude escopo. A duração configurável por empresa pode virar outra change se a atendente pedir.
