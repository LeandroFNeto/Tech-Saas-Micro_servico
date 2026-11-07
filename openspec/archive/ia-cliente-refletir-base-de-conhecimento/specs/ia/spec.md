## ADDED Requirements

### Requirement: IA-07 A IA do cliente final reflete o documento base e o menu real da empresa

O sistema SHALL montar o prompt da IA de atendimento ao cliente final para que ela converse como uma pessoa da equipe da empresa: responde primeiro o que foi perguntado, acompanha o tom do cliente, varia a forma de falar e só lembra do menu quando ajuda, sem roteiro fixo de frases nem fechamento obrigatório. O prompt também traz a ideia do produto e as regras de conduta do documento base, com o menu real da empresa (somente as opções ativas, na numeração que o cliente vê) e com os módulos ativos e o modo de locação. A IA SHALL NOT indicar número de opção que não exista no menu da empresa, informar preço ou horário que não esteja cadastrado, confirmar ou prometer reserva, nem expor credencial, identificador de agenda ou dado de outra empresa. Sem a resposta nos dados cadastrados, a IA SHALL dizer que um atendente vai responder. Falha ou cota da IA continuam caindo no menu determinístico (IA-06).

Prova: `src/test/java/com/example/demo/servico/ServicoIATest.java` e `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`

#### Scenario: conversa sem roteiro
- **WHEN** o prompt do atendimento é montado
- **THEN** ele orienta a IA a responder o que foi perguntado e a acompanhar o tom do cliente
- **AND** não impõe número fixo de frases nem o lembrete do menu em toda resposta

#### Scenario: menu real no prompt
- **GIVEN** uma empresa com `usaIA` verdadeiro e apenas as opções "Fotos" e "Fazer reserva" ativas
- **WHEN** o prompt do atendimento é montado
- **THEN** o prompt lista essas duas opções com a numeração do menu da empresa
- **AND** não cita agenda nem pesquisa de data

#### Scenario: conduta sobre reserva
- **WHEN** o prompt do atendimento é montado
- **THEN** ele orienta a IA a tratar reserva como solicitação feita pelo menu e a não confirmá-la

#### Scenario: sem dado cadastrado
- **GIVEN** uma empresa sem tabela de preços cadastrada
- **WHEN** o cliente pergunta quanto custa
- **THEN** o prompt orienta a IA a não informar valor e a dizer que um atendente vai responder

#### Scenario: nada sensível no prompt
- **WHEN** o prompt do atendimento é montado
- **THEN** ele não contém credencial, `googleCalendarId` nem dado de outra empresa

### Requirement: IA-08 Data em qualquer formato, com pergunta na dúvida

O sistema SHALL reconhecer a data que o cliente escreve em qualquer formato usual (por exemplo `25/12/2026`, `25-12-26`, `25.12`, `25 12 2026`, `25 de dezembro`, `2026-12-25`, `hoje`, `amanhã`), inclusive no meio de uma frase, e encaminhá-la ao sistema do bot para consultar a agenda ou seguir a reserva. Quando a data for duvidosa (apenas o dia, dia da semana, data sem ano que já passou neste ano ou data impossível), o sistema SHALL perguntar ao cliente qual data ele quer dizer antes de consultar a agenda e SHALL NOT inventar disponibilidade.

Prova: `src/test/java/com/example/demo/servico/InterpretadorDeDataTest.java` e `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`

#### Scenario: data escrita livremente
- **GIVEN** uma empresa com agenda configurada
- **WHEN** o cliente escreve "tem a data 25/12/2026 livre?"
- **THEN** o sistema consulta a agenda daquela data e responde se está livre

#### Scenario: data duvidosa
- **WHEN** o cliente escreve "tem sábado livre?"
- **THEN** o sistema pergunta se ele quer dizer o próximo sábado, com a data
- **AND** ao receber "sim", consulta a agenda daquela data

#### Scenario: cliente corrige a sugestão
- **GIVEN** o sistema perguntou qual data o cliente quis dizer
- **WHEN** o cliente responde com outra data
- **THEN** o sistema consulta a agenda da data corrigida
