# Revisão: proteger-dados-nos-logs

- Agente: `claude-opus-5-5` (revisão e spec definitiva), volta 2.
- Cópia: `scratchpad/logs`, branch `proteger-dados-nos-logs`, 8 commits de produto sobre `origin/main` (`39c2b85` a `25f20fc`) mais o commit da spec `9d8e7b2`.
- Veredito: **aprovado**.

## Confirmado no código

| Item | Comando ou leitura | Resultado |
|---|---|---|
| Console fora do logger | `git grep -nE "System\.out\|System\.err\|printStackTrace" -- src/main/java` | zero ocorrências |
| Concatenação em log | `git grep -nE "[lL]og\.(trace\|debug\|info\|warn\|error)\([^;]*\" *\+" -- src/main/java` | zero ocorrências |
| `getMessage()` | `git grep -n "getMessage()" -- src/main/java` | só `ServicoMensagem.java:42` e `:69`, em `contains("401")` para invalidar token; não vai para log |
| Outros nomes de logger | `git grep -nE "(logger\|LOGGER\|LOG)\.(...)\(\|@Slf4j" -- src/main/java` | nenhum |
| Todas as chamadas `log.*`/`Log.*` (30) | lidas uma a uma | só sessão, id de empresa, classe da exceção, código HTTP, e-mail e JID mascarados, tamanho do texto, frases fixas |
| `MascaraLog` | `util/MascaraLog.java` | `numero`, `jid` (`lid:***NNNN`), `idMensagem` (todos os segmentos), `email`, `tamanho` |
| Raio-x | `WhatsappWebhookDTO.resumoRaioX` e `ControllerWhatsapp:58` | sem `body`/`content`; id por `idMensagem`, número por `jid`, texto por `tamanho`; escrito em `log.debug` |
| `Observadorwhatsapp` | diff | assinaturas iguais; `logErro` com classe e, para `HttpStatusCodeException`, só o código numérico |
| `ServicoMensagem` | leitura | passa o texto da resposta a `logSucesso`, que só registra o tamanho |
| Configuração | `application.properties` | `spring.jpa.show-sql=false`, `logging.level.root=INFO`, `logging.level.com.example.demo=INFO` |
| Rotação Docker | `docker-compose.yml` | `json-file`, `max-size: 10m`, `max-file: "3"` nos serviços do WPPConnect e da API |
| `especificacao/` | `git diff --stat origin/main HEAD -- especificacao/` | sem diff |
| `teste-erro.md` | `ls openspec/changes/proteger-dados-nos-logs/teste-erro.md` | não existe |
| Suíte | `./mvnw -q test` (com `credencial.json` copiado e apagado ao fim) e soma de `target/surefire-reports/*.txt` | saída 0; 172 testes, 0 falhas, 0 erros, 0 ignorados |

## Requisito gravado

- `openspec/specs/whatsapp/spec.md`: **WPP-06 Atendimento sem dado pessoal nos logs** (o `proposal.md` cogitava WPP-09; o próximo número livre era WPP-06, porque o arquivo ia até WPP-05). Contrato: nenhum. Prova: `webhook.feature`, `MascaraLogTest`, `ObservadorwhatsappTest`, `ObservadorGoogleAgendaTest`, `WhatsappWebhookDTORaioXTest`, `ServicoWppConnectLogTest`, `LocacaoStrategyLogTest`. Cenários: mensagem recebida e resposta enviada; mensagem de grupo com participante; erro HTTP do WPPConnect.
- `openspec/specs/README.md`: linha "Webhook, sessão e bot" com as provas novas.
- `openspec/specs/00-architecture.md`: **não existe na cópia** (a `main` só tem `specs/README.md` e `specs/whatsapp/`). Não foi criado; a regra transversal de logs fica pendente para quando o arquivo existir.
- Commit `9d8e7b2` na branch `proteger-dados-nos-logs` da cópia. Sem push.

## Pendências para a pessoa

1. **Melhoria opcional de `MascaraLog`** (notas da Qualidade e da Segurança, não bloqueiam): participante com sufixo de dispositivo `:NN` sai com 2 dígitos reais (`***7712`); cortar em `:` antes de contar deixaria `***7777`. Segmento sem `@` com `+` ou `:` passa intacto (formato que o WhatsApp não manda, só em DEBUG).
2. **Candidato a change própria**: `docker-compose.yml` publica `5432` e `21465` no host, tem senha padrão para `ADMIN_DEFAULT_PASSWORD` e `CORS_ALLOWED_ORIGINS=*`.
3. **Na implantação**: apagar ou rotacionar os logs e o histórico dos contêineres escritos antes desta change; conferir o nível de log do WPPConnect Server e desligar o registro do conteúdo das mensagens.
4. **Numeração WPP**: `corrigir-reserva-automatica` cita WPP-06 e `lembrar-confirmacao-reserva` cita WPP-07/08. Esta change usou WPP-06, então as duas precisam renumerar para o próximo livre quando forem gravadas.
5. **`00-architecture.md`**: o README aponta para ele, mas o arquivo não existe na `main`. Decidir se cria (com a linha da regra de logs) ou tira a linha do README.
6. **Documentação da change** (nota da Qualidade): `design.md` não lista `idMensagem` na decisão 2 nem cita `LocacaoStrategyLogTest`/`WebhookSteps` na Prova.

## Veredito

Aprovado. O código cumpre o requisito, a suíte passa e a spec WPP-06 está gravada. Falta o Deploy.
