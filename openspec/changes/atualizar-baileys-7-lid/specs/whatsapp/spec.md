# Spec Delta: Conversa de vários turnos sem mensagem presa e atualização do gateway

## ADDED Requirements

### Requirement: WPP-25 Conversa de vários turnos sem mensagem presa
O sistema SHALL entregar cada resposta do bot e ler cada mensagem do cliente em conversas de vários turnos, também para contato com endereço `@lid`, sem "Aguardando mensagem" no celular e sem `Bad MAC` no log do gateway.

Contrato: nenhum.
Prova: `baileys-gateway/test/` e homologação da tarefa 5.2.

#### Scenario: Segunda resposta do bot chega
- **GIVEN** o número da empresa pareado e um contato de teste que já recebeu a primeira resposta
- **WHEN** o contato escreve de novo e o bot responde
- **THEN** a segunda resposta aparece lida no celular do contato, sem o relógio "Aguardando mensagem"

#### Scenario: Segunda mensagem do cliente é lida
- **GIVEN** o contato de teste que já trocou uma mensagem com o bot
- **WHEN** o contato manda a mensagem seguinte
- **THEN** o gateway a entrega ao Java e o log não traz `Bad MAC` nem `failed to decrypt message`

#### Scenario: Contato com endereço lid mantém o número no webhook
- **GIVEN** uma mensagem de cliente que chega com `remoteJid` terminado em `@lid` e o número em `remoteJidAlt`
- **WHEN** o gateway posta no webhook
- **THEN** `from` leva o número do cliente como hoje e nenhum `@lid` vai ao Java

#### Scenario: Resposta no mesmo endereço da conversa
- **GIVEN** um cliente que escreveu de um endereço `@lid` cujo número o gateway conhece
- **WHEN** o Java manda a resposta para o número desse cliente
- **THEN** o gateway envia a resposta para o mesmo `@lid` da conversa, e não para o endereço de número

#### Scenario: Número sem conversa por lid
- **GIVEN** um número que nunca escreveu de um `@lid`, ou um `@lid` sem número resolvido
- **WHEN** o Java manda a resposta
- **THEN** o gateway envia para o endereço de número, como antes

#### Scenario: Conversa de dez turnos
- **WHEN** o contato de teste e o bot trocam dez mensagens seguidas, nos dois sentidos
- **THEN** todas chegam sem relógio e sem erro de criptografia no log

### Requirement: WPP-26 Atualização do gateway preserva ou encerra a sessão de forma visível
O sistema SHALL, ao atualizar a versão do Baileys do gateway, manter a sessão pareada conectada quando as chaves migrarem, ou, quando não migrarem, mostrar no painel o estado de repareamento (WPP-22) em vez de ficar desconectada sem aviso, sem perder o contato do cliente nem o histórico no banco.

Contrato: nenhum.
Prova: homologação manual da tarefa 5.3 com cópia do volume `baileys-auth`.

#### Scenario: Sessão migra e continua conectada
- **GIVEN** uma sessão pareada no 6.7.24 e o gateway atualizado para o 7.x
- **WHEN** o container sobe
- **THEN** o status volta a `CONNECTED` sem novo pareamento e a próxima mensagem do cliente é respondida

#### Scenario: Sessão não migra
- **GIVEN** uma sessão que o 7.x não consegue ler
- **WHEN** o container sobe
- **THEN** o status fica `DISCONNECTED` com `sessaoEncerrada` verdadeiro e o painel mostra "A conexão foi encerrada. Conecte de novo"

#### Scenario: Volta para a versão anterior
- **GIVEN** o gateway voltado para o 6.7.24 depois de ter rodado no 7.x
- **WHEN** o container sobe
- **THEN** a pasta de sessão antiga restaurada da cópia volta a conectar, ou o painel pede novo pareamento, e o banco continua intacto
