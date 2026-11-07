## MODIFIED Requirements

### Requirement: WPP-12 Sessão da atendente da Gamb no WhatsApp

O sistema deve entregar a mensagem da empresa interna de ramo reservado `ATENDENTE_GAMB` à `AtendenteGambStrategy`, sem mudar o roteamento nem as strategies das empresas clientes (WPP-01). Todo contato dessa sessão é tratado como cliente. A primeira mensagem, e `0` em qualquer passo, abre o menu "Tirar uma dúvida" (`1`), "Marcar reunião" (`2`) ou "Conhecer a Gamb" (`3`). Na opção `3`, e no texto livre que pergunta do que se trata a Gamb, o que ela oferece ou como ela resolve o problema do cliente (no menu e na dúvida), o bot responde pela base comercial da Gamb: quem é, o que existe hoje e o que está em desenvolvimento, ligando o problema descrito ao que existe hoje, sem informar valor ou faixa de preço, sem prometer prazo, vaga ou garantia, sem citar cliente nem comparar com concorrente, e termina convidando a digitar *2* para marcar reunião. Assunto fora da Gamb recebe a recusa comercial. Na dúvida, o bot responde pelo Suporte de `AssistenteTutoriaService`, só com a base de conhecimento e os avisos ativos, sem enviar ao modelo nem devolver dado de empresa e sem executar ação no sistema. A resposta conversa como uma pessoa da equipe: responde primeiro o que foi perguntado, acompanha o jeito de escrever do cliente e varia a forma de falar, sem roteiro fixo, dentro do mesmo escopo. Mensagem fora do escopo recebe a recusa padrão do Suporte seguida de "Para consultar os dados da sua empresa, use o painel do app.". Mensagem filtrada, recusada ou respondida pelo bot devolve 200 ao webhook. O número sai mascarado e o texto só pelo tamanho no log (WPP-06).

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/java/com/example/demo/strategy/AtendenteGambStrategyTest.java`, `src/test/java/com/example/demo/strategy/GambStrategiesLogTest.java`, `src/test/resources/features/webhook.feature`, `src/test/resources/features/roteamento-strategy.feature`

#### Scenario: primeira mensagem abre o menu

- **GIVEN** uma empresa persistida com a sessão da atendente e ramo `ATENDENTE_GAMB`
- **WHEN** chega POST `/webhook/whatsapp` com `type=chat` e texto "Oi"
- **THEN** a API responde 200
- **AND** a `FactoryModulo` entrega a mensagem à `AtendenteGambStrategy`
- **AND** o bot responde o menu de dúvida e reunião

#### Scenario: cliente tira dúvida no número da Gamb

- **GIVEN** o cliente na opção de dúvida da sessão da atendente
- **WHEN** pergunta como conectar o WhatsApp
- **THEN** o bot responde com o passo a passo da base de conhecimento

#### Scenario: cliente pede dado de empresa no número da Gamb

- **GIVEN** o cliente na opção de dúvida da sessão da atendente
- **WHEN** pede preços, reservas ou dados de uma empresa
- **THEN** o bot responde a recusa padrão do Suporte e orienta a usar o painel
- **AND** nenhum dado de empresa é consultado nem devolvido

#### Scenario: strategies das empresas não mudam

- **GIVEN** as strategies carregadas no contexto Spring
- **WHEN** a `FactoryModulo` busca o ramo "Locação"
- **THEN** a strategy retornada continua a `LocacaoStrategy`

#### Scenario: cliente pergunta do que se trata a Gamb

- **GIVEN** o cliente no menu da sessão da atendente
- **WHEN** ele digita *3* ou escreve "o que vocês fazem?"
- **THEN** o bot responde quem é a Gamb e o que ela oferece hoje
- **AND** a resposta não traz valor, prazo nem nome de cliente
- **AND** termina convidando a digitar *2* para marcar reunião

#### Scenario: cliente descreve o problema dele

- **GIVEN** o cliente na sessão da atendente
- **WHEN** ele escreve "perco reservas porque demoro a responder, como vocês resolvem?"
- **THEN** o bot liga o problema ao assistente no WhatsApp com reserva pela Google Agenda
- **AND** apresenta o que está em desenvolvimento como avaliado caso a caso, sem prometer
- **AND** não informa preço nem prazo

#### Scenario: pergunta do app continua no Suporte

- **GIVEN** o cliente na opção de dúvida
- **WHEN** ele pergunta como conectar o WhatsApp no app
- **THEN** o bot responde pelo Suporte, com o passo a passo, e não pela resposta comercial

#### Scenario: resposta de dúvida com tom de conversa

- **GIVEN** o cliente na opção de dúvida da sessão da atendente
- **WHEN** a pergunta é enviada ao Suporte
- **THEN** o prompt orienta responder primeiro o que foi perguntado, acompanhar o tom do cliente e variar a forma de falar
- **AND** o prompt mantém o escopo: sem dado de empresa e sem ação no sistema

### Requirement: WPP-13 Reunião com a equipe da Gamb pelo WhatsApp

O sistema deve, na sessão da atendente, deixar o cliente marcar reunião com a equipe informando o motivo (`AJUSTE` ou `NOVA_INOVACAO`), a data (escrita em qualquer formato usual, como `25/12/2026`, `25 de dezembro` ou `amanhã`, de hoje até o limite de dias à frente do funcionamento e em dia aberto; o bot responde sempre no formato DD-MM-AAAA) e o horário (HH:MM, entre os horários livres) e confirmando o resumo. Quando a data é duvidosa (apenas o dia, dia da semana, data sem ano que já passou neste ano ou data impossível), o bot pergunta qual data o cliente quis dizer antes de seguir, aceita *sim* ou a data corrigida e não consulta a agenda enquanto a dúvida não for resolvida. Com o horário livre na Google Agenda da empresa interna, conferido de novo na confirmação, o sistema cria o evento pelo `ServicoReserva` com o motivo no título, o número do cliente mascarado na descrição e todos os `emailsResponsaveisReuniao` daquele momento como convidados, com convite por e-mail, e responde "Reunião marcada para <data> às <horário>.". Com o horário ocupado, o bot oferece os horários livres e não cria evento; sem horário livre na data, pede outra data. Sem responsáveis cadastrados, sem agenda configurada ou com a agenda desconectada ou falhando, o bot informa que o agendamento está indisponível, não cria evento e mantém a dúvida funcionando. Ao mudar a lista de responsáveis (EMP-14), o sistema atualiza só os convidados das reuniões futuras marcadas pelo atendente (entram os e-mails novos, saem os removidos) e não cancela nem remarca reunião; reunião cancelada na agenda é ignorada. O e-mail dos responsáveis não aparece em log.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`, `especificacao/back/paths/empresas-admin.yaml`
Prova: `src/test/java/com/example/demo/strategy/AtendenteGambStrategyTest.java`, `src/test/java/com/example/demo/servico/ServicoReservaReuniaoTest.java`, `src/test/java/com/example/demo/strategy/GambStrategiesLogTest.java`, `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`

#### Scenario: cliente marca reunião em horário livre

- **GIVEN** responsáveis cadastrados e a Google Agenda da Gamb conectada
- **WHEN** o cliente escolhe marcar reunião, informa motivo, data e horário livres e confirma o resumo
- **THEN** o evento é criado com os responsáveis convidados por e-mail
- **AND** o bot responde com a data e o horário da reunião

#### Scenario: cliente escolhe horário ocupado

- **GIVEN** um horário já ocupado na Google Agenda da Gamb
- **WHEN** o cliente informa esse horário, ou o horário fica ocupado antes da confirmação
- **THEN** o bot oferece outros horários
- **AND** nenhum evento é criado

#### Scenario: agendamento indisponível

- **GIVEN** a lista de responsáveis vazia, ou a agenda desconectada
- **WHEN** o cliente escolhe marcar reunião
- **THEN** o bot informa que o agendamento está indisponível e nenhum evento é criado
- **AND** a opção de dúvida continua respondendo

#### Scenario: lista de responsáveis muda

- **GIVEN** reuniões futuras marcadas pelo atendente
- **WHEN** o admin salva uma nova lista de responsáveis
- **THEN** só os convidados dessas reuniões são atualizados
- **AND** título, data e horário ficam como estão e nada é cancelado
- **AND** nenhum e-mail aparece em log

#### Scenario: cliente escreve a data do jeito que preferir

- **GIVEN** o cliente no passo de data da reunião
- **WHEN** ele escreve "25 de dezembro" ou "25.12" e a data cai no futuro dentro do limite
- **THEN** o bot segue para os horários livres daquele dia
- **AND** nenhuma mensagem pede o formato DD-MM-AAAA

#### Scenario: data duvidosa pergunta antes de seguir

- **GIVEN** o cliente no passo de data da reunião
- **WHEN** ele escreve "sábado" ou "dia 25"
- **THEN** o bot pergunta se ele quer dizer a data sugerida, com o dia da semana
- **AND** ao receber "sim", segue para os horários livres daquela data
- **AND** ao receber outra data, usa a data corrigida
- **AND** ao receber "não", volta a pedir a data
