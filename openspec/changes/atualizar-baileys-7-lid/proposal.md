# Proposal

## Why

Depois de parear o número limpo no gateway Baileys 6.7.24, só a primeira resposta do bot chega ao cliente. As seguintes ficam no relógio "Aguardando mensagem" no celular dele, e a segunda mensagem do cliente não é lida pelo bot (o log do gateway mostra `Bad MAC`, `No matching sessions found` e `failed to decrypt message`, com contatos `lid:`). A hipótese de trabalho é que o 6.7.x cria sessões de criptografia diferentes para o endereço `@lid` e para o número `@s.whatsapp.net` do mesmo contato, e cada lado cifra com uma sessão que o outro não tem. O Baileys 7 cria as sessões novas no formato LID e migra as existentes. O bot ainda não foi liberado aos clientes, então é o momento de menor custo para trocar de versão.

## What Changes

- **Primeiro passo, no 6.7.24:** o gateway passa a responder pelo mesmo endereço da conversa. Quando o cliente escreve de um `@lid` e o número dele é conhecido, a resposta para esse número vai ao `@lid`, e não ao `@s.whatsapp.net`. Evidência: o log mostrou a mensagem chegando de `lid:***1059` e a resposta saindo para `***2580`. Entregue em `sessao.ts`, com teste de regressão.
- **Segundo passo, condicional:** só se a homologação do primeiro passo não fechar o problema, trocar a biblioteca.
- Trocar `@whiskeysockets/baileys` de `6.7.24` para `7.0.0-rc14` (versão fixa, sem `^`), no `baileys-gateway/package.json` e no `package-lock.json`.
- Adaptar o armazenamento de chaves para as categorias novas (`lid-mapping`, `device-list`, `tctoken`) e para os protobufs enxutos (`.create()` no lugar de `.fromObject()`).
- Adaptar o gateway às mudanças de API que o guia de migração do Baileys 7 lista e que o código usa: `onWhatsApp` deixa de devolver LID, `isJidUser` some, os ACKs de entrega deixam de ser enviados.
- Usar o mapa PN/LID do Baileys 7 para identificar o cliente e manter o número em `from`/`to` do webhook, sem mudar o contrato com o Java.
- Manter o cache de mensagens enviadas (`getMessage`) já entregue em `7ed87c9`.
- Documentar o plano de volta: voltar a `6.7.24` exige apagar a sessão e parear de novo, porque o 7.x grava chaves num formato que o 6.x não lê.
- Validar em número de teste, com conversa de vários turnos e nos dois sentidos, antes de liberar a clientes.
- Sem **BREAKING** para o Java, o painel ou o contrato REST: as rotas, o webhook e os formatos de `from`/`to` continuam iguais. O que muda é a pasta de sessão no volume `baileys-auth`.

## Capabilities

### New Capabilities

### Modified Capabilities
- `whatsapp`: duas regras novas, WPP-25 (conversa de vários turnos sem mensagem presa em "aguardando", nos dois sentidos) e WPP-26 (a atualização do gateway preserva a sessão pareada ou a encerra de forma visível no painel).

## Impact

- Código do primeiro passo: `baileys-gateway/src/sessao.ts` e `test/sessao.test.ts`.
- Código do segundo passo (condicional): `baileys-gateway/package.json`, `package-lock.json`, `src/auth-state.ts`, `src/fabrica-baileys.ts`, `src/sessao.ts`, `src/mensagem.ts` e os testes em `baileys-gateway/test/`.
- Dependência de versão candidata a release (`rc14`): sujeita a mudança até a 7.0.0 final. A versão fica fixa e a troca para a final é uma change à parte.
- Operação: o volume `baileys-auth` guarda sessões que o 7.x pode migrar; fazer cópia do volume antes do deploy (tarefa na lista) para a volta.
- Sem alteração em Java, front, banco ou contratos YAML.
- Segurança (`.cursor/rules/seguranca.mdc`): nenhuma superfície nova; o log continua sem número completo nem texto (WPP-06, WPP-21).
