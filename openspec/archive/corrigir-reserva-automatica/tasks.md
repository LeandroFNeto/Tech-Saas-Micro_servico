# Tarefas

## Programação

- [x] Contrato YAML: 400 da regra da agenda (admin) e 400 do PUT do cliente em `paths/empresas-admin.yaml` e `paths/empresas-cliente.yaml`
- [x] Java: `EstadoUsuario.RESERVA_REGISTRANDO` e `GerenciadorSessao.concluirRegistro`
- [x] Java: `EventoReservaConfirmada` com horário, número original, nome e `idPedido`; `AgendaNaoConfiguradaException`
- [x] Java: `ServicoReserva.agendarNoGoogle` cria o evento de dia inteiro, com id derivado do `idPedido`, e devolve o id
- [x] Java: `LocacaoStrategy` com estado `RESERVA_REGISTRANDO`, segundo "sim" ignorado, menu bloqueado no registro, reserva por hora e empresa sem agenda como reserva manual, resposta "anotei"
- [x] Java: `ObservadorGoogleAgenda` com mapa de bloqueios por empresa e data, reconsulta sob bloqueio, busca do próprio evento nas tentativas seguintes, 409 como sucesso, 3 tentativas só em erro transitório (inclui 403 de limite de uso), mensagens de falha e `concluirRegistro` em `try/finally`
- [x] Java: `ControllerEmpresa` com POST e PUT do admin exigindo agenda para ligar, e PUT do cliente rejeitando mudança com 400
- [x] Front, canvas: board `ClienteChatbot` com a opção só para leitura e o aviso, antes do Angular
- [x] Front: opção só para leitura no perfil do cliente, com aviso de que o admin libera; 400 da agenda na tela do admin
- [x] Prova: ajustar `ControllerEmpresaTest` (hoje o PUT do cliente espera `true`) e criar os casos de `LocacaoStrategyTest`, `GerenciadorSessaoTest`, `observer-agenda.feature` e `empresas.feature`, conforme `design.md`, seção Arquivos

## Fechamento (não é da programação)

- [x] Revisão: gravar AGD-02, WPP-07 (WPP-06 já existia) e a regra de EMP-02 nos `spec.md`, ajustar AGD-01 e atualizar `openspec/specs/README.md`
- [x] Deploy: mover esta pasta para `openspec/archive/corrigir-reserva-automatica/`
