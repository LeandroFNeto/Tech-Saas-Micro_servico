# reserva-por-hora

## Problema

Empresa com `locacaoPorHora` ligado não tem reserva automática. A change `corrigir-reserva-automatica` (arquivada) cobre só a diária e manda essas empresas para reserva manual em `SOLICITAR_RESERVA`.

Três pontos do código impedem reaproveitar o fluxo da diária:

- A conversa já pergunta o horário (`RESERVA_ESPERANDO_HORARIO`, `tratarReservaHorario`), mas `tratarReservaData` só pergunta se o **dia** está livre. `ServicoGoogleagenda.dataEstaLivre` e `datasOcupadas` devolvem datas inteiras: qualquer evento, de qualquer duração, ocupa o dia. Resultado: uma reserva por dia, o oposto do que a locação por hora promete.
- O horário informado só é validado contra o passado. Não há duração, então não existe "fim" para comparar com outros eventos.
- O evento criado hoje é de dia inteiro (`start` = data, `end` = data + 1).

## O que muda

### Entra

- Duração do atendimento definida pela empresa, e intervalo opcional entre atendimentos.
- Horário de funcionamento por perfil (7 dias da semana e `FERIADO`), com duração própria opcional por perfil.
- Feriados nacionais e estaduais calculados pelo sistema; municipais e folgas cadastrados pela empresa como dias especiais.
- Lista de horários livres mostrada ao cliente final.
- Reserva de várias datas num mesmo pedido (até 10, até 180 dias à frente), tudo ou nada.
- Aviso ao admin, na tela, quando fechar uma data que já tem reserva.

### Altera

- Checagem de disponibilidade: de "dia inteiro" para sobreposição de intervalo.
- Evento no Google: com `dateTime` e fuso `America/Sao_Paulo` para empresa por hora.
- `LocacaoStrategy`: empresa por hora com reserva automática e agenda deixa de cair em reserva manual.
- `ObservadorGoogleAgenda`: registra uma ou várias datas, com compensação.
- `Empresa`: campos `duracaoAtendimentoMinutos`, `intervaloEntreAtendimentosMinutos` e `uf`.

### Sai

- Nada é removido. A diária segue como está para empresa sem `locacaoPorHora`.

## Fora de escopo

- Cancelamento e remarcação pelo cliente.
- Duração escolhida pelo cliente final.
- Preço por hora e cobrança.
- Cancelamento automático de reservas quando a empresa fecha uma data.
- Feriados municipais automáticos ou por busca externa.
- Mais de uma instância da API (premissa de operação: uma instância).

## Respostas do dono do produto

1. Intervalo entre atendimentos: opcional, a empresa liga se fizer sentido para ela.
2. Várias datas: mesmo horário em todas; a variação fica nos perfis de funcionamento (fim de semana e feriado) e nos dias especiais.
3. Várias datas: tudo ou nada.
4. Limites de 10 datas por pedido e 180 dias à frente: aprovados.
5. Feriados nacionais, estaduais e municipais: necessários. Nacionais e estaduais valem sempre para toda empresa, com o perfil `FERIADO` igual ao domingo até a empresa mudar; municipais a empresa cadastra.
6. Fechar uma data com reservas: avisa só o admin, na tela. Avisar o cliente final entra com `lembrar-confirmacao-reserva`.

## Domínios

- spec: `openspec/specs/agenda/spec.md`
- spec: `openspec/specs/empresa/spec.md`
- spec: `openspec/specs/whatsapp/spec.md`

Vem depois de `corrigir-reserva-automatica` e pode reaproveitar `lembrar-confirmacao-reserva` para o lembrete, que passa a ter horário real.
