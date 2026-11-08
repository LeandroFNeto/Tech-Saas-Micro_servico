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

## 5. Revisão

- [x] 5.1 Tratar "menu" e "voltar" como *0* na Gamb e repetir o menu para "sim", "não", "ok", "sair" e "cancelar", verificando em `AtendenteGambStrategyTest` que o modelo não é chamado
- [x] 5.2 Mandar ao Suporte a pergunta de uso do app escrita no menu da Gamb, mantendo o restante do texto livre na base comercial, e verificar que o cliente fica no menu
- [x] 5.3 Expor a situação da guarda de cota e mostrar a IA como degradada no diagnóstico, verificando em `DiagnosticoModulosServiceTest` e `GuardaCotaIATest`
- [x] 5.4 Somar o custo estimado fixo no timeout, com padrão de R$ 0,005 configurável por variável, e verificar em `ServicoIAConversaTest`
- [x] 5.5 Dar validade de 6 horas à lembrança da `ConversaCurta`, verificar em `ConversaCurtaTest`, e cobrir o aviso de teto no log, o início da Locação sem modelo, o "obrigado" na dúvida da Gamb e o cumprimento no cenário "Oi" do `webhook.feature`
- [x] 5.6 Listar as propriedades `gemini.*` no design (tarefa 1.3) e ajustar IA-06, IA-09, WPP-12 e o diagnóstico em `assistente-tutoria`

## Workflow follow-up

- Arquivar a change e sincronizar `ia` e `whatsapp` depois da revisão.
