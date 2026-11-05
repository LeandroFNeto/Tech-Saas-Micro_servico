# Tarefas

## Back
- [x] `EstadoUsuario`: novo estado `REUNIAO_CONFIRMACAO_DATA`
- [x] `AtendenteGambStrategy`: ler a data com `InterpretadorDeData` em `REUNIAO_DATA`, separar a validação (passada, limite, dia fechado, horários livres) para receber a `LocalDate`
- [x] `AtendenteGambStrategy`: data duvidosa guarda a data pendente, vai a `REUNIAO_CONFIRMACAO_DATA` e pergunta; tratar *sim*, *não*, outra data e texto sem data
- [x] `AtendenteGambStrategy`: `MENSAGEM_PEDIR_DATA` e mensagens de erro sem exigir o formato; respostas seguem em `DD-MM-AAAA`
- [x] `AssistenteTutoriaService`: bloco de tom humano em `construirPromptSuporte` e `construirPromptComercial`, sem mexer em escopo, recusa e filtro de valor

## Back: conhecer a Gamb
- [x] `EstadoUsuario`: novo estado `ATENDENTE_SOBRE`
- [x] `AssistenteTutoriaService`: `responderComercialWhatsapp`, canal (SITE ou WHATSAPP) em `construirPromptComercial`, `gerarFallbackComercial`, `textoContato` e na recusa comercial; pedido de ligar o problema do cliente ao que existe hoje
- [x] `AtendenteGambStrategy`: opção `3` no `MENSAGEM_MENU`, estado `ATENDENTE_SOBRE`, desvio por intenção comercial no menu e na dúvida, fechamento convidando a digitar *2*

## Provas
- [x] `AtendenteGambStrategyTest`: `25 de dezembro`, `25.12`, `amanhã` e frase com data avançam para os horários; `sábado` pergunta e o *sim* avança; correção troca a data; *não* volta a pedir; texto sem data repete o pedido; data passada e fora do limite continuam recusadas
- [x] `AssistenteTutoriaServiceTest`: os dois prompts trazem a orientação de tom e mantêm o escopo (sem dado de empresa, recusa padrão, sem valor)
- [x] `AtendenteGambStrategyTest`: *3* e "o que vocês fazem?" respondem pela base comercial e convidam a digitar *2*; pergunta do app na dúvida continua no Suporte; `1` e `2` seguem iguais
- [x] `AssistenteTutoriaServiceTest`: prompt e fallback do canal WhatsApp não mandam para o WhatsApp da Gamb, trazem o pedido de ligar o problema ao que existe hoje e mantêm os limites (sem valor, prazo, cliente, concorrente); canal SITE não muda
- [x] Rodar `mvn test` (660 testes passam)
- [ ] Conferir uma conversa real no número da Gamb: marcar reunião escrevendo a data de vários jeitos, perguntar "o que vocês fazem?", descrever um problema e fazer uma pergunta de dúvida em tom informal

## Fechamento
- [ ] Sincronizar `openspec/specs/whatsapp/spec.md` (WPP-12 e WPP-13) com o delta, à mão
- [ ] Mover esta pasta para `openspec/archive/gamb-whatsapp-datas-livres-e-tom-humano/`
