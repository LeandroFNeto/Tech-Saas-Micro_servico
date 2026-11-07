# trava-proprietario-mensagens-antigas

## Problema
Atualmente, o bot processa mensagens históricas/antigas descarregadas durante a sincronização ou reconexão do WPPConnect. Além disso, quando o proprietário/atendente humano responde diretamente pelo celular (`fromMe=true`), o bot descarta apenas aquela mensagem individual, mas não pausa o atendimento; na primeira mensagem seguinte do cliente, o bot reage e assume a conversa, atropelando o atendimento humano.

## O que muda
### Entra
- Mapeamento de `timestamp` no `WhatsappWebhookDTO`.
- Descarte automático de mensagens antigas recebidas com atraso maior que a tolerância (120 segundos / 2 minutos em relação ao relógio atual).
- Trava temporal de atendimento do proprietário no `GerenciadorSessao`: quando o proprietário envia uma mensagem (`fromMe=true`) em uma conversa privada, o bot entra em pausa de 20 minutos para aquele contato/remetente, descartando mensagens automáticas do robô e mantendo silêncio para preservar a conversa humana.

### Altera
- `WhatsappWebhookDTO`: recebe campo `timestamp` e método para detectar mensagem antiga.
- `GerenciadorSessao`: armazena e consulta a trava temporal de 20 minutos por chave de contato.
- `ControllerWhatsapp`: integra o descarte de mensagens antigas e a ativação/checagem da trava de 20 minutos do proprietário.
- `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml` e `especificacao/back/paths/webhook-whatsapp.yaml`: documentam `timestamp`, rejeição de mensagens antigas e trava de 20 minutos.
- `openspec/specs/whatsapp/spec.md`: atualiza o requisito de filtro e soberania do proprietário.

### Sai
- Comportamento de reativar o bot imediatamente após intervenção humana do dono.

## Fora de escopo
- Alterações em tela ou painel web do cliente/admin.
- Pausa manual via endpoint HTTP ou comando de texto.

## Domínios
- spec: `openspec/specs/whatsapp/spec.md`
