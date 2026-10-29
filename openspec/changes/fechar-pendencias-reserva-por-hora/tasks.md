# Tarefas

## Antes da programação (a pessoa faz, fora do código)

- [ ] Conferir a tabela de feriados estaduais de `CalendarioFeriados` com a fonte oficial de cada estado e entregar as correções.
- [ ] Conferir se `HorarioReserva` e `DiaReserva` ainda são usados por alguma tela, rota ou consulta; decidir manter ou remover.
- [ ] Decidir: o painel `/cliente` abre direto na aba Funcionamento? O `step="5"` do intervalo sai? O fechamento 24:00 entra?
- [ ] Abrir a aba Funcionamento em 375, 768 e 1280 px contra a API rodando e anotar o que quebra.

## Programação

- [ ] Contrato YAML, só se o 24:00 for aprovado, e `$ref` em `especificacao/back/swagger.yaml` se algo novo entrar
- [ ] Java: correções da tabela de feriados estaduais
- [ ] Java: remoção de `HorarioReserva` e `DiaReserva`, se a conferência mostrar que não têm uso
- [ ] Java: `LocacaoStrategy` sugere as próximas 3 datas abertas quando a data não tem horário livre e o início mais próximo quando o horário está fora do passo
- [ ] Java: fechamento 24:00 em `ControllerFuncionamento` e `ServicoFuncionamento`
- [ ] Java: limpeza (fuso e formato de data únicos, `Log`, `ehFeriado`, `pesquisarData`, comentários antigos de `Empresa.java`)
- [ ] Front: 375 px, `aria-invalid`, `aria-describedby` e `step` do intervalo em `funcionamento`; aba por query, se aprovada
- [ ] Prova: `CalendarioFeriadosTest`, teste de `ServicoReserva.listarReservas` por hora, `LocacaoStrategyTest`, cenários de 24:00 em `empresas.feature` e specs Angular

## Fechamento (não é da programação)

- [ ] Revisão: atualizar `empresa/spec.md` (EMP-07 e EMP-08), `whatsapp/spec.md` (WPP-08) e `agenda/spec.md` (AGD-04) no que a decisão mudar, com a fonte e a data da conferência dos feriados
- [ ] Deploy: mover esta pasta para `openspec/archive/fechar-pendencias-reserva-por-hora/`
