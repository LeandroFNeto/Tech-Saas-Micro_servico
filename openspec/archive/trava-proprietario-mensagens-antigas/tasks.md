# Tarefas

- [x] Contrato YAML: documentar `timestamp`, descarte de mensagens antigas e trava de 20 minutos do proprietário em `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml` e `especificacao/back/paths/webhook-whatsapp.yaml`
- [x] Java cumpre o contrato:
  - [x] `WhatsappWebhookDTO`: adicionar suporte a `timestamp` / `t` / `to` e métodos `ehMensagemAntiga` e `resolverContatoAlvo`
  - [x] `GerenciadorSessao`: adicionar métodos `ativarTravaProprietario` e `estaComTravaProprietarioAtiva` (20 minutos)
  - [x] `ControllerWhatsapp`: descartar mensagens antigas e ativar/respeitar a trava de 20 minutos do proprietário
- [x] Front: sem alterações visuais
- [x] Prova: testes unitários em `WhatsappWebhookDTOTest`, `GerenciadorSessaoTest`, `ControllerWhatsappTest` e BDD em `webhook.feature`
- [x] Atualizar `openspec/specs/whatsapp/spec.md` com os requisitos e cenários atualizados
- [x] Mover esta pasta para `openspec/archive/trava-proprietario-mensagens-antigas/`
