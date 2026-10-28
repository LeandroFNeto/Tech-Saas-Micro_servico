# Design

## Decisão

1. **Escopo: só diária.** Esta change registra a reserva automática de diária. Empresa com `locacaoPorHora` ligado é tratada como reserva manual em `SOLICITAR_RESERVA` ("Recebemos sua mensagem. Logo um atendente vai te responder.") até a change `reserva-por-hora` entrar. Sobreposição de horários, duração e evento com hora ficam nela.
2. **Estado novo `RESERVA_REGISTRANDO`.** No "sim", a `LocacaoStrategy`:
   - se o estado já for `RESERVA_REGISTRANDO`, não publica de novo (segundo "sim" ou reenvio do webhook);
   - gera um `idPedido` (UUID) e copia data, horário e nome da sessão para o `EventoReservaConfirmada`, porque a sessão é limpa quando o estado volta a `INICIO`;
   - muda o estado para `RESERVA_REGISTRANDO` antes de publicar e responde "Anotei seu pedido para <data>. Estou registrando na agenda e já confirmo.".

   Em `RESERVA_REGISTRANDO`, qualquer mensagem, inclusive `0`, `menu` e `voltar`, recebe "Ainda estou registrando sua reserva, já te respondo." O teste desse estado vem antes do comando de menu em `processarMensagem`.
3. **Quem encerra o estado.** O observador, ao terminar, chama `GerenciadorSessao.concluirRegistro(numero, novoEstado)`, que só troca o estado se ele ainda for `RESERVA_REGISTRANDO` (evita sobrescrever uma sessão que expirou ou recomeçou):
   - sucesso e falha: `INICIO`;
   - data ocupada: `RESERVA_ESPERANDO_DATA`, mantendo nome e demais dados da reserva.
4. **Uma só consulta de disponibilidade e o bloqueio.** A reconsulta acontece apenas no observador, sob o bloqueio, com `ServicoGoogleagenda.dataEstaLivre(calendarId, data)`, o mesmo método que a conversa já usa. A estratégia não consulta de novo, então o webhook não espera o Google (AGD-01). `ServicoReserva.pesquisarData` não entra no fluxo.
    - O bloqueio é um mapa `ConcurrentHashMap<String, ReentrantLock>` dentro do `ObservadorGoogleAgenda`, com chave `empresaId + data`. Ele cobre só a reconsulta e a inserção, nunca a espera entre tentativas nem o envio de mensagem.
5. **Evento criado de verdade.** `agendarNoGoogle(empresa, data, nome, whatsappId, idPedido)` insere o evento e devolve o `eventId`:
   - título "Reserva - <nome>"; descrição "Reserva feita pelo bot. WhatsApp: <número>";
   - dia inteiro: `start` = data e `end` = data + 1 dia, porque a data final do Google é exclusiva;
   - sem `googleCalendarId`: lança `AgendaNaoConfiguradaException`;
   - falha do Google: lança a exceção original.
6. **Idempotência.** O id do evento é derivado do `idPedido` (hash em base32hex minúsculo, que é o alfabeto aceito pelo Google). Repetir a inserção do mesmo pedido devolve 409, tratado como sucesso (o evento já existe). O id não usa empresa e data, porque o Google não permite reaproveitar o id de um evento apagado: se o dono apagasse o evento, uma reserva futura da mesma data falharia com 409 e seria dada como sucesso.
7. **Resultado real e tentativas.** O `ObservadorGoogleAgenda`:
   - sucesso: "✅ Reserva confirmada" com a data;
   - data ocupada na reconsulta: mensagem de data ocupada, sem inserir;
   - falha: 3 tentativas no total, com 2 e 5 segundos de espera. O bloqueio é solto durante a espera e retomado em cada tentativa.
   - Primeira tentativa: reconsulta a data e, estando livre, insere.
   - Da segunda em diante: antes de reconsultar, busca o evento pelo id derivado do `idPedido` (`events.get`). Se existir, é sucesso, porque a tentativa anterior chegou ao Google e só a resposta se perdeu. Se não existir, reconsulta e insere. Sem isso, o evento do próprio cliente faria a data parecer ocupada e ele receberia "data acabou de ser reservada" com a reserva já feita.
   - Repete em erro 5xx, 429, falha de rede (`IOException` sem status HTTP) e 403 com motivo `rateLimitExceeded` ou `userRateLimitExceeded` (o Google usa 403 para limite de uso). Falham na hora: 400, 401, 404, 403 de permissão (calendário só leitura) e `IllegalStateException` de credencial ausente em `conectarAgenda`;
   - o fim do método usa `try/finally` e sempre chama `concluirRegistro`, qualquer que seja a falha, inclusive no envio da mensagem;
   - depois da última falha, "Não consegui registrar sua reserva agora. O responsável vai confirmar com você em breve." e `log.error` com o id da empresa, o `idPedido` e a classe da exceção, sem número, nome nem texto do cliente (`MascaraLog`, da change `proteger-dados-nos-logs`, já aprovada);
   - `AgendaNaoConfiguradaException`: mesma mensagem de falha, sem repetir.
8. **Sem agenda, vira reserva manual desde o início.** Com a reserva automática ligada e `googleCalendarId` vazio, `SOLICITAR_RESERVA` responde como reserva manual e grava `log.warn` com o id da empresa. O cliente final não percorre data, nome e confirmação para receber a falha no fim. A exceção do item 7 continua como rede de segurança para a agenda removida no meio do caminho.
9. **Regra no cadastro.** Ligar `permiteReservaAutomatica` exige `googleCalendarId` preenchido no `POST` do admin e no `PUT` do admin: 400 com "Cadastre a agenda do Google antes de ligar a reserva automática.". A agenda não é obrigatória no cadastro: o campo nasce `false`, e empresa que não usa agenda segue como reserva manual. Empresas já ligadas e sem agenda continuam como estão e caem na decisão 8.
10. **Agenda removida com a opção ligada.** O admin pode apagar `googleCalendarId` de empresa com `permiteReservaAutomatica` ligada. A empresa passa a atender como reserva manual (decisão 8), sem erro, e o log registra. Fica como está de propósito: a regra de ligar exige agenda, a de manter não.
11. **Opcional e só do admin.** O `PUT /{sessao}/cliente` não altera `permiteReservaAutomatica`:
    - valor igual ao atual: aceito e ignorado;
    - valor diferente: 400 com "A reserva automática é liberada pelo administrador.", e nada é gravado.

    O perfil do cliente mostra a opção só para leitura e não envia o campo. É um ajuste provisório até os planos liberarem a opção.

## Arquivos

- Contrato:
  - `especificacao/back/paths/empresas-admin.yaml` (400 da regra da agenda no POST e no PUT)
  - `especificacao/back/paths/empresas-cliente.yaml` (400 da regra do admin)
- Java:
  - `model/EstadoUsuario.java` (`RESERVA_REGISTRANDO`)
  - `servico/GerenciadorSessao.java` (`concluirRegistro`)
  - `evento/EventoReservaConfirmada.java` (horário, número original, nome, `idPedido`)
  - `strategy/LocacaoStrategy.java` (estado novo, segundo "sim", menu bloqueado, reserva por hora e sem agenda como manual, resposta "anotei")
  - `servico/ServicoReserva.java` (`agendarNoGoogle` com evento de dia inteiro e id determinístico)
  - `servico/AgendaNaoConfiguradaException.java` (novo)
  - `servico/observador/ObservadorGoogleAgenda.java` (resultado real, tentativas, bloqueio, estado final)
  - `controller/ControllerEmpresa.java` (POST e PUT do admin validam a agenda; PUT do cliente rejeita mudança)
- Front:
  - Antes do Angular: canvas de design, board `ClienteChatbot` (a configuração do chatbot, rota `/cliente/chatbot`) com a opção só para leitura e o aviso de que o admin libera; mesmo cuidado na tela do admin para o 400 da agenda
  - `perfil.component`: opção só para leitura, com aviso de que o admin libera, e sem enviar o campo
  - tela do admin: mostrar o 400 da regra da agenda
- Prova:
  - `LocacaoStrategyTest`: "sim" publica o evento e entra em `RESERVA_REGISTRANDO`; segundo "sim" não publica; `0` em `RESERVA_REGISTRANDO` não sai do estado; reserva por hora e empresa sem agenda respondem como manual
  - `GerenciadorSessaoTest`: `concluirRegistro` só troca de `RESERVA_REGISTRANDO`
  - `src/test/resources/features/observer-agenda.feature`: sucesso cria o evento e confirma; 409 conta como sucesso; falha 5xx tenta 3 vezes e avisa; 403 de permissão falha sem repetir; 403 de limite de uso repete; credencial ausente falha sem repetir; primeira tentativa chega ao Google e a resposta se perde, a segunda acha o evento pelo id e confirma sem dizer "data ocupada"; falha no envio da mensagem ainda leva a conversa a `INICIO`; agenda não configurada avisa; data ocupada volta a pedir data; duas confirmações da mesma data, só uma reserva
  - `src/test/resources/features/empresas.feature`: POST e PUT do admin ligando sem agenda respondem 400; POST sem agenda e sem ligar funciona; PUT do cliente com valor diferente responde 400 e um GET seguinte mostra o valor antigo; PUT do cliente com valor igual funciona

## Alinhamento

- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`
- O webhook HTTP não espera a agenda (AGD-01)

## Riscos

- **A agenda do cliente precisa estar compartilhada com a conta do sistema com permissão de edição.** Calendário compartilhado só para leitura falha na inserção (403) e o cliente final recebe a mensagem de falha.
- **O dono só fica sabendo da falha pelo log** até a change `lembrar-confirmacao-reserva` entrar. O cliente final recebe a promessa de que o responsável vai confirmar, sem alguém ser avisado. Por isso a reserva automática continua liberada só pelo admin.
- **Reinício da API durante o registro.** Sessão e evento ficam em memória: se a API reiniciar entre o "sim" e o resultado, o cliente final não recebe resposta e a reserva pode não existir. Com uma instância e escala pequena, o risco é aceito e o `log.error` não cobre esse caso.
- **Duas reservas para a mesma data durante a espera entre tentativas.** O bloqueio é solto na espera. Quem tenta de novo busca o próprio evento e depois reconsulta, então a segunda pessoa vê a data ocupada. A janela entre a reconsulta e a inserção fica sob o bloqueio.
- **Bloqueio em memória:** premissa de operação com uma instância da API. Com mais de uma, a corrida entre duas confirmações volta e o bloqueio precisa ir para o banco.
- **Telefone na descrição do evento:** fica na agenda do próprio dono, e a change `lembrar-confirmacao-reserva` aproveita isso para ligar a reserva ao contato.

## Riscos conhecidos para outra change

Achados baixos de `seguranca.md`, aceitos aqui e não implementados:

- **Sessão por número, não por empresa:** o mesmo número em `RESERVA_REGISTRANDO` numa empresa recebe "Ainda estou registrando" na outra, e o `concluirRegistro` de uma muda o estado da conversa com a outra.
- **Sem limite de reservas por número nem de data distante:** um número pode reservar todas as datas livres, até `31-12-9999`, e cada pedido deixa uma entrada permanente no mapa de bloqueios.
- **"Sim" simultâneo:** ler e trocar o estado não é atômico, então dois webhooks ao mesmo tempo publicam dois eventos e o cliente pode receber "data acabou de ser reservada" logo depois de "confirmada".
- **Nome sem limite de tamanho:** o nome entra cru no título do evento, e um nome enorme só faz a inserção falhar.
