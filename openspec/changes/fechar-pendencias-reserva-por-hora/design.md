# Design

## Decisão

1. **Feriados estaduais.** Mantém a tabela fixa por UF no código. A conferência é feita por uma pessoa com a fonte oficial de cada estado; o resultado é uma lista de correções em `CalendarioFeriados` e um registro da fonte e da data no próprio `EMP-07`. O teste de datas por UF (`CalendarioFeriadosTest`) é ajustado junto.
2. **`HorarioReserva` e `DiaReserva`.** Primeiro se confere se alguma tela, rota ou consulta ainda os usa. Sem uso: a change remove as entidades, os repositórios e o que só eles usam (a tabela no banco não é apagada automaticamente; apagar fica como passo manual do admin). Com uso: ficam e a decisão é registrada no spec.
3. **Front.** Sem rota nova. Em 375 px a linha do dia passa a empilhar o campo "Usar a geral" em vez de cortá-lo. Os campos "Abre às" e o horário do novo dia ganham `aria-invalid`, e os erros de data e de horário ficam ligados ao campo por `aria-describedby`. O `step` do intervalo sai se a pessoa preferir qualquer inteiro de 0 a 120, que é o que o contrato aceita. Tudo antes passa pelo canvas de design se o visual mudar; ajuste só de acessibilidade e de empilhamento não precisa.
4. **Aba por query.** Se aprovado, o atalho do painel `/cliente` leva a `/cliente/chatbot?aba=funcionamento` e o perfil lê o parâmetro só para escolher a aba inicial, ignorando valor desconhecido.
5. **Sugestões do bot.** Data sem horário livre responde com o aviso de hoje mais as próximas 3 datas abertas, reaproveitando `ServicoFuncionamento.proximasDatasAbertas`. Horário fora do passo responde com o início válido mais próximo, antes da lista completa. Sem nova consulta ao Google além das que já existem.
6. **Meia-noite.** O fechamento 24:00 é aceito e guardado como fechamento às 00:00 do dia seguinte, com `Janela.comporta` tratando 00:00 como fim do dia; o `HH:mm` do contrato continua, aceitando "24:00". Abertura não aceita 24:00.
7. **Teste de `listarReservas`.** Cenário de empresa por hora com dia livre, lotado e fechado, usando o mesmo mock de agenda dos outros testes de `ServicoReserva`.
8. **Limpeza.** Constantes únicas para o fuso e o formato de data nos serviços do domínio de agenda, renome de `Log` para `log`, remoção do código sem uso e dos comentários antigos. Sem mudança de comportamento.

## Arquivos

- Contrato: `especificacao/back/paths/empresas-funcionamento.yaml` e `empresas-dias-especiais.yaml` (aceitar "24:00"), só se a decisão 6 for aprovada.
- Java: `servico/CalendarioFeriados.java`, `servico/ServicoFuncionamento.java`, `controller/ControllerFuncionamento.java`, `strategy/LocacaoStrategy.java`, `servico/ServicoReserva.java`, `model/Empresa.java`, `model/HorarioReserva.java` e `model/DiaReserva.java` (se saírem).
- Front: `features/client/funcionamento/`, `features/client/perfil/` e `features/client/painel/` (se a aba por query for aprovada); `especificacao/front/cliente-perfil.content.json` e `telas.design.json`.
- Prova:
  - `CalendarioFeriadosTest`, `ServicoFuncionamentoTest`, `LocacaoStrategyTest`
  - novo teste de `ServicoReserva.listarReservas` por hora
  - `src/test/resources/features/empresas.feature` (24:00 e janela menor que a duração)
  - `funcionamento.component.spec.ts` (acessibilidade) e `perfil.component.spec.ts` (aba por query)

## Alinhamento

- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`
- Uma instância da API; bloqueio por dia em memória aceito

## Riscos

- **Fonte oficial:** feriado estadual muda por lei; a tabela precisa de revisão a cada virada de ano, e a empresa sempre pode sobrescrever com dia especial.
- **Remover legado:** apagar entidade com dado em produção perde o histórico; por isso a tabela não é apagada pela change.
- **24:00:** mexe na regra de janela; precisa de teste de borda (início às 23:00 com duração de 60 minutos).
- **Cobertura no navegador:** a conferência de 375, 768 e 1280 px é manual; sem ela o risco de corte continua.
