# ativar-trava-do-dono-no-gateway

## Problema

A trava Anti-X9 de 20 minutos (WPP-02) existe no Java, mas nunca dispara no uso real: o dono responde o cliente pelo celular e o bot continua respondendo por cima. A change `travar-bot-so-quando-o-dono-escreve` corrigiu o alvo da trava e o eco do bot, mas partiu de uma suposição que a leitura do gateway desmentiu: a de que a mensagem do dono chega ao Java com `fromMe=true`. Ela não chega. Duas falhas em série, lidas no código:

1. **O gateway não envia a mensagem do dono.** Em `wppconnect-server/src/config.ts`, `webhook.onSelfMessage` é `false`. O gateway só posta no webhook o evento `onmessage` (mensagem recebida, nunca a do próprio número) e o evento `onselfmessage` só sai quando essa opção está ligada. Resultado: o que a pessoa da empresa digita no celular ou no WhatsApp Web não chega ao Java, a trava nunca liga e o `registrarOuAtualizarContato`/estratégia segue atendendo.
2. **O Java descartaria o evento mesmo se ele chegasse.** `WhatsappWebhookDTO.ehEventoConexao()` lista `onselfmessage` como evento de conexão, e `ControllerWhatsapp` responde 200 e sai nesse teste antes de olhar `enviadaPorMim()`. Os testes passam porque simulam `fromMe=true` sem o campo `event`, que o gateway real sempre preenche.

Quando as duas forem corrigidas aparece uma terceira, que hoje está escondida:

3. **Corrida no reconhecimento do eco do bot.** `ServicoMensagem` anota o envio em `RegistroEnviosBot` só depois de `postWpp` voltar. O gateway dispara `onselfmessage` assim que a mensagem nasce, enquanto o `send-message` ainda espera o ack. O eco pode chegar ao webhook antes do registro existir, ser lido como "o dono digitou" e silenciar o bot por 20 minutos logo depois da própria resposta.

A regra de negócio continua a mesma: se uma pessoa da empresa escreveu para o cliente, o bot fica quieto naquela conversa por 20 minutos, renovados a cada nova mensagem da pessoa; se foi o bot, nada muda.

## O que muda

### Entra
- Gateway: `onSelfMessage` ligado por padrão, com variável de ambiente para desligar em emergência.
- Java: `onselfmessage` deixa de ser tratado como evento de conexão e passa a ser mensagem do dono (`fromMe=true`), mesmo se o payload não trouxer o campo.
- `ServicoMensagem` registra o envio no `RegistroEnviosBot` antes de chamar o gateway e desfaz o registro se o envio falhar.
- Log de diagnóstico no Java quando chega evento de mensagem do dono (número mascarado, sem texto), para provar na conversa real que o caminho está de pé.
- Teste de contrato que usa o payload real do gateway (com `event: "onselfmessage"`), no lugar do payload sem evento.

### Altera
- WPP-02, item 1: a mensagem do dono chega pelo evento `onselfmessage` do gateway; item 4 deixa de listar esse evento como ruído de conexão.

### Sai
- Nada.

## Capabilities

### Modified Capabilities
- `whatsapp`: WPP-02.

## Suposições

- O tempo continua 20 minutos fixos, renovados a cada mensagem do dono. Tornar o tempo configurável por empresa fica de fora.
- Estado da trava em memória (`GerenciadorSessao`): reiniciar o container zera as travas. Aceitável na escala atual.
- O texto da mensagem do dono no `onselfmessage` do WPPConnect traz `fromMe=true`, `to` com o cliente e `body` com o texto; é o formato que `resolverContatoAlvo()` já lê.
- Esta change substitui o texto de WPP-02 do delta de `travar-bot-so-quando-o-dono-escreve` (superconjunto). Sincronizar uma ou as duas dá o mesmo resultado se a ordem for respeitada.

## Fora de escopo

- Trocar o gateway (change `migrar-wppconnect-para-baileys`); só se registra ali que o novo gateway precisa postar a mensagem do dono.
- Botão no painel para liberar o bot antes dos 20 minutos.
- Persistir a trava em banco.

## Impacto

- Gateway: `wppconnect-server/src/config.ts` (e teste Jest da configuração).
- Back: `WhatsappWebhookDTO`, `ControllerWhatsapp`, `ServicoMensagem`, `RegistroEnviosBot`.
- Spec: `openspec/specs/whatsapp/spec.md` (WPP-02); contrato `especificacao/back/paths/webhook-whatsapp.yaml` só se a descrição mudar.
- Testes: `WhatsappWebhookDTOTest`, `ControllerWhatsappTest`, `webhook.feature`, `ServicoMensagemTest`, `RegistroEnviosBotTest`.
- Operação: rebuild e redeploy do container `wppconnect` (a imagem copia `config.ts`).
