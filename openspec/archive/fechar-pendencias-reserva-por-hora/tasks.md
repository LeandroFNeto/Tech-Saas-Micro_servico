# Tarefas

## Antes da programação (a pessoa faz, fora do código)

- [x] Conferir a tabela de feriados estaduais de `CalendarioFeriados` com a fonte oficial de cada estado e entregar as correções.
- [x] Conferir se `HorarioReserva` e `DiaReserva` ainda são usados por alguma tela, rota ou consulta; decidido: remover entidades Java sem apagar tabela física.
- [x] Decidir: o painel `/cliente` abre direto na aba Funcionamento? Aprovado via query param `?aba=funcionamento`.
- [x] Abrir a aba Funcionamento em 375, 768 e 1280 px contra a API rodando e anotar o que quebra.

## Programação

- [x] Contrato YAML: aceitar "24:00" em `especificacao/back/paths/empresas-funcionamento.yaml` e `empresas-dias-especiais.yaml`
- [x] Java: remoção de `HorarioReserva` e `DiaReserva` (e referências em `Empresa.java`)
- [x] Java: correções da tabela de feriados estaduais
- [x] Java: `LocacaoStrategy` sugere datas contextuais (sexta/sábado sugere próximo dia do fim de semana; domingo sugere próximo/anterior domingo) e início mais próximo quando horário fora do passo
- [x] Java: fechamento 24:00 em `ControllerFuncionamento`, `ServicoFuncionamento` e cálculo de `Janela`
- [x] Java: limpeza (fuso e formato de data únicos, `Log` -> `log`, `ehFeriado`, `pesquisarData`, comentários antigos de `Empresa.java`)
- [x] Front: 375 px (empilhamento de "Usar a geral"), `aria-invalid`, `aria-describedby` e `step` livre (0 a 120 min) em `funcionamento`; aba por query no `perfil`
- [x] Prova: `CalendarioFeriadosTest`, teste de `ServicoReserva.listarReservas` por hora, `LocacaoStrategyTest`, cenários de 24:00 em `ControllerFuncionamentoTest` e specs Angular

## Fechamento (não é da programação)

- [x] Revisão: atualizar `empresa/spec.md` (EMP-07 e EMP-08), `whatsapp/spec.md` (WPP-08) e `agenda/spec.md` (AGD-04) no que a decisão mudar, com a fonte e a data da conferência dos feriados
- [x] Deploy: mover esta pasta para `openspec/archive/fechar-pendencias-reserva-por-hora/`
