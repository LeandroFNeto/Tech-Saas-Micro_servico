# Tarefas

## Programação

- [x] Java: `MascaraLog` (número, jid `@lid`, e-mail e tamanho do texto)
- [x] Java: `Observadorwhatsapp`, `ObservadorGoogleAgenda`, `ControllerWhatsapp` e `WhatsappWebhookDTO.resumoRaioX` sem texto nem número completo; raio-x em DEBUG
- [x] Java: `ServicoReserva`, `ServicoWppConnect`, `ServicoIA`, `AdminInitializer` e `RestauradorSessoesWhatsapp` com logger, sem `System.out`, `printStackTrace`, `getMessage()` de exceção nem nome ou e-mail em claro
- [x] Configuração: `spring.jpa.show-sql=false`, níveis de log em `application.properties` e rotação de logs do Docker em `docker-compose.yml`
- [x] Limpeza: remover os comentários de código dos arquivos alterados
- [x] Prova: `MascaraLogTest`, `ObservadorwhatsappTest`, cenário em `webhook.feature`, teste do raio-x e das exceções, conforme `design.md`, seção Arquivos

## Na implantação (a pessoa faz, fora do código)

- Apagar ou rotacionar os arquivos de log e o histórico dos contêineres escritos antes desta change.
- Conferir o nível de log do WPPConnect Server e desligar o registro do conteúdo das mensagens, se houver.

## Fechamento (não é da programação)

- [x] Revisão: gravar WPP-06 (próximo livre; a proposta citava WPP-09) em `openspec/specs/whatsapp/spec.md` e a linha em `openspec/specs/README.md`. A regra de logs em `openspec/specs/00-architecture.md` ficou pendente: o arquivo não existe na main (ver `revisao.md`)
- [x] Deploy: mover esta pasta para `openspec/archive/proteger-dados-nos-logs/`
