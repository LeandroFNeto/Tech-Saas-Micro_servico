# Tasks

## 1. Contrato

- [x] 1.1 Atualizar `especificacao/back/paths/webhook-whatsapp.yaml` com as duas sessões da Gamb, o fluxo de reunião, o limite diário e o comportamento do número não autorizado; conferir que o YAML valida.
- [x] 1.2 Atualizar o contrato de edição da empresa interna com `emailsResponsaveisReuniao` (DTO nomeado) e `$ref` em `especificacao/back/swagger.yaml`; conferir que o YAML valida.
- [x] 1.3 Atualizar `openspec/specs/whatsapp/spec.md` e `assistente-tutoria/spec.md` ao arquivar; conferir que cada requisito novo tem `Contrato:` e `Prova:`.

## 2. Atendente da Gamb no WhatsApp

- [x] 2.1 Criar `AtendenteGambStrategy` com o menu de dúvida e reunião, chamando o Suporte do `AssistenteTutoriaService`; conferir com `AtendenteGambStrategyTest` que a resposta vem da base e que pedido de dado de empresa é recusado.
- [x] 2.2 Registrar o ramo `ATENDENTE_GAMB` em `FactoryModulo`; conferir com o cenário do `webhook.feature` e que as strategies das empresas não mudaram.
- [x] 2.3 Implementar o limite diário de mensagens por número; conferir com teste que a mensagem acima do limite recebe a resposta fixa sem chamar a IA.

## 3. Reunião com a equipe

- [x] 3.1 Adicionar `emailsResponsaveisReuniao` à empresa interna, com validação de e-mail, e a edição no painel admin; conferir com teste de controller e `*.spec.ts` que a lista salva, que e-mail inválido é rejeitado e que o front mostra o campo.
- [x] 3.2 Implementar o fluxo motivo, data, horário e confirmação, criando o evento pelo `ServicoReserva` com os e-mails como convidados; conferir com `AtendenteGambStrategyTest` os casos horário livre, horário ocupado, lista vazia e agenda desconectada.
- [x] 3.3 Ao salvar a lista, atualizar só os convidados das reuniões futuras já marcadas, sem cancelar nem remarcar; conferir com teste que o evento continua igual e que o e-mail não aparece em log.

## 4. Bot de teste de módulos

- [x] 4.1 Criar a interface `ModuloExperimental` e o registro dos módulos; conferir com teste que `/modulos` lista os registrados.
- [x] 4.2 Criar `TesteModulosStrategy` com `/modulo <nome> <texto>` e a lista de números autorizados; conferir com `TesteModulosStrategyTest` os casos autorizado, não autorizado e lista vazia.
- [x] 4.3 Reaproveitar a lógica do `TESTE_VIRTUAL` como primeiro módulo experimental; conferir que `ClienteAssistenteChatTest` continua passando.
- [x] 4.4 Passar o número e o texto pelo `MascaraLog` nos logs das duas strategies; conferir com teste de log.

## 5. Operação e fechamento

- [ ] 5.1 Cadastrar as duas empresas internas da Gamb, as sessões no WPPConnect, a Google Agenda da Gamb, os e-mails dos responsáveis e a lista de autorizados; conferir uma mensagem real em cada número e uma reunião de teste com convite recebido.
- [x] 5.2 Rodar os testes do backend e do front e conferir que passam; atualizar `openspec/specs/README.md` com as provas novas.
Pendência operacional: 5.1 fica com a pessoa antes de usar em produção
