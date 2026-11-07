# Proposal

## Why

O atendente da própria Gamb no WhatsApp (menu de dúvida e reunião) ficou de fora da melhoria feita no atendimento das empresas clientes. Hoje a reunião só aceita a data em `DD-MM-AAAA` e responde "Não entendi a data" para "25 de dezembro", "amanhã" ou "sábado", e a dúvida responde pelo prompt de suporte, que não pede tom de conversa. Quem fala com a Gamb vê um bot mais engessado do que o bot que a Gamb entrega aos clientes.

## What Changes

- Passo de data da reunião (`REUNIAO_DATA`) passa a ler a data em qualquer jeito de escrever, reaproveitando `InterpretadorDeData`: `25/12/2026`, `25-12-26`, `25.12`, `25 12 2026`, `25 de dezembro`, `2026-12-25`, `hoje`, `amanhã`, `depois de amanhã`, inclusive no meio de uma frase.
- Quando a data é duvidosa (só o dia, dia da semana, data sem ano que já passou, data impossível), o bot pergunta "Só para confirmar: você quer dizer sábado, 07-11-2026? Responda *sim* ou me diga a data certa." e segue com o *sim* ou com a data corrigida. Novo estado `REUNIAO_CONFIRMACAO_DATA`, reaproveitando a data pendente do `GerenciadorSessao`.
- As mensagens do bot deixam de exigir o formato: "Para qual data? Pode escrever do seu jeito, por exemplo 16/09/2026 ou 16 de setembro."
- Os prompts de suporte (dúvida no WhatsApp) e comercial (site) do `AssistenteTutoriaService` ganham orientação de tom humano: responder primeiro o que foi perguntado, acompanhar o jeito do cliente, variar a forma de falar, sem roteiro. Os limites de escopo (sem dado de empresa, sem valor, sem prazo, recusa padrão) não mudam.
- Novo item no menu, `3️⃣ - Conhecer a Gamb e ver como podemos ajudar no seu negócio`, e desvio por intenção no texto livre (menu e dúvida): perguntas como "o que vocês fazem", "quem é a Gamb" ou "como resolvem meu problema" recebem a resposta comercial, montada com `base-comercial.md` (`AssistenteTutoriaService.responderComercial`). Quando o cliente descreve um problema, a resposta mostra como o que existe hoje ajuda e apresenta o que está em desenvolvimento como avaliado caso a caso.
- No WhatsApp, o fechamento da resposta comercial convida a digitar *2* para marcar reunião, em vez de mandar para o WhatsApp da Gamb (o cliente já está nele). A resposta comercial do site continua como está. Sem valor, prazo, nome de cliente nem comparação com concorrente.
- Fora de escopo: menu numérico da Gamb, horário (`HH:MM`), limite diário, teste de módulos.

## Capabilities

### New Capabilities

### Modified Capabilities
- `whatsapp`: WPP-13 (a data da reunião aceita qualquer formato, com pergunta na dúvida) e WPP-12 (menu com a opção de conhecer a Gamb, resposta comercial sem valor nem prazo e dúvida com tom de conversa, dentro do mesmo escopo).

## Impact

- Código: `strategy/AtendenteGambStrategy`, `model/EstadoUsuario` (+`REUNIAO_CONFIRMACAO_DATA`, +`ATENDENTE_SOBRE`), `servico/assistente/AssistenteTutoriaService` (prompts).
- Reuso: `InterpretadorDeData` e `GerenciadorSessao.guardarDataPendente/obterDataPendente/limparDataPendente`, já criados pela change `ia-cliente-refletir-base-de-conhecimento`, que precisa estar na `main` (já está, commit ac60c88).
- Testes: `AtendenteGambStrategyTest`, `AssistenteTutoriaServiceTest`.
- Sem mudança de API, banco ou front.
