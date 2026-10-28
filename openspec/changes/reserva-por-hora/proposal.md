# reserva-por-hora

## Problema

Empresas com `locacaoPorHora` precisam de reserva automática por horário, e hoje isso não existe. A change `corrigir-reserva-automatica` cobre só a diária e deixa essas empresas como reserva manual.

## Pontos já conhecidos (a detalhar antes de virar proposta)

- Disponibilidade por sobreposição de horário: hoje `dataEstaLivre` e `pesquisarData` marcam o dia inteiro como ocupado com qualquer evento, o que deixaria uma reserva por dia.
- Duração da reserva por hora: não existe campo em `Empresa`; decidir com o dono do produto (por empresa ou fixa).
- Evento com hora, em `America/Sao_Paulo`, e conflito entre diária e reserva por hora no mesmo dia.
- Mostrar horários livres ao cliente final.

Vem depois de `corrigir-reserva-automatica`.
