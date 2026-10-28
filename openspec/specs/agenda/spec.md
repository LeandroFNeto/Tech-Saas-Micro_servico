# Agenda

Comportamento observável da integração com a Google Agenda. A confirmação da reserva não segura a resposta do webhook.

## Requisitos

### AGD-01 Confirmar reserva sem bloquear o publicador

O sistema deve consumir `EventoReservaConfirmada` no `ObservadorGoogleAgenda` de forma assíncrona e registrar no Google. Quem publica o evento não espera o Google. O evento carrega empresa, data, horário, nome do cliente final, número do WhatsApp e um `idPedido` próprio, porque a sessão da conversa é limpa ao voltar ao início. O observador não assume sucesso: a mensagem ao cliente final segue o resultado real do registro (AGD-02).

Contrato de arquitetura: o webhook HTTP não espera a agenda. Carga de referência: `k6/webhook-carga.js`
Prova: `src/test/resources/features/observer-agenda.feature`

Cenário: evento de reserva é agendado em background

- Dado uma empresa persistida com sessão e ramo `LOCACAO`, com agenda Google e a data livre
- Quando `EventoReservaConfirmada` é publicado para uma data
- Então o observador registra no Google
- E o publicador não fica bloqueado nessa chamada
- E o cliente final recebe "Reserva confirmada" só depois do registro

### AGD-02 Registrar a reserva automática no Google com resultado real

O sistema deve criar no Google Agenda da empresa, para cada `EventoReservaConfirmada`, um evento de dia inteiro (início na data, fim no dia seguinte) com título "Reserva - <nome>" e descrição "Reserva feita pelo bot. WhatsApp: <número>". O id do evento é derivado do `idPedido` (SHA-256 em base32hex minúsculo), então repetir o mesmo pedido não duplica a reserva e o 409 do Google conta como sucesso.

Antes de inserir, o observador consulta de novo a disponibilidade da data sob um bloqueio por empresa e data, que cobre só a reconsulta e a inserção. Data ocupada não insere. Em falha transitória (5xx, 429, falha de rede e 403 com motivo `rateLimitExceeded` ou `userRateLimitExceeded`) faz 3 tentativas no total, com espera de 2 e 5 segundos fora do bloqueio; da segunda tentativa em diante, procura antes o evento pelo id do pedido e, se ele existir e não estiver cancelado, conta como sucesso. Falham na hora, sem repetir: 400, 401, 404, 403 de permissão, credencial ausente e agenda não configurada (`AgendaNaoConfiguradaException`).

Ao terminar, sempre chama `GerenciadorSessao.concluirRegistro`, que só troca o estado se a conversa ainda estiver em `RESERVA_REGISTRANDO`: sucesso e falha voltam a `INICIO`; data ocupada volta a `RESERVA_ESPERANDO_DATA`, mantendo o nome já informado. Isso vale mesmo se o envio da mensagem falhar. A falha final grava `log.error` com o id da empresa, o `idPedido` e a classe da exceção, sem número, nome ou texto do cliente. Só reserva de diária entra; empresa com locação por hora segue como reserva manual (WPP-07).

Contrato: nenhum (não há rota nem DTO de API novo)
Prova: `src/test/resources/features/observer-agenda.feature`, `src/test/java/com/example/demo/servico/ServicoReservaAgendarTest.java`, `src/test/java/com/example/demo/servico/observador/ObservadorGoogleAgendaTest.java`, `src/test/java/com/example/demo/servico/GerenciadorSessaoTest.java`

Cenário: sucesso cria o evento e confirma

- Dado uma empresa com agenda Google, a data livre e a conversa em `RESERVA_REGISTRANDO`
- Quando `EventoReservaConfirmada` é publicado para "25-12-2026"
- Então o Google recebe 1 inserção de evento de dia inteiro
- E o cliente final recebe a mensagem com "Reserva confirmada" e a data
- E a conversa volta a `INICIO`

Cenário: 409 do Google conta como sucesso

- Dado a inserção respondendo 409 porque o evento do pedido já existe
- Quando o evento é publicado
- Então o cliente final recebe "Reserva confirmada"

Cenário: falha transitória que persiste

- Dado o Google respondendo 5xx em todas as tentativas
- Quando o evento é publicado
- Então o Google recebe 3 tentativas de inserção
- E o cliente final recebe "Não consegui registrar sua reserva agora. O responsável vai confirmar com você em breve."
- E a conversa volta a `INICIO`

Cenário: 403 de permissão ou credencial ausente

- Quando a inserção falha com 403 de permissão ou a credencial do Google não existe
- Então o Google recebe no máximo 1 tentativa
- E o cliente final recebe a mensagem de falha

Cenário: 403 de limite de uso

- Dado a primeira inserção respondendo 403 com `rateLimitExceeded`
- Quando o evento é publicado
- Então o observador tenta de novo
- E o cliente final recebe "Reserva confirmada" quando a nova tentativa passa

Cenário: a resposta da primeira tentativa se perde

- Dado a primeira inserção chegando ao Google e a resposta voltando com erro transitório
- Quando o observador tenta de novo
- Então acha o evento pelo id do pedido e confirma
- E o cliente final não recebe aviso de data ocupada

Cenário: agenda não configurada

- Dado a empresa sem `googleCalendarId`
- Quando o evento é publicado
- Então o Google não é chamado
- E o cliente final recebe a mensagem de falha

Cenário: data ocupada na reconsulta

- Dado a data ocupada no Google no momento do registro
- Quando o evento é publicado
- Então nada é inserido
- E o cliente final recebe "Essa data acabou de ser reservada" com o pedido de outra data
- E a conversa volta a `RESERVA_ESPERANDO_DATA`

Cenário: duas confirmações da mesma data

- Quando duas confirmações da mesma data e da mesma empresa são publicadas ao mesmo tempo
- Então o Google recebe 1 inserção
- E sai uma confirmação e um aviso de data ocupada

Cenário: falha no envio da mensagem

- Dado o envio da mensagem ao WhatsApp falhando
- Quando o registro termina
- Então a conversa sai de `RESERVA_REGISTRANDO` mesmo assim
