# Proposal

## Why

O módulo de IA conversa com pouca liberdade: "oi", "bom dia" ou "obrigado" caem no menu ou em "Opção inválida", e a atendente da Gamb repete o menu para qualquer texto fora das opções. A equipe quer que a IA converse como uma pessoa do negócio, cumprimente, faça conversa curta e adapte a ideia ao negócio de quem está falando, em todas as sessões que usam o módulo (empresas clientes com IA e atendente da Gamb). O obstáculo é o Gemini na versão gratuita: cota curta, erro 429 e timeout de 3 segundos impedem chamar o modelo a cada mensagem.

## What Changes

- Camada de conversa curta local no módulo de IA: cumprimentos, "tudo bem", agradecimento e despedida são respondidos sem chamar o modelo, com várias frases por situação, sem repetir a mesma em mensagens seguidas do mesmo contato.
- Orientação do modelo mais livre: conversa natural dentro do ramo do negócio, adaptando a ideia ao negócio de quem fala (empresa cliente: ramo e dados cadastrados; Gamb: negócio descrito pelo cliente), sem inventar dado.
- Texto livre fora das opções deixa de resultar só em menu ou "Opção inválida" quando a IA está ligada: segue para a conversa livre; sem modelo, vem uma resposta humana local com o convite ao `menu`.
- Guarda de cota no módulo: tetos de chamadas por minuto e por dia, pausa após 429, reaproveitamento de resposta recente para perguntas idênticas.
- Mantidas as regras atuais: sem preço ou horário não cadastrado, sem confirmar reserva, sem dado de outra empresa; na Gamb, sem valor, prazo, garantia ou cliente citado, e convite a digitar *2*. O limite diário por número da Gamb (WPP-14) continua valendo.

## Capabilities

### New Capabilities
<!-- Nenhuma -->

### Modified Capabilities
- `ia`: IA-06 (guarda de cota, pausa após 429) e novo IA-09 (conversa livre com conversa curta local).
- `whatsapp`: WPP-12 (texto livre no menu da atendente da Gamb segue a conversa livre do IA-09).

## Impact

- Código: `ServicoIA` (guarda de cota, cache, conversa curta), novo componente de conversa curta, `LocacaoStrategy` e `AtendenteGambStrategy` (usar a conversa livre antes do menu/"Opção inválida"), `atendimento-empresa.md` e `base-comercial.md`, `application.properties`.
- Sem mudança de API HTTP nem de banco.
- Fora de escopo: trocar de provedor, plano pago, memória longa da conversa, empresas sem o módulo IA ligado (continuam no menu).
