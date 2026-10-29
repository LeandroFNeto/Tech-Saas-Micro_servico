# Tarefas

## Antes da programação (a pessoa faz, fora do código)

- Conferir se `HorarioReserva` e `DiaReserva` ainda são usados por alguma tela; se não, ficam fora do cálculo e sem mudança.
- Conferir a tabela de feriados estaduais com uma fonte oficial de cada estado antes de gravá-la.

## Programação

- [x] Contrato YAML: campos novos no perfil da empresa (`duracaoAtendimentoMinutos`, `intervaloEntreAtendimentosMinutos`, `uf`), `FuncionamentoDTO`, `HorarioFuncionamentoDTO`, `DataEspecialDTO` com o aviso de reservas, rotas de funcionamento e dias especiais, 400 das validações, e `$ref` em `especificacao/back/swagger.yaml`
- [x] Java: campos novos em `Empresa`, entidades `HorarioFuncionamento` e `DataEspecial`, repositórios, padrões para empresa existente
- [x] Java: `CalendarioFeriados` (fixos, móveis pela Páscoa, estaduais por UF) e `ServicoFuncionamento.janelaDa`
- [x] Java: `ServicoGoogleagenda.horarioEstaLivre` e `horariosLivres` (sobreposição, intervalo, dia inteiro)
- [x] Java: `ServicoReserva.agendarNoGoogle` com hora e fuso, e remoção de evento para a compensação
- [x] Java: `EstadoUsuario.RESERVA_MAIS_DATAS`, `DadosReservaSessao` com lista de datas, `EventoReservaConfirmada` com datas, horário e duração
- [x] Java: `LocacaoStrategy` (lista de horários livres, data fechada com próximas datas, mais datas, limites, horário fora do passo, empresa por hora liberada para reserva automática)
- [x] Java: `ObservadorGoogleAgenda` com várias datas (bloqueio em ordem de data, tudo ou nada, compensação, log sem dado pessoal), sem alterar tentativas nem `concluirRegistro`
- [x] Java: `ControllerEmpresa` com validações no `PUT`, rotas de funcionamento e dias especiais, e o aviso de reservas ao fechar uma data
- [x] Front, canvas: board `ClienteChatbot` com o bloco "Funcionamento" e "Personalizar", antes do Angular
- [x] Front (visual em diasabatico e ligado à API): `*.content.json` e `telas.design.json`; componente do bloco "Funcionamento" em `features/client/` (horário único, perfis, `uf`, intervalo, dias especiais, aviso de reservas)
- [x] Prova (Java): `CalendarioFeriadosTest`, `ServicoFuncionamentoTest`, `ServicoGoogleagendaSobreposicaoTest`, `LocacaoStrategyTest`, `ObservadorGoogleAgendaPorHoraTest`, `ControllerFuncionamentoTest` e três cenários por hora em `observer-agenda.feature`
- [x] Prova: 16 cenários de funcionamento em `empresas.feature` e testes Angular do bloco "Funcionamento"

## Fechamento (não é da programação)

- [x] Revisão: gravar os requisitos novos em `openspec/specs/agenda/spec.md` (AGD, próximo número livre), `empresa/spec.md` (EMP) e `whatsapp/spec.md` (WPP), e as linhas em `openspec/specs/README.md`
- [x] Deploy: mover esta pasta para `openspec/archive/reserva-por-hora/`
