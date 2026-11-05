# travar-bot-so-quando-o-dono-escreve

## Problema

O bot não para quando a pessoa da empresa assume a conversa: o dono escreve para o cliente, o cliente responde e o bot continua respondendo por cima. A trava Anti-X9 de 20 minutos (WPP-02) deveria segurar o bot e não segura. Causas prováveis, lidas no código (a confirmar na tarefa 1):

- **Trava no contato errado.** Para mensagem do dono (`fromMe=true`), `WhatsappWebhookDTO.resolverContatoAlvo()` pega o destinatário (`to`) e passa por `resolverLid`. Quando o destinatário é um `@lid`, `resolverLid` troca pelo `sender.id`, que nesse evento é o próprio número da empresa. A trava fica gravada no número da empresa e não no do cliente, então a mensagem seguinte do cliente passa direto.
- **Cliente com dois identificadores.** O mesmo cliente pode chegar como `@lid` ou como número (`@c.us`). A trava é gravada e consultada por dígitos, então um identificador não enxerga a trava do outro.

E, quando a trava passa a funcionar, existe o efeito colateral que a change já trata:

- `ControllerWhatsapp` trata todo evento com `fromMe=true` como "o dono digitou" e liga a trava Anti-X9 de 20 minutos para aquele contato (WPP-02).
- O número da empresa é o mesmo do bot. Quando o bot responde pela API (`ServicoMensagem`), o gateway devolve essa resposta também como `fromMe=true`. O sistema não separa "o bot enviou" de "a pessoa da empresa digitou no celular", então a própria resposta do bot liga a trava e o bot se silencia por 20 minutos.

A regra de negócio é: se o número da empresa mandou a mensagem e foi uma pessoa (não o bot), liga a trava; se foi o bot, não liga. Isso vale em qualquer ambiente (desenvolvimento, homologação, produção; celular, WhatsApp Web, gateway diferente).

## O que muda

### Entra
- Trava gravada sempre no cliente (destinatário da mensagem do dono), nunca no número da empresa, também quando o destinatário vem como `@lid`.
- Trava vista pelos dois identificadores do cliente (`@lid` e número), em qualquer ambiente.
- Reconhecer a mensagem que o próprio bot enviou quando ela volta no webhook como `fromMe=true` e não tratá-la como intervenção do dono.
- Trava de 20 minutos ligada só quando uma pessoa da empresa escreve para o cliente.
- Log (número mascarado, sem texto) de "Eco do bot ignorado".

### Altera
- WPP-02, item 1: a trava liga só para mensagem do número da empresa que não foi enviada pelo bot.

### Sai
- Nada. Os avisos do bot ("Opção inválida...", "agendamento indisponível") ficam como estão e não entram nesta change.

## Capabilities

### Modified Capabilities
- `whatsapp`: WPP-02.

## Suposições

- Estado em memória (`GerenciadorSessao`), dado o porte pequeno do sistema.
- A change `migrar-wppconnect-para-baileys` troca o gateway; esta não depende dela, mas a regra do eco precisa valer nos dois.

## Fora de escopo

- Mudar o tempo da trava (20 min) ou criar botão no painel para liberar o bot.
- Limitar ou reescrever os avisos do bot.
- Autenticar o webhook (change `autenticar-webhook-whatsapp`).

## Impacto

- Back: `ControllerWhatsapp`, `ServicoMensagem`, DTO do webhook.
- Spec: `openspec/specs/whatsapp/spec.md`; contrato `especificacao/back/paths/webhook-whatsapp.yaml` só se a descrição mudar.
- Testes: `ControllerWhatsappTest`, `webhook.feature`, teste novo do registro de envios.
