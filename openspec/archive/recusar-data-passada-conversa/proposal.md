# recusar-data-passada-conversa

## Problema

No WhatsApp de locação, a pesquisa de data (`ESPERANDO_DATA`) e a reserva (`RESERVA_ESPERANDO_DATA`) aceitam qualquer dia que caiba no calendário, inclusive dia que já passou. O bot segue para o Google Agenda. O horário de hoje que já passou também entra na reserva por hora.

## O que muda
### Entra
- Data anterior a hoje, no fuso de Brasília, recebe uma resposta de erro e não consulta a agenda.
- Na reserva por hora, data de hoje com horário que já passou recebe uma resposta de erro e não consulta a agenda.
- A conversa permanece no passo em que o cliente informa data ou horário, para ele enviar outro valor.

### Altera
- `LocacaoStrategy` passa a recusar esses casos antes de `ServicoGoogleagenda`.

### Sai
- Nada de rota, DTO ou tela.

## Fora de escopo

- Consulta GraphQL `POST /api/ia-query`
- Texto livre enviado ao Gemini
- Data ocupada no futuro, formato inválido e agenda sem ID: esses erros já existem e permanecem
- Limite de quantos dias à frente o cliente pode pedir

## Domínios
- spec: `openspec/specs/whatsapp/spec.md`
