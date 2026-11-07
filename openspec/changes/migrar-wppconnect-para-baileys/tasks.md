# Tasks: Migração para Baileys Garantindo Paridade Total

## 1. Fundação e Estrutura do Gateway
- [ ] 1.1 Mapeamento e matriz de paridade formalizados em `docs/mapeamento-funcoes-migracao-baileys.md` cobrindo todas as rotas e eventos
- [ ] 1.2 Criar diretório `baileys-gateway/` com `package.json`, `@whiskeysockets/baileys` em versão estritamente fixada, TypeScript e `Dockerfile`
- [ ] 1.3 Configurar o serviço `baileys-gateway` no `docker-compose.yml` com volume persistente `baileys-auth`, variáveis `WPPCONNECT_SECRET_KEY` e `WPPCONNECT_WEBHOOK_SECRET`
- [ ] 1.4 Manter o serviço `wppconnect` original intacto sob perfil de rollback para garantia de reversão

## 2. Implementação da API REST Compatível
- [ ] 2.1 Implementar `POST /api/:session/:secret/generate-token` com comparação de tempo constante contra o segredo compartilhado
- [ ] 2.2 Implementar `POST /api/:session/start-session` gerenciando sockets Baileys independentes por empresa via `useMultiFileAuthState`
- [ ] 2.3 Implementar `GET /api/:session/status-session` e `GET /api/:session/qrcode-session` devolvendo status `CONNECTED`, `QRCODE` e `DISCONNECTED`
- [ ] 2.4 Implementar `POST /api/:session/send-message` e `POST /api/:session/send-image` com os exatos payloads consumidos por `ServicoMensagem`
- [ ] 2.5 Implementar `POST /api/:session/close-session` para desconexão segura

## 3. Disparo do Webhook e Garantia dos Comportamentos WPP-01 a WPP-16
- [ ] 3.1 Implementar despacho do webhook com o cabeçalho obrigatório `x-webhook-token: ${WPPCONNECT_WEBHOOK_SECRET}` para conformidade com `WPP-16`
- [ ] 3.2 Implementar captura de mensagens enviadas pelo próprio dono (`fromMe=true`) emitindo o evento `onselfmessage` com `from`, `to` e `body` para garantir a ativação da trava de 20 min Anti-X9 (`WPP-02`)
- [ ] 3.3 Garantir que o timestamp da mensagem seja despachado no padrão Unix em segundos para atendimento do filtro de atraso temporal > 120s (`WPP-02`)
- [ ] 3.4 Tratar identificadores `@lid` e garantir normalização sem quebra para contatos de clientes (`WPP-03`)
- [ ] 3.5 Assegurar emissão dos eventos de `qrcode` e conexão no mesmo formato tratado por `ControllerWhatsapp`

## 4. Ajustes Mínimos no Backend Java (Ponytail)
- [ ] 4.1 Modificar `ServicoMensagem.java` para utilizar a propriedade injetada `wppconnect.url` em vez de URL fixa no código
- [ ] 4.2 Validar que `RestauradorSessoesWhatsapp` inicializa e restaura sessões no gateway Baileys sem erros
- [ ] 4.3 Rodar a suíte completa de testes de regressão (`scripts/auditar-kirito.ps1 -ComRegressao`) garantindo 100% de aprovação

## 5. Testes Integrados e Homologação
- [ ] 5.1 Subir o container `baileys-gateway` em ambiente local e executar teste ponta a ponta com uma sessão de teste
- [ ] 5.2 Validar disparo de webhook para o backend e conferir no log que a trava do proprietário Anti-X9 é acionada ao enviar mensagem pelo aparelho
- [ ] 5.3 Validar que o status comuta para `CONNECTED` após leitura do QR Code
- [ ] 5.4 Simular reinicialização do container e validar restauração de sessão sem novo QR Code (`WPP-18`)

## 6. Documentação e Plano de Virada
- [ ] 6.1 Documentar o gateway e procedimento de virada em `docs/baileys-gateway.md`
- [ ] 6.2 Executar a virada planejada empresa por empresa, mantendo os dados legados do WPPConnect disponíveis para reversão imediata se necessário
- [ ] 6.3 Atualizar `openspec/specs/whatsapp/spec.md` com os requisitos definitivos de paridade de provedor e arquivar a change
