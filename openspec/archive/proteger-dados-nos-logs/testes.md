# Testes: proteger-dados-nos-logs

## Rodada 1 (resultado: erro)

- `./mvnw test`: 154 testes, 1 falha, 0 erros.
- Falha: `WhatsappWebhookDTORaioXTest.resumoComIdSerializadoNaoExpoeNumero`. O raio-x escrevia `id.serialized()` sem máscara e o id (`false_<numero>@c.us_<hash>`) levava o número completo ao log DEBUG.
- Defeito de produto, devolvido à programação.

## Correção (commit `e21dcad`)

- `MascaraLog.idMensagem(String)` mascara o telefone dentro do id, mantendo prefixo `true_`/`false_`, domínio e hash; `@lid` vira `lid:***` + 4 últimos caracteres.

## Rodada 2 (resultado: aprovado)

Arquivos de teste:

- `src/test/java/com/example/demo/util/MascaraLogTest.java` (21 testes: 13 anteriores + 8 de `idMensagem`: nulo/vazio, sem `@`, JID `false_`, prefixo `true_`, `@lid`, parte curta, grupo `@g.us` com hífen, JID sem hash)
- `src/test/java/com/example/demo/servico/observador/ObservadorwhatsappTest.java` (6 testes)
- `src/test/java/com/example/demo/servico/observador/ObservadorGoogleAgendaTest.java` (2 testes)
- `src/test/java/com/example/demo/dto/WhatsappWebhookDTORaioXTest.java` (4 testes, agora todos passam)
- `src/test/java/com/example/demo/servico/ServicoWppConnectLogTest.java` (2 testes)
- `src/test/resources/features/webhook.feature` + `bdd/steps/WebhookSteps.java` (cenário de log sem número completo nem texto)

Comando: `./mvnw -q test` na cópia `scratchpad/logs`, com `credencial.json` copiado temporariamente (removido ao fim).

Resultado real (soma de `target/surefire-reports/*.txt`): 162 testes, 0 falhas, 0 erros, 0 ignorados.

## Sem prova

- `ServicoReserva` (erros do Google Calendar) e `AdminInitializer`/`RestauradorSessoesWhatsapp`: sem teste; exigem credenciais do Google ou contexto de inicialização.
- `ServicoIA`, `docker-compose.yml` e `application.properties` (show-sql, níveis, rotação): sem teste.
- Saída real do WPPConnect Server (fora do código).
- Resposta enviada no BDD: `ServicoMensagem` é dublê no Cucumber; a prova cobre o fluxo recebido e processado, não o log do envio real.

## Volta 1 (resultado: aprovado)

Motivo: a Qualidade reprovou porque `LocacaoStrategy.tratarDataInformada` registrava o texto do cliente e nenhum teste exercitava essa linha. A programação trocou por frase fixa (commit `e0db759`).

Coberto:

- `src/test/java/com/example/demo/strategy/LocacaoStrategyLogTest.java` (1 teste): conversa em `ESPERANDO_DATA`, envio de `"12/05 meu nome e Fulano Sigiloso"` por `processarMensagem`, `ListAppender` do Logback no logger de `LocacaoStrategy` (nível DEBUG, restaurado ao fim). Prova que a frase "Buscando data informada pelo cliente." aparece e que nenhuma mensagem formatada nem argumento de log contém o texto digitado. Google Agenda e envio de mensagem são dublês, sem rede.
- Verificado que o teste falha contra o código antigo: com `Log.info("Buscando data: " + texto)` reposto temporariamente, a asserção falhou com a mensagem `Buscando data: 12/05 meu nome e Fulano Sigiloso`. Código restaurado com `git checkout` e teste passa no código corrigido.

Comandos: `./mvnw -q test -Dtest=LocacaoStrategyLogTest` e `./mvnw -q test` na cópia `scratchpad/logs`, com `credencial.json` copiado temporariamente (removido ao fim).

Resultado real (soma de `target/surefire-reports/*.txt`): 163 testes, 0 falhas, 0 erros, 0 ignorados.

Sem prova:

- `log.warn` de `ServicoGoogleagenda.inicializarAgenda` (classe do erro no lugar de `getMessage()`): provocar a exceção exigiria trocar o `credencial.json` real ou alterar o produto para injetar o caminho do arquivo; não foi inventado teste. Verificado por leitura.
- `RuntimeException` de `ServicoReserva.pesquisarData` (sem chamador em `src/main`) e os itens da rodada 2 listados acima seguem sem teste.

## Volta 2 (resultado: aprovado)

Motivo: a Segurança reprovou porque `MascaraLog.idMensagem` mascarava só o trecho antes do primeiro `@`. Em mensagem de grupo (`false_<grupo>@g.us_<hash>_<participante>@c.us`) o número do participante saía inteiro no raio-x DEBUG; id sem `@` (`5511999997777`, `true_5511999997777_ABC`) também saía inteiro. A programação corrigiu (commit `57d619c`): o id é dividido por `_`, cada segmento com `@` tem a parte anterior mascarada, e segmentos só de dígitos com 5 ou mais caracteres são mascarados; hashes com letras ficam intactos.

Testes novos (9):

- `MascaraLogTest` (+8, total 29; 16 de `idMensagem`): JID direto com hash; grupo com participante (`false_***8901@g.us_3EB0ABC_***7777@c.us`, sem o número do grupo nem do participante); só dígitos (`***7777`); `true_5511999997777_ABC`; `@lid` de 11 caracteres; bordas (`@`, `x@@c.us`, `_@`, `___`, `1234`, `123-456@g.us`); participante `@lid` dentro de grupo; id de 100000 caracteres (`assertTimeoutPreemptively` de 10 s, saída `***@c.us`).
- `WhatsappWebhookDTORaioXTest` (+1, total 5): `resumoRaioX` com id de grupo de 4 partes; nem o número do grupo nem o do participante aparecem.
- Todas as saídas reais coincidiram com o esperado; nenhuma divergência. Testes existentes intactos.

Comandos: `./mvnw -q test` na cópia `scratchpad/logs`, com `credencial.json` copiado temporariamente (removido ao fim).

Resultado real (soma de `target/surefire-reports/*.txt`): 172 testes, 0 falhas, 0 erros, 0 ignorados. Sem `teste-erro.md`.

Sem prova:

- Um segmento só de dígitos com 5 ou mais caracteres que não seja telefone (por exemplo, um timestamp) também é mascarado; é efeito aceito, não vazamento.
- Segmento só de dígitos com 4 ou menos caracteres passa intacto (`1234`), por decisão do design.
- Formato real dos ids de grupo enviados pelo WPPConnect Server (fora do código); os testes usam o formato descrito pela Segurança.
- Itens de `ServicoReserva`, `AdminInitializer`, `RestauradorSessoesWhatsapp`, `ServicoIA`, `docker-compose.yml`, `application.properties` e `log.warn` de `ServicoGoogleagenda` seguem sem teste, como nas rodadas anteriores.
