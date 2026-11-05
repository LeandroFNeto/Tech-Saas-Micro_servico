# Design

## Contexto

- `ControllerWhatsapp.receberMensagem`: `enviadaPorMim()` liga `ativarTravaProprietario(contatoAlvo)`; mensagem de cliente com trava ativa é descartada.
- `ServicoMensagem` envia por `/send-message`, `/send-image` e outros, e não guarda o que enviou.

## Decisões

1. **Registro de envios do bot.** `ServicoMensagem` anota cada envio bem-sucedido (contato normalizado e texto normalizado, ou o id da mensagem quando o gateway devolve) num mapa em memória com validade curta (2 minutos, o mesmo limite de "mensagem antiga"). O controller, ao receber `fromMe=true`, consulta o registro: se bate, foi o bot; ignora, loga "Eco do bot ignorado" e não liga a trava. Se não bate, foi uma pessoa da empresa e a trava liga como hoje.
2. **Casar por contato e texto.** O casamento usa contato e texto com espaços e quebras normalizados. Imagem enviada pelo bot casa com evento sem texto (ou com a legenda) do mesmo contato no mesmo prazo. O casamento por id da mensagem ficou de fora: o formato da resposta do gateway não é garantido, e o contato mais o texto bastam para os dois gateways.
3. **Mesma regra em todo ambiente.** A decisão não lê perfil, host nem variável de ambiente; só o payload e o registro. Vale também para o gateway novo da change de Baileys.
4. **Falha segura.** Sem dados para casar, o evento é tratado como mensagem da pessoa (comportamento atual). O registro tem tamanho limite e descarta os mais antigos.
5. **Alvo da trava é o destinatário.** Na mensagem do dono, o contato alvo vem de `to` (ou `id.remoteJid`) como está, sem a troca por `sender.id` que `resolverLid` faz: nesse evento o `sender` é a empresa. `resolverLid` continua valendo só para mensagem recebida do cliente.
6. **Um cliente, dois identificadores.** `GerenciadorSessao` guarda um mapa de apelidos `@lid` ↔ número, aprendido quando uma mensagem do cliente traz os dois (`from` `@lid` e `sender.id` número). Ligar e consultar a trava passa pelos dois identificadores. Sem apelido aprendido, vale só o identificador recebido (comportamento de hoje).
7. **Log sem dado pessoal** (WPP-06): número mascarado, nunca o texto.

## Riscos

- Eco que chega depois de 2 minutos liga a trava indevidamente; é igual ao comportamento atual.
- Pessoa que digita exatamente o mesmo texto que o bot enviou no mesmo minuto é lida como bot; improvável.
- Memória: mapa pequeno e com expiração; reinício limpa tudo.

## Questões em aberto

- Confirmar nos logs reais que a resposta do bot volta com `fromMe=true` (tarefa 1). Se não voltar, a causa da mudez é outra e o design volta para revisão antes do código.
