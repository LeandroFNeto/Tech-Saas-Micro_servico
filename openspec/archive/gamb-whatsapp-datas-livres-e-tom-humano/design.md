# Design

## Contexto

`AtendenteGambStrategy` conduz motivo, data, horário e confirmação da reunião com estados próprios (`REUNIAO_*`). `tratarData` faz `LocalDate.parse` de `DD-MM-AAAA` e recusa o resto. A dúvida chama `AssistenteTutoriaService.responderSuporte`, cujo prompt (`construirPromptSuporte`) trata só de escopo. O leitor de datas e a data pendente já existem no atendimento das empresas (change `ia-cliente-refletir-base-de-conhecimento`).

## Decisões

1. **Reusar `InterpretadorDeData`** sem alteração. Ele é sem estado e devolve `Certa`, `Duvidosa` (com sugestão opcional) ou `Nenhuma`. Data certa segue o fluxo atual de validações (passada, limite de dias, dia fechado, horários livres).
2. **Estado novo `REUNIAO_CONFIRMACAO_DATA`** em `EstadoUsuario`, tratado no `switch` da Gamb. A data sugerida fica em `GerenciadorSessao.guardarDataPendente(numero, data, REUNIAO_DATA)`; *sim* aplica, *não* volta a `REUNIAO_DATA`, outra data é lida de novo, texto sem data repete a pergunta. `limparReuniao` e a volta ao menu limpam a data pendente.
3. **Extrair a validação de data** de `tratarData` para um método que recebe a `LocalDate`, para ser chamado pela data certa e pela confirmada, sem duplicar as regras.
4. **Respostas continuam em `DD-MM-AAAA`** (`FORMATO_DATA`), só a entrada fica livre. `MENSAGEM_PEDIR_DATA` passa a dizer que pode escrever do seu jeito.
5. **Tom humano só no prompt.** `construirPromptSuporte` e `construirPromptComercial` ganham um bloco curto de tom (responder primeiro o que foi perguntado, acompanhar o cliente, variar, sem roteiro), no lugar de "poucas frases, tom humano e direto" onde couber. Os blocos PODE/NÃO PODE, a recusa e o filtro de valor (`citaValor`) ficam intactos. Os fallbacks determinísticos não mudam.
6. **Menu da Gamb: `1` e `2` não mudam, entra o `3`.** `MENSAGEM_MENU` ganha "3️⃣ - Conhecer a Gamb e ver como podemos ajudar no seu negócio". Data digitada em `ATENDENTE_MENU` ou `ATENDENTE_DUVIDA` não abre a reunião sozinha; o cliente escolhe a opção 2.
7. **Resposta comercial no WhatsApp.** Novo estado `ATENDENTE_SOBRE`. A opção `3` e o texto livre com intenção comercial (`gamb`, `o que vocês fazem`, `quem são`, `como resolvem`, `meu problema`, `ajudar meu negócio`, comparados sem acento) chamam um novo `AssistenteTutoriaService.responderComercialWhatsapp(mensagem)`. A intenção é testada em `ATENDENTE_MENU` (antes de repetir o menu) e em `ATENDENTE_DUVIDA` (antes do Suporte); o Suporte continua respondendo o que é do app.
8. **Canal no prompt comercial.** `construirPromptComercial` e `gerarFallbackComercial` recebem o canal (SITE ou WHATSAPP). No site tudo fica igual. No WhatsApp, `textoContato()` dá lugar a "digite *2* para marcar uma reunião com a equipe". O prompt ganha o pedido: se o cliente descreveu um problema, ligar ao que existe hoje (assistente no WhatsApp, reserva pela Google Agenda, painel) e tratar o resto como em desenvolvimento, avaliado caso a caso, sem prometer.
9. **Limites iguais aos do site.** Sem valor (`citaValor` e fallback), sem prazo, sem nome de cliente, sem concorrente; `tentaBurlarEscopo` e `RECUSA_COMERCIAL` valem aqui também. A recusa comercial, que cita "pelo WhatsApp", ganha variante do canal WhatsApp.

## Riscos

- Suporte e comercial têm escopo estrito; afrouxar o tom pode soltar demais. Mitigação: o bloco de escopo continua depois do bloco de tom e os testes conferem as duas coisas.
- Desviar texto livre para a resposta comercial pode capturar pergunta que era do app ("como a Gamb conecta o WhatsApp?"). Mitigação: lista curta de intenções comerciais e testes de pergunta do app que continuam no Suporte.
- "dia 25" ou "sábado" viram sugestão e pedem confirmação; um número solto ("25") não é data e cai na repetição do pedido.
