# Design

## Decisão

1. **Duração por empresa.** `Empresa.duracaoAtendimentoMinutos` (múltiplo de 30, de 30 a 720, padrão 60), lido só com `locacaoPorHora` ligado. O cliente final escolhe só o início. A duração também é o passo da lista de horários livres.
2. **Intervalo opcional.** `Empresa.intervaloEntreAtendimentosMinutos` (0 a 120, padrão 0). A checagem de conflito soma o intervalo antes e depois do atendimento pedido; o evento no Google guarda só o atendimento.
3. **Funcionamento por perfil.** Tabela `horario_funcionamento` (empresa, perfil, abre, fecha, fechado, duração própria opcional). Perfis: `SEGUNDA` a `DOMINGO` e `FERIADO`. Duração vazia herda a da empresa. Sem configuração: todos os dias das 08:00 às 22:00 e `FERIADO` igual a `DOMINGO`. Reserva que começa antes de abrir ou termina depois de fechar é recusada.
4. **Qual perfil vale numa data.** `ServicoFuncionamento.janelaDa(empresa, data)` decide, nesta ordem:
   - dia especial da empresa (`data_especial`: fechado ou horário próprio, motivo opcional);
   - feriado nacional ou estadual da `uf` da empresa → perfil `FERIADO`;
   - dia da semana.
5. **Feriados.** `CalendarioFeriados`, classe pura sem rede:
   - nacionais: fixos mais Carnaval (terça), Sexta-feira Santa e Corpus Christi, a partir da Páscoa;
   - estaduais: tabela fixa por UF no código;
   - empresa sem `uf` só segue os nacionais;
   - feriado municipal é dia especial lançado pela empresa;
   - `data_especial` só aceita datas de hoje a 180 dias à frente.
6. **Disponibilidade por sobreposição.** `ServicoGoogleagenda.horarioEstaLivre(calendarId, inicio, fim)` e `horariosLivres(calendarId, empresa, data)`: intervalo `[início, fim)` conflita com qualquer evento que o cruze; terminar exatamente quando o outro começa não conflita; evento de dia inteiro conflita com tudo no dia. `dataEstaLivre` e `datasOcupadas` continuam para a diária.
7. **Evento com hora.** `agendarNoGoogle` recebe início e fim e grava `dateTime` com `timeZone` `America/Sao_Paulo`. O id continua derivado do `idPedido` (com a posição da data quando houver várias); 409 conta como sucesso.
8. **Horários livres na conversa.** Ao receber uma data aberta, o bot lista os inícios livres (ex.: "14:00, 15:00, 18:00"). Sem nenhum, ou data fechada, responde com o motivo e as próximas datas abertas. O cliente pode digitar um horário; a reconsulta no observador é a palavra final. Horário fora do passo é recusado com sugestão do mais próximo.
9. **Várias datas.** Novo estado `RESERVA_MAIS_DATAS`: depois de cada data aceita o bot pergunta "Quer reservar mais alguma data?".
   - Limites: 10 datas por pedido, até 180 dias à frente; datas repetidas são unidas; o cliente pode tirar uma data antes do "sim".
   - Mesmo horário e mesmo nome em todas. Se o horário não couber na janela de uma data, só ela cai, com o motivo.
   - `DadosReservaSessao` guarda a lista de datas; `EventoReservaConfirmada` leva a lista, o horário, a duração e o `idPedido`.
10. **Tudo ou nada no observador.** Sob o bloqueio por dia, em ordem crescente de data (evita travar dois pedidos que dividem datas), o observador reconsulta todas as datas. Se alguma estiver ocupada ou fechada, nada é criado e o bot diz quais caíram e volta a pedir só essas. Se a inserção falhar no meio, apaga os eventos já criados (compensação); se apagar falhar, `log.error` com id da empresa, `idPedido` e classe da exceção, sem dado pessoal. As tentativas, o `concluirRegistro` e o `try/finally` seguem como na diária. O bloqueio continua por dia (`empresaId + data`), não por hora.
11. **Mensagens.** Sucesso, falha e "data ocupada" trazem as datas e o horário ("Esse horário acabou de ser reservado").
12. **Regras do restante.** `permiteReservaAutomatica` continua opcional e só do admin; agenda obrigatória para ligar; sem agenda cai em reserva manual (decisões 8 a 10 da change anterior). `tratarSolicitacaoReserva` deixa de mandar `locacaoPorHora` para reserva manual.
13. **Validação na empresa.** `PUT` da empresa responde 400 se abre >= fecha, duração fora da faixa, intervalo fora da faixa ou `uf` inválida. Mudar a duração não mexe em reservas já criadas.
14. **Fechar data com reservas.** Cadastrar dia especial fechado em data com evento na agenda não cancela nada; a resposta devolve o aviso "Há reservas nesta data" com a lista dos eventos, e a tela do cliente o mostra.
15. **Empresa existente.** Quem já tem `locacaoPorHora` ligado recebe os padrões sem migração manual (campos nulos valem os padrões) e continua em reserva manual até o admin ligar `permiteReservaAutomatica`.
16. **Tela.** Em `/cliente/chatbot`, bloco "Funcionamento": começa com um único horário e a duração; "Personalizar" abre os perfis, os dias especiais, o intervalo e a `uf`. Canvas de design antes do Angular.

## Arquivos

- Contrato:
  - `especificacao/back/paths/empresas-funcionamento.yaml`, `empresas-dias-especiais.yaml` e `empresas-dias-especiais-id.yaml` (rotas próprias em `/empresas/{sessao}/funcionamento` e `/dias-especiais`; o perfil da empresa e o `EmpresaResponseDTO` não mudam)
  - `especificacao/back/schemas/` (DTOs `FuncionamentoDTO`, `HorarioFuncionamentoDTO`, `DataEspecialDTO` com o aviso de reservas, campos novos em `EmpresaUpdateDTO` e `EmpresaResponseDTO`) e `$ref` em `swagger.yaml`
- Java:
  - `model/Empresa.java`, `model/HorarioFuncionamento.java`, `model/DataEspecial.java`, repositórios
  - `servico/CalendarioFeriados.java` e `servico/ServicoFuncionamento.java` (novos)
  - `servico/ServicoGoogleagenda.java` (`horarioEstaLivre`, `horariosLivres`), `servico/ServicoReserva.java` (`agendarNoGoogle` com hora e remoção para compensação)
  - `model/EstadoUsuario.java` (`RESERVA_MAIS_DATAS`), `model/DadosReservaSessao.java`, `evento/EventoReservaConfirmada.java`
  - `strategy/LocacaoStrategy.java` (lista de horários, várias datas, empresa por hora liberada)
  - `servico/observador/ObservadorGoogleAgenda.java` (várias datas, ordem de bloqueio, compensação)
  - `controller/ControllerEmpresa.java` (validações e rotas de funcionamento e dias especiais)
- Front:
  - Antes do Angular: canvas de design, board `ClienteChatbot` com o bloco "Funcionamento"
  - `*.content.json` e `telas.design.json` da tela; componente em `features/client/` do chatbot (funcionamento, dias especiais, aviso de reservas na data fechada)
- Prova:
  - `CalendarioFeriadosTest`: fixos, móveis de 2026 a 2030, estaduais por UF
  - `ServicoFuncionamentoTest`: ordem dia especial, feriado, semana; padrões
  - `ServicoGoogleagendaTest`: sobreposição, borda sem conflito, dia inteiro, intervalo
  - `LocacaoStrategyTest`: lista de horários, data fechada, mais datas, limites, horário fora do passo
  - `src/test/resources/features/observer-agenda.feature`: evento com hora; conflito de sobreposição; várias datas criadas; uma ocupada e nada criado; falha no meio e compensação; compensação que falha registra log; 409 conta como sucesso
  - `src/test/resources/features/empresas.feature`: validações 400, dia especial com aviso de reservas, padrões para empresa existente
  - testes Angular do bloco "Funcionamento"

## Alinhamento

- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`
- O webhook HTTP não espera a agenda (AGD-01)
- Uma instância da API: bloqueio em memória aceito

## Riscos

- **Evento manual do dono** de dia inteiro continua bloqueando o dia todo; evento com hora ocupa só o trecho dele.
- **Várias inserções por pedido** (até 10) podem estourar a cota do Google ou demorar; o observador roda fora do webhook e o cliente já recebeu "Estou registrando".
- **Compensação não é atômica**: o Google não tem transação. Evento criado e não apagado fica na agenda do dono; por isso o log com `idPedido`.
- **Configuração demais para o dono**: tudo tem padrão e a tela começa com um horário só; perfis, `uf`, intervalo e dias especiais ficam em "Personalizar".
- **Tabela estadual desatualizada**: revisada a cada virada de ano, datas nos testes, e a empresa pode sobrescrever com dia especial.
- **Limite de 250 eventos** na consulta pode esconder conflito em agenda muito cheia; para um dia o risco é baixo.
- **Bloqueio em memória por dia**: com mais de uma instância, o bloqueio vai para o banco.
- **Horário de verão**: toda conversão passa por `America/Sao_Paulo`, nunca por deslocamento fixo.
- **`HorarioReserva` e `DiaReserva`** (tabelas `horarios_reserva` e `dias_reserva`, texto livre em `descricao` e `status`) parecem legado do cadastro manual e não entram no cálculo; conferir se alguma tela ainda os usa antes de implementar.
- **Sessão por número, não por empresa** (risco já conhecido da change anterior): vale também para as várias datas.
