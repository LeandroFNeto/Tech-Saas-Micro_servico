# Design

## Decisão

1. **Feriados estaduais.** Mantém a tabela fixa por UF no código. A conferência é feita por uma pessoa com a fonte oficial de cada estado; o resultado é uma lista de correções em `CalendarioFeriados` e um registro da fonte e da data no próprio `EMP-07`. O teste de datas por UF (`CalendarioFeriadosTest`) é ajustado junto.
2. **`HorarioReserva` e `DiaReserva`.** Como a conferência atestou que nenhuma tela, rota ou consulta os utiliza, a change remove essas entidades do modelo Java e do mapeamento JPA (a tabela física no banco não é apagada automaticamente para preservar histórico).
3. **Front.** Sem rota nova. Em 375 px a linha do dia passa a empilhar o campo "Usar a geral" em vez de cortá-lo. Os campos "Abre às" e o horário do novo dia ganham `aria-invalid`, e os erros de data e de horário ficam ligados ao campo por `aria-describedby`. O `step` do intervalo sai de fixo em 5 para permitir qualquer inteiro de 0 a 120 (que é o que o contrato aceita).
4. **Aba por query.** Se aprovado, o atalho do painel `/cliente` leva a `/cliente/chatbot?aba=funcionamento` e o perfil lê o parâmetro só para escolher a aba inicial, ignorando valor desconhecido.
5. **Sugestões contextuais do bot.** Quando uma data solicitada estiver fechada ou sem horários livres:
   - Se for sexta-feira ou sábado: sugere prioritariamente o dia seguinte do fim de semana (ex.: sexta lotada -> sugere sábado; sábado lotado -> sugere domingo).
   - Se for domingo: sugere o próximo domingo (ou o domingo anterior, se a consulta for para uma data distante no futuro).
   - Se o cliente digitar um horário fora do passo/intervalo: responde com o início válido mais próximo antes da listagem completa.
6. **Meia-noite.** O fechamento 24:00 é aceito e guardado como fechamento às 00:00 do dia seguinte, com `Janela.comporta` tratando 00:00 como fim do dia; o `HH:mm` do contrato continua, aceitando "24:00". Abertura não aceita 24:00.
7. **Teste de `listarReservas`.** Cenário de empresa por hora com dia livre, lotado e fechado, usando o mesmo mock de agenda dos outros testes de `ServicoReserva`.
8. **Limpeza.** Constantes únicas para o fuso e o formato de data nos serviços do domínio de agenda, renome de `Log` para `log`, remoção do código sem uso (`CalendarioFeriados.ehFeriado`, `ServicoReserva.pesquisarData`) e comentários antigos de `Empresa.java`. Sem mudança de comportamento.

## Arquivos

- Contrato: `especificacao/back/paths/empresas-funcionamento.yaml` e `empresas-dias-especiais.yaml` (aceitar "24:00").
- Java: `servico/CalendarioFeriados.java`, `servico/ServicoFuncionamento.java`, `controller/ControllerFuncionamento.java`, `strategy/LocacaoStrategy.java`, `servico/ServicoReserva.java`, `model/Empresa.java`, `model/HorarioReserva.java` e `model/DiaReserva.java` (remoção).
- Front: `features/client/funcionamento/`, `features/client/perfil/` e `features/client/painel/`; `especificacao/front/cliente-perfil.content.json` e `telas.design.json`.
- Prova:
  - `CalendarioFeriadosTest`, `ServicoFuncionamentoTest`, `LocacaoStrategyTest`
  - Novo teste de `ServicoReserva.listarReservas` por hora
  - `src/test/resources/features/empresas.feature` (24:00 e janela menor que a duração)
  - `funcionamento.component.spec.ts` (acessibilidade) e `perfil.component.spec.ts` (aba por query)

## Alinhamento

- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`
- Uma instância da API; bloqueio por dia em memória aceito

## Riscos

- **Fonte oficial:** feriado estadual muda por lei; a tabela precisa de revisão a cada virada de ano, e a empresa sempre pode sobrescrever com dia especial.
- **Remover legado:** apagar entidade com dado em produção perde o histórico; por isso a tabela física não é apagada pela change.
- **24:00:** mexe na regra de janela; precisa de teste de borda (início às 23:00 com duração de 60 minutos).
- **Cobertura no navegador:** a conferência de 375, 768 e 1280 px é manual; sem ela o risco de corte continua.
