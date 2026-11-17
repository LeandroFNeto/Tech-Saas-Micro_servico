# Design

## Context

O gateway (`baileys-gateway/`) usa `@whiskeysockets/baileys` 6.7.24, Node 24, ESM (`"type": "module"`), com armazenamento de chaves próprio em arquivos (`src/auth-state.ts`) no volume `baileys-auth`, uma pasta por sessão. Mantém um mapa `paresLid` (lid → número) alimentado por `remoteJidAlt` e por `signalRepository.lidMapping.getPNForLID` (`src/sessao.ts`), e envia sempre para o JID de número obtido por `onWhatsApp`. O motivo da troca está na proposta (Why).

O guia oficial de migração (`https://baileys.wiki/migration/v7`) lista o que muda no 7.x e que o gateway usa ou pode tocar:
- sessões novas nascem no formato LID e as existentes são migradas automaticamente;
- o armazenamento de chaves precisa suportar as categorias `lid-mapping`, `device-list` e `tctoken`;
- `onWhatsApp` não devolve mais LID (usar `lidMapping.getLIDForPN`); `isJidUser` foi removido;
- protobufs enxutos: só `.create()`, `.encode()` e `.decode()` restam, no lugar de `.fromObject()`;
- ACKs de entrega deixam de ser enviados;
- ESM obrigatório (o gateway já é ESM) e Node 20 ou mais (o Dockerfile usa 24).

A versão publicada como `latest` no npm é `7.0.0-rc14`, uma candidata a release. O 6.7.24 é a tag `legacy`.

## Goals / Non-Goals

**Goals:**
- Conversa de vários turnos nos dois sentidos sem "Aguardando mensagem" nem `Bad MAC` (WPP-25).
- Atualização que preserva a sessão ou a encerra de forma visível (WPP-26).
- Contrato REST e webhook com o Java idênticos aos de hoje.

**Non-Goals:**
- Migrar o negócio para LID (o Java e o painel continuam falando número).
- Trocar para a 7.0.0 final quando sair (change à parte).
- Mexer em grupos: o gateway só atende chat privado.
- Reescrever a fila de envio, o cache de `getMessage` ou a trava do dono.

## Decisions

### 0. Responder pelo mesmo endereço da conversa (primeiro passo)
O log do servidor mostra o cliente escrevendo de `lid:***1059` e o gateway enviando a resposta para `***2580`, o endereço de número do mesmo contato. A conversa no celular do cliente vive no `@lid`, então a resposta cifrada para o endereço de número cria outra sessão de criptografia, e dali vêm `Closing session`, `Bad MAC` e "Aguardando mensagem". A correção é local ao gateway: ao tratar uma mensagem de conversa `@lid` com número conhecido, guardar `número → @lid` (mesma validade do mapa de pares, 24 h) e, no envio, devolver esse `@lid` em `resolverDestinoDeNumero`, antes e depois da consulta `onWhatsApp` (que cobre número com ou sem o nono dígito). Sem conversa por `@lid` conhecida, o destino segue o endereço de número como hoje. O contrato com o Java não muda. Alternativa descartada: mandar o Java usar `isLid`, porque o Java não conhece o `@lid` e o contrato deixaria de ser só número. Os passos 1 a 5 abaixo ficam como segundo passo, se este não bastar na homologação.

### 1. Fixar `7.0.0-rc14`, sem `^`
Fixa a versão para o deploy ser reproduzível, já que uma candidata a release pode mudar entre builds. Alternativa descartada: `latest` solto, porque um `npm ci` futuro pegaria uma rc diferente sem teste. Alternativa descartada: ficar no 6.7.24 e contornar com o JID de LID no envio, porque não corrige a recepção (`Bad MAC` ao ler o cliente) e deixa o gateway em uma versão com a falha conhecida.

### 2. O Java e o painel continuam falando número
O gateway converte `@lid` para número com `remoteJidAlt`, depois `lidMapping.getPNForLID`, depois `paresLid` (como hoje). Mensagem sem número resolvido continua sem ir ao Java com `@lid` cru. Isso mantém o contrato e evita mexer em opt-out, contatos e agenda, que usam o número como chave. O envio continua para o número que o Java manda; o Baileys 7 escolhe a sessão LID por conta.

### 3. Armazenamento de chaves genérico, com o ajuste mínimo
O `auth-state.ts` grava um arquivo por `categoria-id` para qualquer categoria, então `lid-mapping`, `device-list` e `tctoken` entram sem código novo. O ajuste é trocar `proto.Message.AppStateSyncKeyData.fromObject` por `.create` e conferir o nome de arquivo dos ids novos (`nomeDoArquivo` já troca `/` e `:`). Alternativa descartada: usar o `useMultiFileAuthState` oficial, porque perde a gravação atômica, a cópia `.bak` e a leitura com reserva que o projeto já tem testadas.

### 4. Plano de volta com cópia do volume
Antes do deploy, copiar o volume `baileys-auth` para fora do container. A volta ao 6.7.24 restaura a cópia e, se a sessão não conectar, o painel mostra o repareamento (WPP-22) e o cliente pareia de novo. O banco e os contatos não dependem do volume.

### 5. Homologar em número de teste antes de liberar
A hipótese da causa vem do log e de um print, sem prova isolada. A tarefa de homologação cobre os dois sentidos e dez turnos. Se o 7.x não resolver, a change registra o resultado e a investigação segue na causa seguinte (envio para o JID certo, limpeza do volume, outra versão).

## Risks / Trade-offs

- **Versão candidata a release** → versão fixa, testes do gateway cobrindo as bordas que o gateway usa e a troca para a final em change à parte.
- **A hipótese da causa pode estar errada** → critério de aceite é a homologação com conversa de dez turnos; sem ela, não se libera a clientes.
- **A migração automática das sessões pode falhar** → a cópia do volume e o repareamento visível (WPP-26) limitam o dano a um novo pareamento.
- **O 7.x grava chaves que o 6.x não lê** → a volta exige restaurar a cópia ou parear de novo; registrado no plano de volta.
- **Mudança de API não coberta pelos testes** (`signalRepository`, `onWhatsApp`) → checagem de tipos e testes com um socket falso no formato novo.
- **O número de teste pode ter comportamento próprio** (por exemplo conta Business) → testar com dois números de tipos diferentes.

## Migration Plan

1. Cópia do volume `baileys-auth` no servidor.
2. Atualizar a dependência, o `auth-state.ts` e o gateway; rodar `typecheck` e os testes.
3. Deploy pelo workflow (`down` sem `-v` e `up --build`, que preserva o volume).
4. Conferir `restauradas` no log e o status no painel.
5. Homologar com número de teste (tarefa 5.2).
6. Se falhar: voltar `package.json` e `package-lock.json` ao 6.7.24, restaurar a cópia do volume e fazer novo deploy.

## Open Questions

- A 7.0.0 final sai antes de o bot ser liberado aos clientes? Se sair, trocar a versão fixa nesta mesma change.
