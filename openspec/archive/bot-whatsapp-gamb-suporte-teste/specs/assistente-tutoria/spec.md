# Spec Delta

## MODIFIED Requirements

### Requirement: Sessão virtual de testes para simulação de novos módulos
O sistema SHALL fornecer um canal de sessão virtual que permite ao desenvolvedor simular conversas de clientes finais e testar módulos existentes ou novas funções em homologação, sem exigir pareamento ou criação de sessões reais de WhatsApp de empresas no WPPConnect. O canal SHALL ser o modo de teste do endpoint autenticado do cliente e o bot de teste de módulos da sessão de teste da Gamb no WhatsApp.

Contrato: `especificacao/back/paths/cliente-assistente-chat.yaml`
Prova: `ClienteAssistenteChatTest`, `AssistenteTutoriaServiceTest`, `TesteModulosStrategyTest`

#### Scenario: Teste simulado de agendamento em sessão virtual
- **GIVEN** que o usuário ativa a simulação de teste no endpoint do cliente
- **WHEN** envia uma mensagem simulada de cliente final solicitando consulta de agenda
- **THEN** o assistente simula o comportamento do bot respondendo com o fluxo do módulo de agendamento da empresa sem disparar mensagens em WhatsApp físico

#### Scenario: Teste de nova função experimental
- **GIVEN** um novo módulo ou funcionalidade em teste registrado no sistema
- **WHEN** o desenvolvedor envia o comando do módulo na sessão de teste da Gamb
- **THEN** o bot processa a regra do módulo correspondente e devolve a resposta simulada sem requerer QR Code de empresa
