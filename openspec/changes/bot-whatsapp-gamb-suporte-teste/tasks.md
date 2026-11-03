# Tasks

## 1. Contrato

- [ ] 1.1 Atualizar `especificacao/back/paths/webhook-whatsapp.yaml` com as duas sessões da Gamb e o comportamento do número não autorizado; conferir que o YAML valida.
- [ ] 1.2 Atualizar `openspec/specs/whatsapp/spec.md` e `assistente-tutoria/spec.md` ao arquivar; conferir que cada requisito novo tem `Contrato:` e `Prova:`.

## 2. Bot de Suporte no WhatsApp

- [ ] 2.1 Criar `SuporteGambStrategy` chamando o Suporte do `AssistenteTutoriaService`; conferir com `SuporteGambStrategyTest` que a resposta vem da base e que pedido de dado de empresa é recusado.
- [ ] 2.2 Registrar o ramo `SUPORTE_GAMB` em `FactoryModulo`; conferir com o cenário do `webhook.feature` e que as strategies das empresas não mudaram.

## 3. Bot de teste de módulos

- [ ] 3.1 Criar a interface `ModuloExperimental` e o registro dos módulos; conferir com teste que `/modulos` lista os registrados.
- [ ] 3.2 Criar `TesteModulosStrategy` com `/modulo <nome> <texto>` e a lista de números autorizados; conferir com `TesteModulosStrategyTest` os casos autorizado, não autorizado e lista vazia.
- [ ] 3.3 Reaproveitar a lógica do `TESTE_VIRTUAL` como primeiro módulo experimental; conferir que `ClienteAssistenteChatTest` continua passando.
- [ ] 3.4 Passar o número e o texto pelo `MascaraLog` nos logs das duas strategies; conferir com teste de log.

## 4. Operação e fechamento

- [ ] 4.1 Cadastrar as duas empresas internas da Gamb e as sessões no WPPConnect e registrar a lista de autorizados; conferir uma mensagem real em cada número.
- [ ] 4.2 Rodar os testes do backend e conferir que passam; atualizar `openspec/specs/README.md` com as provas novas.
