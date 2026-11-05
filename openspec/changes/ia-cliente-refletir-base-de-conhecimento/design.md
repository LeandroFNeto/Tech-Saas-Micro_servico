# Design

## Contexto

- `LocacaoStrategy.tratarInicio` e o caso `default` de `tratarMenuPrincipal` chamam `servicoIA.responderAtendimento(empresa, texto)`.
- `construirPromptEmpresa` monta o prompt com nome, ramo, preços, regras e mapa, mais 5 instruções fixas.
- `ServicoMenu.montarMenuPrincipal` já devolve o menu numerado com os módulos ativos, na ordem de exibição.
- `AssistenteTutoriaService` já carrega `assistente/base-comercial.md` pelo classpath.

## Decisões

1. **Recorte do documento em recurso próprio.** Criar `src/main/resources/assistente/atendimento-empresa.md` com: a ideia do produto (atendimento rápido, reserva conferida na Google Agenda, isolamento por empresa) e as regras de conduta. Não copiar as seções de painel, admin e diagnóstico. O documento em `docs/` segue como fonte; o recurso é o recorte, e a tarefa de fechamento obriga a atualizar os dois juntos.
2. **Persona humana no lugar do roteiro.** O prompt descreve quem fala: alguém da equipe da empresa, atencioso e natural. Orientações: responder primeiro o que o cliente perguntou; acompanhar o tom dele; usar o primeiro nome só se ele se apresentou; variar a abertura e o fechamento; pouca formalidade e no máximo um emoji quando combinar com o tom do cliente; frases de WhatsApp, sem lista nem texto de folheto. O lembrete do menu aparece só quando o cliente parece perdido, pediu algo que o menu resolve ou acabou de chegar, e não em toda resposta. Sai o teto de 2 a 3 frases, no lugar entra "curto, como numa conversa de WhatsApp".
3. **Menu real no prompt.** `ServicoIA` recebe o texto do menu pelo `ServicoMenu` (já montado para a empresa) e o coloca no prompt como "Opções disponíveis". A instrução final passa a ser "lembre que ele pode digitar o número de uma das opções disponíveis", sem citar números fixos. Se o menu não puder ser montado, o prompt omite a lista e pede só para digitar `menu`.
4. **Módulos e modo de locação.** O prompt lista os módulos ativos (rótulos do catálogo) e diz se a locação é por hora ou por diária, para a IA não oferecer o que a empresa não tem.
5. **Conduta explícita.** Instruções novas: só dados cadastrados; sem dado, dizer que um atendente responde; nunca confirmar nem prometer reserva ("a reserva é uma solicitação feita pelo menu"); nunca citar credencial, ID de agenda nem dado de outra empresa (IA-05).
6. **Tamanho do prompt.** Texto curto e fixo, para não aumentar muito o consumo de tokens (RNF04). O recurso é lido uma vez e guardado em memória.
7. **Falha segura.** Recurso ausente ou erro de leitura: usar as instruções atuais já corrigidas (sem os números fixos). IA-06 continua: erro ou cota da IA cai no menu determinístico.

8. **Leitor de datas.** `InterpretadorDeData` (sem estado) devolve `Certa`, `Duvidosa` (com sugestão opcional) ou `Nenhuma`. A ordem de leitura vai do mais específico ao mais frouxo: ISO, `d/m/a` com `/ - .`, dia com nome do mês, `dia d m`, três números com espaço, só `dia d`, `hoje/amanhã/depois de amanhã`, dia da semana. Separador por espaço sem ano e número solto não valem, para não confundir "somos 20 pessoas" com data. Data certa vai direto ao sistema; duvidosa pergunta.
9. **Onde a data entra.** Em `INICIO` e no menu (texto que não é opção), antes da IA, e só se a empresa tem agenda; nos estados de espera de data da pesquisa e da reserva. Estado novo `ESPERANDO_CONFIRMACAO_DATA` guarda a data sugerida e o estado de retorno em `GerenciadorSessao`; *sim* aplica, *não* volta a pedir, outra data é lida de novo.

## Riscos

- Frase com número que parece data ("18.12" como preço) pode abrir a consulta; limitado por exigir mês válido e separador explícito.
- Humanizar pode soltar demais o modelo; a conduta (decisão 5) é o freio e vem depois da persona no prompt.
- Resposta da IA continua sendo texto gerado; a conduta no prompt reduz, mas não elimina, afirmação indevida. Os testes verificam o prompt, não a resposta do modelo.
- Documento e recurso podem divergir com o tempo; a tarefa de fechamento cobre isso.
- Menu muito longo aumenta o prompt; empresas têm no máximo os módulos do catálogo, então é pequeno.

## Questões em aberto

- Confirmar qual "ponto" do documento era a prioridade (ver Suposições da proposta).
