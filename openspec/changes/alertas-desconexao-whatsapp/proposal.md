# Proposal: Alertas ao cliente quando o WhatsApp da empresa desconecta

> Rascunho para trabalhar depois da migração para o Baileys. Só a proposta existe; design, spec e tarefas serão criados quando a change for retomada.

## Why

Quando a sessão do WhatsApp cai e não volta sozinha, o cliente só descobre se abrir o painel e o bot fica sem responder. Na migração para o Baileys, a reconexão por queda de rede, reinício do servidor e celular sem bateria é automática. Fica o caso que exige ação do cliente: o aparelho vinculado é desfeito (`loggedOut`), em especial quando o celular passa **14 dias sem internet**, limite do WhatsApp. Aí é preciso parear de novo, e o cliente precisa saber.

## What Changes

- Aviso ao cliente quando a sessão fica `DISCONNECTED` e precisa de novo pareamento (canal a definir: banner no painel, e-mail ou os dois).
- Aviso antes do limite, quando o gateway não consegue reconectar por tempo próximo de 14 dias, se o gateway tiver como medir.
- Escopo inicial do prazo: **14 dias** (limite do WhatsApp). Outros limiares ficam para depois.
- Sem mudança de pareamento nem de reconexão (changes `migrar-wppconnect-para-baileys` e `conexao-whatsapp-por-codigo`).

## Capabilities

### Modified Capabilities

- `whatsapp`: novo requisito de alerta de sessão desconectada (a definir).

## Impact

- A definir ao retomar: backend (detecção e envio), painel (banner) e contrato YAML. Depende do status `DISCONNECTED`/`INITIALIZING` e da rota de saúde da change `migrar-wppconnect-para-baileys`.
