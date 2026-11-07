# Spec Delta

## ADDED Requirements

### Requirement: WPP-16 Trocar o provedor de WhatsApp sem mudar o contrato
O sistema SHALL manter o mesmo contrato de webhook (`WhatsappWebhookDTO`) e os mesmos valores de status de sessão (`CONNECTED`, `QRCODE`, `DISCONNECTED`) quando o provedor de WhatsApp é trocado, de modo que roteamento, strategies e painel não percebam a troca.

#### Scenario: mensagem de chat após a troca
- **WHEN** o novo provedor entrega uma mensagem de chat 1:1 de um cliente
- **THEN** o webhook recebe o mesmo formato de payload de antes
- **AND** a mensagem é roteada pela strategy do ramo da empresa como já era

#### Scenario: status consultado pelo painel
- **WHEN** o painel consulta o status de uma sessão
- **THEN** o corpo traz sessão, status e QR quando houver, nos mesmos campos de antes

#### Scenario: envio de texto e de imagem
- **WHEN** o sistema envia uma resposta de texto ou imagem a um cliente
- **THEN** o provedor entrega a mensagem pela mesma chamada de envio que o backend já usa

### Requirement: WPP-17 Parear a sessão por QR na virada de provedor
O sistema SHALL tratar a sessão sem credencial válida no novo provedor como não pareada, devolver o status `QRCODE` com o QR para o painel e passar a `CONNECTED` assim que o QR for lido.

#### Scenario: sessão ainda não pareada após a virada
- **WHEN** o painel consulta ou inicia uma sessão que nunca foi pareada no novo provedor
- **THEN** a API responde 200 com status `QRCODE` e o QR
- **AND** nenhuma mensagem de cliente é entregue ao bot enquanto a sessão não estiver conectada

#### Scenario: QR lido
- **WHEN** o responsável lê o QR no WhatsApp
- **THEN** o status da sessão passa a `CONNECTED`

### Requirement: WPP-18 Restaurar sessões pareadas após reinício
O sistema SHALL reconectar, sem novo QR, as sessões já pareadas quando o serviço de WhatsApp ou o servidor reinicia, desde que as credenciais salvas continuem válidas.

#### Scenario: reinício do serviço de WhatsApp
- **GIVEN** uma sessão pareada e conectada
- **WHEN** o serviço de WhatsApp reinicia
- **THEN** a sessão volta a `CONNECTED` sem pedir QR

#### Scenario: credencial inválida
- **WHEN** a credencial salva de uma sessão deixa de ser aceita pelo WhatsApp
- **THEN** o status passa a `QRCODE` ou `DISCONNECTED`
- **AND** o painel mostra que é preciso parear de novo

### Requirement: WPP-19 Resolver contato em identificador alternativo
O sistema SHALL entregar ao webhook o número real do remetente quando o provedor identifica o contato por identificador alternativo (`@lid`) e o número é conhecido; quando o número não puder ser resolvido, o sistema MUST descartar a mensagem sem erro e sem enviar resposta.

#### Scenario: contato `@lid` com número conhecido
- **WHEN** chega mensagem de contato em identificador alternativo cujo número é conhecido
- **THEN** o webhook recebe o remetente no formato de número de sempre
- **AND** o atendimento segue normalmente

#### Scenario: contato `@lid` sem número conhecido
- **WHEN** chega mensagem de contato em identificador alternativo cujo número não pode ser resolvido
- **THEN** a mensagem é descartada e a API responde 200
- **AND** o log registra o descarte sem número completo nem texto da mensagem
