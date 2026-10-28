# Design

## Decisão

A recusa fica no Java da `LocacaoStrategy`, antes de chamar o Google Agenda. Hoje é `LocalDate.now` no fuso que o fluxo da agenda já usa. Data passada na pesquisa e na reserva devolve mensagem própria e não muda o estado para o passo seguinte. Horário de hoje anterior à hora atual, na reserva por hora, segue a mesma regra.

Formato inválido continua na mensagem que já existe. Data futura ocupada continua "já está reservada".

## Arquivos
- Contrato: nenhum. O webhook em `especificacao/back/paths/webhook-whatsapp.yaml` não muda.
- Java: `src/main/java/com/example/demo/strategy/LocacaoStrategy.java`, nos passos `ESPERANDO_DATA`, `RESERVA_ESPERANDO_DATA` e `RESERVA_ESPERANDO_HORARIO`.
- Front: nenhum.
- Prova: `src/test/java/com/example/demo/strategy/LocacaoStrategyTest.java`

## Alinhamento
- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`

## Riscos
- Tratar "hoje" como passado. Hoje, com horário ainda válido, continua consultando a agenda.
- Depender do modelo para recusar a data. A regra fica no código, que é quem chama a agenda.
