# Proposal

## Why

A Gamb quer um número de WhatsApp próprio para tirar dúvidas dos clientes e um ambiente para testar módulos novos antes de levá-los às empresas. Hoje o bot de teste (modo `TESTE_VIRTUAL`) só existe dentro do chat do painel, e a change `ajustes-visuais-chat-flutuante-tutoriais` tira o Simulador desse chat. Esta change dá aos dois bots o seu lugar no WhatsApp, reaproveitando o escopo do Suporte definido na outra change.

## What Changes

- **Bot de Suporte no WhatsApp da Gamb**: uma sessão de WhatsApp da Gamb responde às dúvidas dos clientes com o mesmo bot de Suporte do painel: só base de conhecimento e avisos, nenhum dado de empresa, nenhuma ação no sistema. Assunto fora do app recebe a recusa padrão.
- **Bot de teste de módulos**: uma segunda sessão, de uso interno, roda os módulos novos e experimentais em conversa de WhatsApp real. Só números autorizados pelo admin conversam com ele.
- **Registro de módulos experimentais**: cada módulo novo se registra com nome e resposta, e o bot de teste lista os módulos e executa um por comando. Não há grande volume de dados, então o registro fica em memória e a lista vem do código.
- **Simulador do painel**: o modo `TESTE_VIRTUAL` do endpoint do cliente permanece no backend, e a lógica dele vira o primeiro módulo experimental do bot de teste.
- **Roteamento**: o webhook passa a reconhecer as duas sessões da Gamb e entregá-las às estratégias de Suporte e de Teste, sem tocar nas estratégias das empresas.

## Capabilities

### New Capabilities

### Modified Capabilities
- `whatsapp`: duas sessões da Gamb, uma de Suporte e uma de Teste de módulos, roteadas por estratégia própria.
- `assistente-tutoria`: a sessão virtual de testes passa a ser também um bot de WhatsApp de uso interno, com módulos experimentais registrados.

## Impact

- Backend: `ControllerWhatsapp` e `FactoryModulo` (duas estratégias novas), `AssistenteTutoriaService` (Suporte reaproveitado, teste virtual reaproveitado), novo registro de módulos experimentais.
- Contratos: `especificacao/back/paths/webhook-whatsapp.yaml` e `openspec/specs/whatsapp/spec.md`.
- Operação: cadastrar as duas sessões da Gamb no WPPConnect e a lista de números autorizados do bot de teste.
- Depende de `ajustes-visuais-chat-flutuante-tutoriais`: o escopo do Suporte e o prompt sem dado de empresa vêm de lá.
