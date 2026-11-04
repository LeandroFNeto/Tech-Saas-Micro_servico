# Tarefas

- [ ] Confirmar se o WPPConnect em uso envia cabeçalho próprio no webhook; se não, escolher o segredo no caminho da URL e registrar no design
- [ ] Contrato YAML: cabeçalho de segredo e resposta 401 em `especificacao/back/paths/webhook-whatsapp.yaml`; `$ref` em `especificacao/back/swagger.yaml`, se novo
- [ ] Java: filtro que valida o segredo com comparação de tempo constante, recusa tudo sem segredo configurado, não registra o valor em log
- [ ] Ajustar `ControllerWhatsappTest` e os passos Cucumber para enviar o cabeçalho
- [ ] Prova: cenários sem cabeçalho, com cabeçalho errado e com cabeçalho certo em `webhook.feature`; teste JUnit do filtro
- [ ] Operação: configurar o segredo no backend e no WPPConnect e conferir uma mensagem real em cada número
- [ ] Atualizar `openspec/specs/whatsapp/spec.md` com o requisito novo
- [ ] Mover esta pasta para `openspec/archive/autenticar-webhook-whatsapp/`
