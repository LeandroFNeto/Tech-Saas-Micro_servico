# Proposal

## Why

A Gamb quer um número de WhatsApp próprio onde todo contato é tratado como cliente: ele tira dúvidas do app ou marca uma reunião com a equipe, para um ajuste ou para uma nova inovação. A Gamb também precisa de um ambiente para testar módulos novos antes de levá-los às empresas. Hoje o bot de teste (modo `TESTE_VIRTUAL`) só existe dentro do chat do painel, e a change `ajustes-visuais-chat-flutuante-tutoriais` tira o Simulador desse chat. Esta change dá aos dois bots o seu lugar no WhatsApp, reaproveitando o escopo do Suporte definido na outra change.

## What Changes

- **Atendente da Gamb no WhatsApp**: uma sessão de WhatsApp da Gamb atende todo contato como cliente. Ela tira dúvidas com o mesmo bot de Suporte do painel (só base de conhecimento e avisos, nenhum dado de empresa) ou marca reunião com a equipe. Assunto fora do app recebe a recusa padrão.
- **Reunião com a equipe**: o cliente informa o motivo (ajuste ou nova inovação), a data e o horário. O sistema cria o evento na Google Agenda da Gamb e convida por e-mail os responsáveis cadastrados.
- **Responsáveis da reunião**: um campo de e-mails que o admin edita no painel define quem é convidado em cada reunião. A lista pode mudar a qualquer momento.
- **Bot de teste de módulos**: uma segunda sessão, de uso interno, roda os módulos novos e experimentais em conversa de WhatsApp real. Só números autorizados pelo admin conversam com ele.
- **Registro de módulos experimentais**: cada módulo novo se registra com nome e resposta, e o bot de teste lista os módulos e executa um por comando. O registro fica em memória e a lista vem do código.
- **Simulador do painel**: o modo `TESTE_VIRTUAL` do endpoint do cliente permanece no backend, e a lógica dele vira o primeiro módulo experimental do bot de teste.
- **Roteamento**: o webhook passa a reconhecer as duas sessões da Gamb e entregá-las às estratégias da Atendente e de Teste, sem tocar nas estratégias das empresas.
- **Limite de uso**: o número é público, então cada número de cliente tem um limite diário de mensagens.

## Capabilities

### New Capabilities

### Modified Capabilities
- `whatsapp`: duas sessões da Gamb, a da atendente (dúvidas e reunião) e a de teste de módulos, roteadas por estratégia própria.
- `assistente-tutoria`: a sessão virtual de testes passa a ser também um bot de WhatsApp de uso interno, com módulos experimentais registrados.

## Impact

- Backend: `ControllerWhatsapp` e `FactoryModulo` (duas estratégias novas), `AssistenteTutoriaService` (Suporte reaproveitado, teste virtual reaproveitado), `ServicoReserva` e Google Agenda (evento com convidados), novo registro de módulos experimentais, campo de e-mails dos responsáveis na empresa interna da Gamb.
- Painel admin: edição da lista de e-mails dos responsáveis.
- Contratos: `especificacao/back/paths/webhook-whatsapp.yaml`, o contrato de edição da empresa interna e `openspec/specs/whatsapp/spec.md`.
- Operação: cadastrar as duas sessões da Gamb no WPPConnect, conectar a Google Agenda da Gamb e cadastrar a lista de números autorizados do bot de teste.
- Depende de `ajustes-visuais-chat-flutuante-tutoriais`: o escopo do Suporte e o prompt sem dado de empresa vêm de lá.
