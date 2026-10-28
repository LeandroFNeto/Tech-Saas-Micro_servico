# Segurança: proteger-dados-nos-logs (volta 2)

- Agente: `claude-opus-5-5`
- Cópia avaliada: `scratchpad/logs`, branch `proteger-dados-nos-logs`, `git diff origin/main HEAD` (8 commits; correção em `57d619c`, provas em `25f20fc`)
- Escopo: só os arquivos da seção Arquivos do `design.md` (Java de `src/main`, `application.properties`, `docker-compose.yml`). Tela, texto e estilo ignorados.
- Método: leitura do diff e dos arquivos inteiros do escopo; execução isolada de `MascaraLog` (cópia fora do repositório, apagada depois) com 36 formatos de id/JID e entradas de borda.

## Volta 1 (resumo)

Reprovou porque `MascaraLog.idMensagem` mascarava só o JID antes do primeiro `@`: em mensagem de grupo (`false_<grupo>@g.us_<hash>_<participante>@c.us`) o número do participante saía inteiro no raio-x DEBUG. Nota junto: id sem `@` (`5511999997777`, `true_5511999997777_ABC`) saía como veio.

## Achados

| # | Severidade | Arquivo:linha | Descrição | Cenário |
|---|---|---|---|---|
| 1 | Informativa (não bloqueia) | `src/main/java/com/example/demo/util/MascaraLog.java:15-20` (via `segmentoDeId`, linha 60) | Participante com sufixo de dispositivo `5511999997777:12@c.us` sai `***7712`: os dígitos do dispositivo entram na contagem e só 2 dígitos reais do número aparecem. | Menos exposição que os "4 últimos" aceitos pelo design. Não é vazamento. Correção opcional, para o log ficar comparável: cortar em `:` antes de extrair os dígitos (sai `***7777`). |
| 2 | Baixa (nota) | `src/main/java/com/example/demo/util/MascaraLog.java:62-65` | Segmento sem `@` que não é só dígitos passa intacto: `+5511999997777` e `5511999997777:12`. | Só chega ao `resumoRaioX` se o WPPConnect mandar `id` como texto sem `@` e com `+` ou `:`. O `_serialized` do WPPConnect sempre traz `@domínio` e o `remoteJid` do objeto Baileys também; não achei formato real que produza isso. Só em DEBUG (desligado por padrão). O campo `numero=` é mascarado em separado por `MascaraLog.jid(resolverRemoteJid())` e não usa esse id. Correção sugerida: em todo segmento, trocar cada sequência de 5 ou mais dígitos por `***` mais os 4 últimos (laço linear, sem regex). |
| 3 | Baixa (nota) | `src/main/java/com/example/demo/util/MascaraLog.java:56,60` | O trecho depois do primeiro `@` de um segmento é copiado sem máscara. | Só em formatos inventados, sem `_` entre dois JIDs: `5511999997777@c.us5511888886666@c.us` ou `x@5511999997777@c.us`. Não existe no WhatsApp. A mesma correção do achado 2 fecha isso. |

Nada para devolver à programação. As notas 1 a 3 podem entrar numa melhoria futura de `MascaraLog`.

## Verificado e OK

- **Vazamento da volta 1 fechado.** Resultados medidos:
  - privado `false_5511999997777@c.us_3EB0…` → `false_***7777@c.us_3EB0…` (também `true_`, `@s.whatsapp.net`, sufixo `:3`);
  - grupo `false_120363012345678901@g.us_3EB0ABC_5511999997777@c.us` → `false_***8901@g.us_3EB0ABC_***7777@c.us` (também com `@s.whatsapp.net`, participante `@lid` e grupo antigo `5511999997777-1600000000@g.us`);
  - `@lid` → `lid:***2345@lid`; `@lid` com dispositivo → `lid:***45:7@lid`;
  - `status@broadcast_…_5511999997777@c.us` → `***@broadcast_…_***7777@c.us`; `@newsletter` → `***1111@newsletter_123`;
  - dois números no mesmo id separados por `_` → os dois mascarados;
  - `+55 11 99999-7777@c.us` e `+5511999997777@c.us` → `***7777@c.us`;
  - id sem `@` só com dígitos (`5511999997777`, `true_5511999997777_ABC`) → `***7777` (a nota 2 da volta 1 também fechou).
- **Robustez de `MascaraLog`.** Sem exceção com `null`, vazio, `_`, `@`, `@@@`, `___@@@___`, `a@`, `@lid`, `1@lid`, `false_@c.us_X`, `abc@lid@c.us`, `x@@c.us`. `split("_", -1)` com literal de um caractere e laço linear; a única regex é `\D`. 600 mil caracteres em menos de 100 ms. Sem ReDoS.
- **`resumoRaioX`**: sem `body` nem `content`; número por `jid`, texto por `tamanho`; escrito só em `log.debug` no `ControllerWhatsapp`, único log desse controlador.
- **`Observadorwhatsapp`**: as três funções usam jid mascarado e `tamanho`; `logErro` registra a classe e, para `HttpStatusCodeException`, só o código numérico; sem corpo de resposta HTTP nem `getMessage()`.
- **Serviços** (`ServicoWppConnect`, `ServicoIA`, `ServicoGoogleagenda`, `ServicoReserva`, `ObservadorGoogleAgenda`, `RestauradorSessoesWhatsapp`, `LocacaoStrategy`): todos os logs relidos no arquivo inteiro, não só no diff. Nenhum `System.out`, `System.err`, `printStackTrace` ou `getMessage()`. Token WPPConnect, chave do Gemini (vai na URL), `googleCalendarId`, nome do cliente, nome da empresa e texto do cliente não aparecem; ficam sessão, id da empresa e classe da exceção.
- **Causa da exceção não chega ao cliente.** `ServicoReserva.pesquisarData` agora lança `RuntimeException("Erro ao buscar data", e)`, mas não tem chamador em `src/main` e não há `@ControllerAdvice` no projeto. Os `catch` de `LocacaoStrategy` mandam ao WhatsApp só frases fixas.
- **`AdminInitializer`**: e-mails por `MascaraLog.email`; a senha só passa por `passwordEncoder.encode` e nunca vai a log.
- **Configuração.** `application.properties`: `show-sql=false`, `root` e `com.example.demo` em `INFO`. `docker-compose.yml`: só rotação `json-file` 10 MB × 3; sem porta, segredo ou variável nova.
- **Acesso indevido.** O diff não toca segurança, filtros, CORS, rotas nem autorização. Em `config/` mudam só `AdminInitializer` e `RestauradorSessoesWhatsapp`, nas linhas de log.

## Notas anteriores à change (sem mudança desde a volta 1)

- `docker-compose.yml` publica `5432` e `21465` no host, tem padrão para `ADMIN_DEFAULT_PASSWORD` e `CORS_ALLOWED_ORIGINS=*`. Vale uma change própria.
- `MascaraLog.email("a@b.com")` → `a***@b.com` revela uma parte local de 1 caractere; impacto baixo.

## Veredito

**Aprovado.** O vazamento do participante de grupo está fechado, e o caso sem `@` também. Nenhum formato real de id ou JID do WhatsApp expõe o número inteiro. O achado 1 mostra menos que os 4 últimos dígitos aceitos pelo design. Os achados 2 e 3 dependem de formatos que o WhatsApp não manda e só aparecem em DEBUG. Ficam como notas.
