# Tasks

## 1. Linha de base e esqueleto do gateway

- [ ] 1.1 Medir `docker stats` do WPPConnect no VPS com as sessões ativas e registrar o resultado em `docs/baileys-ram.md`; verificar que o arquivo traz memória por container e o número de sessões
- [ ] 1.2 Criar `baileys-gateway/` (Node) com `package.json`, versão do Baileys fixada sem intervalo e `Dockerfile`; verificar que `docker build` conclui e o container sobe
- [ ] 1.3 Adicionar o serviço ao `docker-compose.yml` com volume `baileys-auth`, porta interna, sem `shm_size`, e variáveis de ambiente do segredo; verificar que `docker compose up baileys-gateway` fica saudável ao lado do WPPConnect

## 2. API compatível com o WPPConnect

- [ ] 2.1 Implementar `generate-token` e a exigência de `Bearer` nas demais rotas, com comparação de segredo em tempo constante e sem registrar o valor; verificar com teste do gateway que token errado responde 401
- [ ] 2.2 Implementar `start-session`, `status-session` e `close-session` com os status `CONNECTED`, `QRCODE` e `DISCONNECTED` (WPP-16); verificar com teste de contrato por status
- [ ] 2.3 Implementar `qrcode-session` (PNG) e o QR no corpo de `status-session`, e postar o evento de QR no webhook (WPP-17); verificar que sessão nova devolve `QRCODE` com QR
- [ ] 2.4 Implementar `send-message` e `send-image` com o corpo que `ServicoMensagem` já envia (WPP-16); verificar com teste que a chamada existente do backend funciona sem alteração de payload
- [ ] 2.5 Persistir a credencial de cada sessão em `baileys-auth` e reconectar as sessões pareadas ao subir (WPP-18); verificar reiniciando o container e conferindo `CONNECTED` sem novo QR

## 3. Webhook e tradução de mensagens

- [ ] 3.1 Converter a mensagem recebida para o JSON de `WhatsappWebhookDTO` (sessão, `from` em `@c.us`, `fromMe`, `timestamp`, texto, tipo) e postar em `WPPCONNECT_WEBHOOK_URL`; verificar com teste que o JSON valida contra `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`
- [ ] 3.2 Resolver contato `@lid` para o número real e descartar sem resposta quando não houver número, com log sem número completo nem texto (WPP-19); verificar com dois testes: resolvível e não resolvível
- [ ] 3.3 Enviar `fromMe`, mensagem antiga, grupo e status no mesmo formato de antes; verificar que `ControllerWhatsapp` os filtra como hoje, rodando `WebhookSteps` e `ControllerWhatsappTest` contra o payload do gateway

## 4. Ajustes no Java

- [ ] 4.1 Trocar o host fixo `http://wppconnect:21465` de `ServicoMensagem` por `wppconnect.url`; verificar com teste que a URL de envio vem da propriedade
- [ ] 4.2 Apontar `WPPCONNECT_URL` e `WPPCONNECT_WEBHOOK_URL` do compose para o gateway e conferir `ServicoWppConnect` sem mudança de lógica; verificar com `mvn test` verde
- [ ] 4.3 Rodar `RestauradorSessoesWhatsapp` contra o gateway; verificar que o log mostra cada sessão restaurada sem número completo

## 5. Prova do contrato e documentação

- [ ] 5.1 Escrever cenários em `webhook.feature` para sessão não pareada, QR lido, reinício com restauração e `@lid`; verificar que passam com o gateway
- [ ] 5.2 Documentar o gateway, as variáveis, o volume e o procedimento de repareamento em `docs/baileys-gateway.md`; verificar que os comandos escritos rodam como descritos
- [ ] 5.3 Atualizar `openspec/specs/whatsapp/spec.md` com WPP-16 a WPP-19 e trocar "WPPConnect" por termo genérico em WPP-03 e WPP-04; verificar com `openspec validate`

## 6. Virada em produção

- [ ] 6.1 Subir o gateway ao lado no VPS e parear uma sessão de teste; verificar uma mensagem real de ida e volta
- [ ] 6.2 Combinar a janela com cada empresa e com a atendente da Gamb; verificar com a lista de empresas, horário e responsável por QR
- [ ] 6.3 Virar sessão por sessão, começando pela Gamb: parar o WPPConnect, apontar o backend, reparear, conferir uma mensagem real em cada número; verificar com o status `CONNECTED` e a resposta do bot
- [ ] 6.4 Manter o WPPConnect parado com `dados-wpp` preservado durante o período de reversão; verificar que o plano de reversão do design volta o bot em teste
- [ ] 6.5 Medir `docker stats` com o gateway e registrar o ganho em `docs/baileys-ram.md`; verificar a comparação com a linha de base
- [ ] 6.6 Remover o serviço `wppconnect` do compose e a pasta `wppconnect-server/` após a reversão expirar; verificar que `docker compose up` sobe sem o container antigo

## Workflow follow-up

- Arquivar a change depois de a revisão e a virada estarem concluídas.
- `autenticar-webhook-whatsapp` segue separada; se aplicada, o gateway envia o cabeçalho de segredo.
