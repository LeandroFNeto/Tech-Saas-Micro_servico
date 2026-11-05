# Tarefas

## Confirmar a causa
- [x] Reproduzir com o gateway: enviar uma resposta do bot e ver nos logs se ela volta com `fromMe=true` e liga a trava ("Dono do celular digitou (Anti-X9)" logo após "[RESPOSTA ENVIADA]"); anotar o resultado no design

## Back
- [x] `WhatsappWebhookDTO.resolverContatoAlvo()`: na mensagem do dono, devolver o destinatário (`to` / `id.remoteJid`) sem trocar por `sender.id`
- [x] `GerenciadorSessao`: apelidos `@lid` ↔ número aprendidos de mensagens do cliente; `ativarTravaProprietario` e `estaComTravaProprietarioAtiva` olham os dois
- [x] `ControllerWhatsapp`: registrar o apelido ao receber mensagem do cliente com `@lid` e número
- [x] Registro de envios recentes do bot em `ServicoMensagem` (id quando houver, senão contato e texto normalizado), com validade de 2 minutos e tamanho limitado
- [x] `ControllerWhatsapp`: `fromMe=true` que bate com o registro não liga a trava; log "Eco do bot ignorado" com número mascarado

## Provas
- [x] `ControllerWhatsappTest`: resposta do bot não liga a trava; mensagem digitada pela pessoa da empresa liga (o `webhook.feature` já cobre a mensagem da pessoa; o eco do bot ficou no teste do controller, pois o cenário exigiria preparar o registro)
- [x] `WhatsappWebhookDTOTest`: mensagem do dono para `@lid` mira o cliente, não a empresa
- [x] `GerenciadorSessaoTest`: trava ligada pelo `@lid` vale para o número do mesmo cliente e o contrário
- [x] `ControllerWhatsappTest`: dono escreve para o cliente `@lid`, cliente responde pelo número, bot não responde
- [x] Teste do registro: casa por id, casa por texto, expira em 2 minutos, respeita o tamanho limite
- [x] Rodar `mvn test` (suíte do back passa)
- [x] Conferir uma conversa real em cada número

## Fechamento
- [x] Sincronizar `openspec/specs/whatsapp/spec.md` com o delta, à mão (o cabeçalho de WPP-02 não tem o prefixo `Requirement:`)
- [x] Atualizar `especificacao/back/paths/webhook-whatsapp.yaml` se a descrição da trava mudar
- [x] Mover esta pasta para `openspec/archive/travar-bot-so-quando-o-dono-escreve/`
