# fechar-pendencias-reserva-por-hora

## Problema

A change `reserva-por-hora` foi implementada, provada e arquivada (`openspec/archive/reserva-por-hora/`, commit `44cae30`), mas deixou pendências que não bloqueiam o uso e foram separadas para serem tratadas à parte:

- a tabela de feriados estaduais foi montada de memória e nunca foi conferida com fonte oficial;
- `HorarioReserva` e `DiaReserva` (tabelas `horarios_reserva` e `dias_reserva`) parecem legado do cadastro manual e não foram conferidos;
- a auditoria do front apontou risco de corte em 375 px, falta de `aria-invalid` e `aria-describedby`, e um `step="5"` que o contrato não pede; a aba nunca foi vista no navegador contra a API real;
- o bot não faz duas coisas que o design prometia: sugerir as próximas datas abertas quando a data não tem horário livre, e sugerir o horário mais próximo quando o cliente digita um fora do passo;
- `fecha` não aceita 24:00, então funcionar até a meia-noite só é possível com 23:59;
- o caminho `listarReservas` de empresa por hora (painel e previsão do tempo) não tem teste próprio;
- a revisão de qualidade deixou limpezas pequenas.

## O que muda

### Entra
- Conferir a tabela de `CalendarioFeriados` com fonte oficial de cada estado e registrar a fonte e a data da conferência.
- Decidir o destino de `HorarioReserva` e `DiaReserva`: manter, ou remover com o código que só eles usam.
- Front: corrigir o corte em 375 px, ligar `aria-invalid` e `aria-describedby` nos campos de horário e de data, decidir o `step` do intervalo, e conferir a aba em 375, 768 e 1280 px contra a API real.
- Decidir se o painel `/cliente` abre direto na aba Funcionamento (`?aba=funcionamento`).
- Bot: sugerir as próximas datas abertas quando a data não tem horário livre, e o início mais próximo quando o horário está fora do passo.
- Aceitar fechamento à meia-noite (24:00) em horário de funcionamento e dia especial.
- Teste de `ServicoReserva.listarReservas` com empresa por hora.
- Limpeza: o fuso `America/Sao_Paulo` e o formato de data repetidos no código, `Log` com inicial maiúscula em `LocacaoStrategy`, `CalendarioFeriados.ehFeriado` sem uso, `ServicoReserva.pesquisarData` sem uso, e os dois comentários antigos de `Empresa.java`.

### Altera
- `openspec/specs/empresa/spec.md` (EMP-07 e EMP-08), `whatsapp/spec.md` (WPP-08) e `agenda/spec.md` (AGD-04), só no que a decisão mudar.

### Sai
- Nada do comportamento já entregue deixa de existir.

## Fora de escopo

- Comparação do token administrativo em tempo constante e recusa de chave vazia: pertence ao domínio de login (`AUTH`) e à change `remover-x-cliente-token`, já fechada; vai para uma change própria.
- Limite de taxa por número de WhatsApp.
- Paginação da leitura da agenda (hoje 500 eventos por período).
- Mais de uma instância da API (o bloqueio por dia continua em memória).

## Domínios

- spec: `openspec/specs/empresa/spec.md`
- spec: `openspec/specs/whatsapp/spec.md`
- spec: `openspec/specs/agenda/spec.md`
