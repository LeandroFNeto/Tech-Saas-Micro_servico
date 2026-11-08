# Design

## Context

`ServicoIA` é o único ponto de chamada ao Gemini (`gerarRespostaHumanizada`), usado por `LocacaoStrategy` (empresas com o módulo `IA_GEMINI`), por `AssistenteTutoriaService` (Suporte e comercial da Gamb) e pelo diagnóstico. Hoje não há contagem de chamadas: só o 429 é logado e devolve `null`. Em `LocacaoStrategy`, texto livre cai em `responderAtendimento` apenas em `tratarInicio` e no `default` do menu, e sem resposta vem "Opção inválida". Em `AtendenteGambStrategy`, texto fora das opções repete `MENSAGEM_MENU`. A cota gratuita e o timeout de 3 s são o limite que governa o desenho.

## Goals / Non-Goals

**Goals:**
- Mais liberdade de conversa sem aumentar de forma relevante as chamadas ao modelo.
- Uma única guarda de cota, no `ServicoIA`, valendo para todos os chamadores.

**Non-Goals:**
- Trocar o provedor, usar plano pago ou guardar histórico longo da conversa.
- Mudar o fluxo de reserva, reunião ou o limite diário da Gamb (WPP-14).

## Decisions

1. **Conversa curta local (`ConversaCurta`)**: classe pura que normaliza o texto (sem acento, minúsculas), reconhece cumprimento, "tudo bem", agradecimento e despedida por padrões e devolve uma frase de um conjunto por situação. Escolhe a frase evitando a última usada pelo mesmo contato (mapa em memória com expiração). Alternativa descartada: pedir ao modelo; gastaria cota com o caso mais frequente e mais simples.
2. **Guarda de cota no `ServicoIA`**: contadores em memória por janela de 1 minuto e de 1 dia (fuso de Brasília), tetos em `gemini.max-chamadas-por-minuto` e `gemini.max-chamadas-por-dia`, e pausa `gemini.pausa-apos-429-segundos` armada no 429. Dentro da pausa ou do teto, `gerarRespostaHumanizada` devolve `null` sem rede. Alternativa descartada: guarda por empresa; a cota é da chave, então o teto é global. Trade-off: uma empresa movimentada pode consumir a cota das demais; mitigado pelo cache e pela conversa curta local.
   **Teto de gasto diário em reais (trava principal)**: a cada resposta, o `ServicoIA` soma o custo estimado a partir de `usageMetadata` (tokens de entrada e de saída, incluindo pensamento) vezes os preços por milhão e a cotação, todos em propriedades (`gemini.preco-entrada-usd-por-milhao=0.30`, `gemini.preco-saida-usd-por-milhao=2.50`, `gemini.cotacao-usd-brl=5.50`). Ao chegar em `gemini.teto-gasto-diario-reais` (padrão 15,00), segue o fallback até a virada do dia em Brasília. Com cerca de R$ 0,005 por chamada, R$ 15 equivalem a uns 3 mil chamadas por dia, no máximo R$ 450 por mês. Sem `usageMetadata` na resposta, conta-se um custo estimado fixo por chamada. Os tetos por minuto e por dia continuam como proteção contra rajada. Alternativa descartada: depender só do alerta de orçamento do Google Cloud, que avisa mas não bloqueia. O contador é uma estimativa em memória; a fatura real manda, e o reinício do servidor zera o contador do dia (risco aceito na escala atual, e por isso vale também configurar o alerta de orçamento no Google Cloud).

3. **Cache de resposta**: chave = hash do prompt completo, validade configurável (`gemini.cache-minutos`, padrão 360), tamanho máximo limitado. Cobre a opção `3` da Gamb e perguntas repetidas com os mesmos dados. Prompts com mensagem do cliente diferente não colidem.
4. **Conversa livre com fallback local**: novo `ServicoIA.conversar(empresa, contato, texto, menu)` que tenta, em ordem, conversa curta local, cache/modelo e mensagem humana local com o convite a `menu`. `LocacaoStrategy` passa a usá-lo no lugar de "Opção inválida" quando a IA está ligada; empresas sem IA seguem como hoje. `AtendenteGambStrategy` o usa para texto livre no menu e para o cumprimento inicial, antes do menu.
5. **Orientação do modelo**: `atendimento-empresa.md` ganha a permissão de conversa curta e de adaptar a ideia ao ramo da empresa, mantendo as proibições; `base-comercial.md` ganha o roteiro de adaptar a Gamb ao negócio descrito. Os testes de prompt (IA-07) são ajustados.

## Risks / Trade-offs

- [Cumprimento mal reconhecido vira chamada ao modelo] → padrões em testes com variações reais (oi, oii, bom dia, boa noite, e aí, tudo bem?).
- [Modelo mais livre inventa dado] → mantidas as proibições no prompt e teste do prompt; o dado vem só do cadastro.
- [Estado em memória se perde no reinício] → aceitável na escala atual; a contagem recomeça e a pausa some, no pior caso gerando um 429 que rearma a pausa.
- [Resposta em cache envelhece] → validade curta e chave pelo prompt completo, que inclui os dados cadastrados.
