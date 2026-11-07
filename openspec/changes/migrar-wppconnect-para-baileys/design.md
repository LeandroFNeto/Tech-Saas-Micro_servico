# Design

## Context

Ver `proposal.md` - Why. O backend usa o WPPConnect por uma superfície pequena de HTTP e um webhook:

- Saída: `generate-token`, `start-session` (com `webhook` e `waitQrCode`), `status-session`, `qrcode-session`, `close-session`, `send-message`, `send-image`.
- Entrada: `POST /webhook/whatsapp` com `WhatsappWebhookDTO` (sessão, remetente, texto, `fromMe`, `timestamp`, eventos de QR e conexão).
- `ServicoWppConnect` cobre sessão, QR e token. `ServicoMensagem` envia mensagem e imagem e tem a URL `http://wppconnect:21465` escrita no código.
- Servidor de 8 GB, escala pequena. O Chromium por sessão é o consumo dominante.

## Goals / Non-Goals

**Goals:**
- Trocar o provedor sem alterar strategies, `ControllerWhatsapp` nem o contrato do webhook.
- Cair o consumo de RAM por sessão para dezenas de MB e comprovar com medição.
- Poder voltar ao WPPConnect na janela de virada.

**Non-Goals:**
- Introduzir a interface `ProvedorWhatsapp` no Java.
- Migrar as sessões existentes do WPPConnect.
- Rodar os dois provedores em produção ao mesmo tempo para a mesma sessão.

## Decisions

1. **Gateway Node que imita a API do WPPConnect, em vez de adaptador no Java.** O gateway reimplementa só as rotas listadas acima e posta no webhook o mesmo JSON. Isso deixa o Java quase intacto e a troca reversível pelo compose. Alternativa descartada: interface `ProvedorWhatsapp` no Java, mais limpa, mas toca o controller, o DTO e os testes no mês da virada. Fica como refatoração posterior.

2. **Mesmo prefixo de rota e autenticação.** O gateway aceita `/api/{sessao}/{secret}/generate-token` e devolve um token; as demais rotas exigem `Bearer`. O backend mantém `wppconnect.secret-key` sem mudança de lógica. O segredo é comparado em tempo constante e nunca vai para log.

3. **Um processo Node, um socket Baileys por sessão.** Sessões vivem no mesmo processo, sem Chromium. Não há `shm_size` no serviço.

4. **Autenticação em volume dedicado.** `useMultiFileAuthState` por sessão em um volume Docker próprio (`baileys-auth`), com uma pasta por sessão. Serve para a escala pequena. Perder o volume obriga a repareamento de todas as sessões, por isso entra no backup.

5. **Tradução do payload no gateway.** O gateway converte a mensagem do Baileys para o JSON do `WhatsappWebhookDTO`: `session`, `from` no formato `<numero>@c.us`, `fromMe`, `timestamp`, texto e tipo. Contato em `@lid` é resolvido para o número real quando o Baileys o expõe; se não for possível resolver, a mensagem é descartada e registrada sem o conteúdo. Eventos de QR e de conexão saem nos mesmos nomes que o controller já trata.

6. **QR.** O gateway guarda o último QR por sessão e responde em `status-session` (campo `qrcode`) e em `qrcode-session` (PNG), para o backend não mudar. Posta o evento de QR no webhook como hoje.

7. **Substituição das URLs no Java.** `ServicoMensagem` deixa de ter o host no código e passa a ler `wppconnect.url`, já existente. O nome do host do gateway entra por variável de ambiente; o nome das propriedades `wppconnect.*` não muda nesta change.

8. **Versão do Baileys fixada** no `package.json` do gateway, sem intervalo, e atualizada só por decisão explícita.

## Risks / Trade-offs

- [Todos reescaneiam o QR] → combinar a janela com cada empresa e a atendente da Gamb; ordem de virada começa pela sessão da Gamb.
- [Baileys não é oficial; protocolo pode quebrar ou gerar banimento] → versão fixada, volume de autenticação com backup e plano de reversão ao container antigo.
- [Contato `@lid` sem número resolvível] → descartar com log mascarado e tratar o caso como pendência do bot de teste, sem inventar número.
- [Diferença sutil de status e de eventos entre os provedores] → o gateway devolve os mesmos valores de status que `normalizarStatus` já reconhece (`CONNECTED`, `QRCODE`, `DISCONNECTED`); teste de contrato cobre cada um.
- [Perda do volume de autenticação] → backup do volume e procedimento de repareamento documentado.
- [Ganho de RAM menor que o previsto] → medir `docker stats` antes e depois; o critério é o gateway usar menos que uma sessão do WPPConnect.

## Migration Plan

1. Medir `docker stats` do WPPConnect no VPS.
2. Subir o gateway ao lado, em porta e nome diferentes, apontando uma sessão de teste.
3. Validar o contrato com testes: token, start-session, QR, status, envio, webhook com mensagem, `fromMe`, mensagem antiga e `@lid`.
4. Janela de virada: parar o WPPConnect, apontar `WPPCONNECT_URL` para o gateway, reparear sessão por sessão, conferir uma mensagem real em cada número.
5. Manter o container antigo parado, com o volume `dados-wpp` preservado, por um período de reversão. Depois remover o serviço e o `wppconnect-server/`.
6. Medir de novo e registrar o ganho.

**Reversão:** voltar `WPPCONNECT_URL` ao container antigo e subir o serviço; as sessões antigas voltam sem novo QR, porque o volume foi preservado.
