# Spec Delta

## ADDED Requirements

### Requirement: WPP-21 Log do gateway atual sem dado pessoal
O gateway WPPConnect em uso SHALL registrar no nível `info`, sem número completo, texto de mensagem nem corpo do webhook, também quando o envio ou o webhook falha.

Prova: testes em `wppconnect-server/src/tests`, conferência do log do container

#### Scenario: Envio para um número
- **WHEN** o backend pede um envio ao gateway
- **THEN** o log do gateway SHALL não trazer o número de destino

#### Scenario: Falha no webhook
- **WHEN** o gateway não consegue postar no webhook do backend
- **THEN** o log SHALL trazer só o código HTTP ou o tipo da falha, sem o corpo da mensagem
