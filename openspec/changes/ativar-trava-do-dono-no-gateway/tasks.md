# Tasks

## 1. Gateway

- [ ] 1.1 `wppconnect-server/src/config.ts`: `webhook.onSelfMessage` lê `WPPCONNECT_ON_SELF_MESSAGE` e vale `true` quando a variável não existe; verificar com `wppconnect-server/src/tests/config.test.ts` (padrão `true`, `false` com a variável em `false`) rodando `npx jest config`
- [ ] 1.2 `docker-compose.yml` e `.env.example`: repassar `WPPCONNECT_ON_SELF_MESSAGE` ao serviço `wppconnect`; verificar com `docker compose config` mostrando a variável no serviço

## 2. Back

- [x] 2.1 `WhatsappWebhookDTO`: `onselfmessage` sai de `ehEventoConexao()` e `enviadaPorMim()` devolve `true` quando `event` ou `type` é `onselfmessage`; `resumoRaioX` inclui o `event`; verificar com `WhatsappWebhookDTOTest` (evento sem `fromMe` vira dono; `onack` continua conexão)
- [x] 2.2 `RegistroEnviosBot`: método para esquecer um registro (contato e texto); verificar com `RegistroEnviosBotTest` (registra, esquece, não casa mais)
- [x] 2.3 `ServicoMensagem`: registrar texto e mídia antes de `postWpp` nos envios de texto e imagem, e esquecer o registro quando o envio lançar exceção; verificar com `ServicoMensagemTest` (o registro existe durante a chamada ao gateway e some quando o envio falha)
- [x] 2.4 `ControllerWhatsapp`: manter a ordem QR → conexão → antiga → dono → ruído → atendimento sem o `onselfmessage` na conexão; verificar com `ControllerWhatsappTest`

## 3. Provas

- [x] 3.1 `ControllerWhatsappTest`: payload real do gateway (`event=onselfmessage`, `fromMe=true`, `to` do cliente) liga a trava e o cliente seguinte não é atendido; verificar com o teste verde
- [x] 3.2 `ControllerWhatsappTest`: eco do bot em `onselfmessage` não liga a trava, e `ServicoMensagemTest` prova que o registro existe durante o POST ao gateway
- [x] 3.3 `webhook.feature`: cenário do dono passa a enviar `event=onselfmessage`; verificar com `mvn test` rodando o Cucumber
- [x] 3.4 Rodar `mvn test` completo; verificar com a suíte inteira passando

## 4. Operação e conferência

- [ ] 4.1 Rebuild e redeploy do container `wppconnect` e do Java no VPS; verificar nos logs do gateway que a mensagem do dono gera POST em `/webhook/whatsapp`
- [ ] 4.2 Conferir numa conversa real: dono digita no celular, log "Dono do celular digitou (Anti-X9)" aparece, o cliente responde e o bot fica quieto por 20 minutos; a resposta normal do bot não gera esse log; anotar o resultado no design
- [ ] 4.3 Sincronizar `openspec/specs/whatsapp/spec.md` (WPP-02) com o delta, à mão (o cabeçalho de WPP-02 não tem o prefixo `Requirement:`); verificar relendo o texto do arquivo
- [ ] 4.4 Atualizar `especificacao/back/paths/webhook-whatsapp.yaml` se a descrição mudar; verificar com `git diff` do arquivo

## Workflow follow-up

- Registrar em `openspec/changes/migrar-wppconnect-para-baileys/tasks.md` (task 3.1) que o gateway novo precisa postar a mensagem do dono com `fromMe=true` e `to` do cliente.
- Ao concluir, fechar também `travar-bot-so-quando-o-dono-escreve` (tarefa "Conferir uma conversa real" coberta por 4.2) e mover as duas pastas para `openspec/archive/`.
