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

Ao terminar, sempre chama `GerenciadorSessao.concluirRegistro`, que só troca o estado se a conversa ainda estiver em `RESERVA_REGISTRANDO`: sucesso e falha voltam a `INICIO`; data ocupada volta a `RESERVA_ESPERANDO_DATA`, mantendo o nome já informado. Isso vale mesmo se o envio da mensagem falhar. A falha final grava `log.error` com o id da empresa, o `idPedido` e a classe da exceção, sem número, nome ou texto do cliente. Este fluxo é o da reserva de diária; a reserva por hora tem o fluxo de AGD-03.

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

### AGD-03 Registrar a reserva por hora sem conflito, de uma ou de várias datas

O sistema deve, para empresa com locação por hora e reserva automática, criar no Google Agenda um evento com hora (início e fim com fuso `America/Sao_Paulo`) para cada data do pedido, com título "Reserva - <nome>" e a mesma descrição de AGD-02. O fim é o início mais a duração do atendimento da data. O id de cada evento deriva de `idPedido#posição`, então repetir o pedido não duplica e o 409 conta como sucesso.

Um horário conflita com qualquer evento que o cruze; terminar exatamente quando outro começa não conflita; evento de dia inteiro conflita com tudo no dia. O intervalo entre atendimentos da empresa (EMP-07) é somado antes e depois do atendimento pedido, mas o evento no Google guarda só o atendimento. Agenda com mais de 500 eventos no período consultado pode esconder conflito.

Antes de inserir, o observador pega o bloqueio de cada data (empresa e data) em ordem crescente de data, reconsulta a janela de funcionamento (EMP-07) e a disponibilidade de cada data, e só então insere. O resultado é tudo ou nada: se uma data está fechada, ocupada ou fora da janela, nada é criado, o cliente recebe "Nada foi reservado" com as datas que caíram, só elas saem do pedido e a conversa volta a `RESERVA_ESPERANDO_DATA`. As tentativas e o `concluirRegistro` seguem como em AGD-02, e a nova tentativa pula os eventos que já existem.

Se a inserção falha de vez no meio do pedido, o observador apaga os eventos já criados dele (404 e 410 contam como apagado) e avisa o cliente como em AGD-02. Se apagar também falha, grava `log.error` com o id da empresa, o `idPedido` e a classe da exceção, sem dado pessoal. Agenda não configurada não aciona a compensação. A compensação só roda depois da falha final, nunca entre tentativas, porque o Google não deixa reusar o id de um evento apagado.

Contrato: nenhum (não há rota nem DTO de API novo)
Prova: `src/test/resources/features/observer-agenda.feature`, `src/test/java/com/example/demo/servico/observador/ObservadorGoogleAgendaPorHoraTest.java`, `src/test/java/com/example/demo/servico/ServicoGoogleagendaSobreposicaoTest.java`

Cenário: várias datas criam um evento com hora para cada data

- Dado uma empresa por hora com agenda Google e o horário livre em todas as datas
- Quando `EventoReservaConfirmada` é publicado para "10-11-2026" e "17-11-2026" às "14:00"
- Então o Google recebe 2 inserções com hora
- E o cliente final recebe "Reserva confirmada" com as duas datas e o horário
- E a conversa volta a `INICIO`

Cenário: uma data ocupada derruba o pedido inteiro

- Dado o horário ocupado em "17-11-2026"
- Quando o evento é publicado para as duas datas
- Então o Google recebe 0 inserções
- E o cliente final recebe "Nada foi reservado" e a data "17-11-2026"
- E a conversa volta a `RESERVA_ESPERANDO_DATA`

Cenário: falha 5xx desfaz o pedido

- Dado o Google respondendo 503 em toda inserção
- Quando o evento é publicado para duas datas
- Então o Google recebe 3 tentativas de inserção
- E o Google remove os 2 eventos do pedido
- E o cliente final recebe a mensagem de falha
- E a conversa volta a `INICIO`

### AGD-04 Mostrar a disponibilidade por hora na agenda e na pesquisa de data

O sistema deve, para empresa com locação por hora, calcular o status de cada dia pelos horários livres da janela de funcionamento (EMP-07) menos as ocupações do Google, e não pelo dia inteiro: o dia é "Lotado" quando nenhum início cabe, "Fechado" quando a janela está fechada e "Livre" quando sobra ao menos um início. O painel de reservas e a previsão do tempo (EMP-06) marcam "Alugado" quando o dia está lotado ou fechado e "Disponível" quando está livre. A agenda dos próximos 15 dias do bot (WPP-09) e o painel dos 30 dias usam uma só consulta ao Google para o período.

Contrato: `especificacao/back/paths/empresas-previsao.yaml` (o campo de status por dia não muda de formato)
Prova: `src/test/java/com/example/demo/servico/ServicoFuncionamentoTest.java`, `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`, `src/test/java/com/example/demo/servico/ServicoReservaListarTest.java`.

Cenário: dia com uma reserva ainda tem vaga

- Dado uma empresa por hora com janela das 08:00 às 10:00 e duração de 60 minutos, e um evento das 08:00 às 09:00
- Quando o status do dia é calculado
- Então o dia é "Livre"

Cenário: todos os horários ocupados

- Dado um evento das 08:00 às 10:00 na mesma janela
- Quando o status do dia é calculado
- Então o dia é "Lotado"


### AGD-05 Sincronizar reservas da Google Agenda para confirmação

O sistema deve consultar periodicamente (a cada hora) os eventos dos próximos 30 dias na agenda Google das empresas que possuem lembretes ativados, criando ou atualizando reservas locais e vinculando ao contato do cliente correspondente quando identificado no título ou na descrição.

Contrato: `especificacao/back/paths/empresas-reservas.yaml`
Prova: `src/test/java/com/example/demo/servico/ServicoSincronizacaoAgendaTest.java`

Cenário: evento ativo cria reserva
- Dado uma empresa com lembretes ativados e evento na Google Agenda
- Quando a rotina de sincronização é executada
- Então a reserva é persistida localmente com os dados do evento

### AGD-06 Gravar reserva do bot e vincular contato

O sistema deve, ao confirmar uma reserva automática pelo chatbot ou por locação, registrar a reserva localmente com o identificador do evento no Google Agenda e vincular ao contato identificado pelo WhatsApp.

Contrato: `especificacao/back/paths/empresas-reservas.yaml`
Prova: `src/test/java/com/example/demo/servico/observador/ObservadorGoogleAgendaTest.java`

Cenário: reserva do bot registrada
- Dado um pedido de reserva confirmado pelo cliente
- Quando o evento de confirmação é processado pelo observador
- Então a reserva é gravada vinculada ao contato correspondente
