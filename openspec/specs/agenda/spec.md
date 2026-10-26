# Agenda

Comportamento observável da integração com a Google Agenda. A confirmação da reserva não segura a resposta do webhook.

## Requisitos

### AGD-01 Confirmar reserva sem bloquear o publicador

O sistema deve consumir `EventoReservaConfirmada` no `ObservadorGoogleAgenda` de forma assíncrona e agendar no Google. Quem publica o evento não espera o Google.

Contrato de arquitetura: o webhook HTTP não espera a agenda. Carga de referência: `k6/webhook-carga.js`
Prova: `src/test/resources/features/observer-agenda.feature`

Cenário: evento de reserva é agendado em background

- Dado uma empresa persistida com sessão e ramo `LOCACAO`
- Quando `EventoReservaConfirmada` é publicado para uma data
- Então o observador agenda no Google
- E o publicador não fica bloqueado nessa chamada
