# Design

## Decisão

1. **Logger no lugar do console.** Cada classe abaixo ganha um `Logger` SLF4J (como `AdminInitializer` e `ServicoGoogleagenda` já têm) e deixa de usar `System.out`, `System.err` e `printStackTrace`: `ControllerWhatsapp`, `Observadorwhatsapp`, `ObservadorGoogleAgenda`, `ServicoReserva`, `ServicoWppConnect` e `ServicoIA` (checar se há saída).
2. **`MascaraLog`.** Classe utilitária em `util/` com funções puras:
   - `numero(jid)`: extrai os dígitos e devolve `***` mais os 4 últimos (`***7777`). Vazio ou curto devolve `***`;
   - `jid(jid)`: igual a `numero`, e para `@lid` devolve `lid:***` mais os 4 últimos caracteres;
   - `email(email)`: primeira letra, `***` e o domínio;
   - `tamanho(texto)`: `"<n> caracteres"`, sem o conteúdo.
3. **Regra de conteúdo.** Nenhum log escreve:
   - texto de mensagem recebida ou enviada, legenda, nome de cliente final, número completo;
   - e-mail completo de usuário, token, senha, chave de API;
   - `googleCalendarId`, URL com identificador, corpo de resposta HTTP.
   Podem aparecer: nome da sessão, id da empresa, id interno de reserva, número mascarado, tipo de evento, tamanho do texto, classe da exceção e código HTTP.
4. **`Observadorwhatsapp`.** Mantém as assinaturas `logSucesso(sessao, jid, msg)`, `logFiltro(sessao, jid, motivo)` e `logErro(sessao, jid, local, e)`, para não mexer em quem chama:
   - `logSucesso`: `INFO` com `sessao`, `MascaraLog.jid` e `MascaraLog.tamanho(msg)`;
   - `logFiltro`: `INFO` com `sessao`, jid mascarado e o motivo;
   - `logErro`: `WARN` com `sessao`, jid mascarado, `local` e a classe da exceção, com o código HTTP quando a exceção for do cliente HTTP do Spring. Sem a mensagem da exceção.
5. **Raio-x do webhook.** `WhatsappWebhookDTO.resumoRaioX` passa a devolver id, tipo, `isGroupMsg`, `fromMe`, número mascarado e tamanho do texto, sem `body` nem `content`. `ControllerWhatsapp` o escreve em `DEBUG`. O padrão do projeto é `INFO`, então fica desligado.
6. **Erros dos serviços.** Em `ServicoWppConnect`, `ServicoReserva` e `ObservadorGoogleAgenda`, cada `catch` registra com `log.warn`/`log.error` uma frase fixa, o nome da sessão ou o id da empresa e a classe da exceção. O `printStackTrace` sai. O nome da empresa e do cliente final saem das mensagens; fica o id.
7. **E-mails de usuários.** `AdminInitializer` registra o e-mail mascarado e a sessão da empresa.
8. **Configuração.**
   - `application.properties`: `spring.jpa.show-sql=false`, `logging.level.root=INFO` e `logging.level.com.example.demo=INFO`. Quem quiser SQL ou DEBUG em desenvolvimento usa a variável de ambiente (`SPRING_JPA_SHOW_SQL`, `LOGGING_LEVEL_COM_EXAMPLE_DEMO`).
   - `docker-compose.yml`: `logging` com driver `json-file`, `max-size: 10m` e `max-file: "3"` nos serviços da API e do WPPConnect.
9. **Comentários.** Os arquivos alterados perdem os comentários de código (o projeto adotou o método spec-driven: o comportamento mora em `openspec/`). Isso inclui os `//` de explicação e os blocos `/** */` descritivos dos trechos mexidos. Não há mudança de lógica nessa limpeza.

## Arquivos

- Contrato:
  - Nenhum. Não há rota nem DTO de API novo.
- Java:
  - `util/MascaraLog.java` (novo)
  - `controller/ControllerWhatsapp.java` (raio-x em DEBUG, logger)
  - `dto/WhatsappWebhookDTO.java` (`resumoRaioX` mascarado)
  - `servico/observador/Observadorwhatsapp.java`, `servico/observador/ObservadorGoogleAgenda.java`
  - `servico/ServicoReserva.java`, `servico/ServicoWppConnect.java`, `servico/ServicoIA.java`, `servico/ServicoGoogleagenda.java` (avisos sem `getMessage()`)
  - `strategy/LocacaoStrategy.java` (log da data informada sem o texto do cliente)
  - `config/AdminInitializer.java` (e-mail mascarado) e `config/RestauradorSessoesWhatsapp.java` (mensagem sem `e.getMessage()`)
  - `src/main/resources/application.properties` e `docker-compose.yml`
- Front:
  - Nenhum.
- Prova:
  - `src/test/java/com/example/demo/util/MascaraLogTest.java` (número, `@lid`, e-mail, vazio, curto)
  - `src/test/java/com/example/demo/servico/observador/ObservadorwhatsappTest.java` (com `OutputCaptureExtension` ou appender do Logback: o texto e o número completo não aparecem na saída)
  - `src/test/resources/features/webhook.feature` (mensagem recebida e resposta enviada: a saída capturada não contém o número completo nem o texto)
  - Teste de `WhatsappWebhookDTO.resumoRaioX` (sem `body` nem `content`)
  - Teste das exceções de `ServicoWppConnect` e `ObservadorGoogleAgenda` (saída sem a mensagem da exceção e sem o nome do cliente)

## Alinhamento

- Stack de `00-architecture.md`
- Sem entidade nova e sem mudança de contrato HTTP
- Sem comentários no código; o comportamento está na spec

## Riscos

- **Menos informação para o suporte.** O número mascarado e o tamanho do texto ainda permitem seguir uma conversa e achar falhas. Para investigar o conteúdo de uma mensagem, o caminho é o próprio WhatsApp ou o histórico do cliente, não o log.
- **Logs antigos continuam no servidor.** A change não apaga o que já foi escrito. Ao colocar no ar, apagar ou rotacionar os arquivos e o histórico dos contêineres.
- **WPPConnect Server tem os próprios logs**, fora deste código. Na implantação, conferir o nível de log dele (variáveis de ambiente ou arquivo de configuração) e desligar o registro do conteúdo das mensagens, se houver.
- **Mensagens de exceção de bibliotecas** (Hibernate, Spring, Google) podem trazer dados. Por isso o código registra só a classe e o código HTTP, não `getMessage()`.
- **DEBUG ligado em produção** voltaria a expor o número mascarado e metadados. O padrão fica `INFO`, e `docker-compose.yml` não liga `DEBUG`.
- **Mudar `show-sql` para falso** esconde SQL durante o desenvolvimento. Ligar por variável de ambiente local.
