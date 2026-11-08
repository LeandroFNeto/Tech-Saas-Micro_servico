# Tasks

## 1. Guarda de cota no módulo de IA

- [x] 1.1 Criar tetos por minuto e por dia e pausa após 429 em `ServicoIA`, com as propriedades em `application.properties`, e verificar com teste em `ServicoIATest` que, no teto ou na pausa, o modelo não é chamado e o retorno é vazio
- [x] 1.1b Somar o custo estimado por dia a partir dos tokens da resposta e travar o modelo no teto `gemini.teto-gasto-diario-reais` (padrão 15,00), zerando na virada do dia em Brasília, e verificar em `ServicoIATest` que no teto o modelo não é chamado e que no dia seguinte volta a ser
- [x] 1.2 Criar o cache de resposta por prompt com validade e tamanho máximo e verificar com teste que a mesma pergunta não gera segunda chamada ao modelo
- [x] 1.3 Atualizar `IA-06` na documentação de propriedades e confirmar que `ServicoIATest` passa por inteiro

## 2. Conversa curta local

- [x] 2.1 Criar `ConversaCurta` com reconhecimento de cumprimento, "tudo bem", agradecimento e despedida e frases variadas, verificando com teste unitário os exemplos do IA-09 e a não repetição em mensagens seguidas
- [x] 2.2 Criar `ServicoIA.conversar` (conversa curta, cache/modelo, mensagem humana local com convite a `menu`) e verificar com teste que cumprimento não chama o modelo e que falha do modelo devolve a mensagem local

## 3. Uso nas sessões

- [x] 3.1 Usar `conversar` em `LocacaoStrategy` no lugar de "Opção inválida" e no início, só com IA ligada, e verificar em `LocacaoStrategyTest` o texto livre, a opção numérica sem conversa livre e a empresa sem IA inalterada
- [x] 3.2 Usar `conversar` em `AtendenteGambStrategy` para cumprimento inicial e texto livre no menu, mantendo limite diário e regras comerciais, e verificar em `AtendenteGambStrategyTest` e `webhook.feature` o cenário "Oi"
- [x] 3.3 Ajustar `atendimento-empresa.md` e `base-comercial.md` para conversa livre adaptada ao negócio e verificar com teste de prompt que as proibições (preço, reserva, outra empresa, valor, prazo) continuam no texto

## 4. Fechamento

- [x] 4.1 Rodar a suíte completa e `openspec validate liberdade-conversa-modulo-ia`, e atualizar `docs`/painel se listarem o módulo de IA

## Workflow follow-up

- Arquivar a change e sincronizar `ia` e `whatsapp` depois da revisão.
