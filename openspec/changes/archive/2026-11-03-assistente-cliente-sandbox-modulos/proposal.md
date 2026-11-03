# Proposal

## Why

Atualmente, o assistente de tutoria e diagnóstico está restrito à tela do administrador. Os clientes (proprietários de estabelecimentos) necessitam de suporte e aprendizado contínuo no próprio painel para esclarecer dúvidas sobre configuração de horários, regras de locação, avisos e conexão do WhatsApp. Além disso, o cliente precisa de uma tela dedicada de tutoriais na sua navegação para consultar guias práticos passo a passo, e a equipe precisa de uma sessão virtual permanente de teste ("contato de assistente") para experimentar novas funções sem escanear QR Code e criar sessões físicas de WhatsApp a cada teste.

## What Changes

- Criação de uma **Tela Separada de Tutoriais** na área do cliente (`/cliente/tutoriais`), adicionando o item correspondente no menu de navegação da barra lateral e abas do cliente no `ShellComponent`.
- A tela de tutoriais organiza guias ilustrados passo a passo (conexão de WhatsApp, configuração de agenda/locação, fotos/regras, simulação de atendimento) combinando comunicados de tutorial ativos do sistema com o documento de conhecimento oficial (`docs/base-conhecimento-gamb.md`).
- Disponibilização do widget / chat do Assistente de Dúvidas e Tutoria no painel do cliente, com atalhos para tirar dúvidas sobre cada tutorial.
- Criação de endpoint para suporte ao cliente (`/api/cliente/assistente/chat`) que responde dúvidas operacionais com base no cadastro da própria empresa, avisos ativos e tutoriais.
- Implementação de canal de "Sessão Virtual de Testes" no assistente, permitindo simular o envio de comandos e mensagens de clientes finais e testar módulos existentes ou experimentais (ex.: agendamento, consulta de regras, novos módulos de produtos) sem dependência de pareamento físico via QR Code no WPPConnect.
- Isolamento multi-tenant garantido: o cliente visualiza apenas orientações e dados pertinentes ao seu estabelecimento.

## Capabilities

### New Capabilities

*(Nenhuma nova capability criada - expansão da capability existente `assistente-tutoria`)*

### Modified Capabilities

- `assistente-tutoria`: Inclusão dos requisitos de tela de tutoriais no painel do cliente, assistência inteligente ao cliente e sessão virtual de testes para experimentação de módulos sem dependência de conexões físicas de WhatsApp.

## Impact

- **Frontend (Angular)**:
  - Nova rota e componente `/cliente/tutoriais` (`TutoriaisComponent`).
  - Atualização do menu do cliente no `ShellComponent` (desktop e mobile).
  - Componente/widget conversacional de suporte e sessão virtual.
- **Backend (Spring Boot)**: Novo endpoint seguro `/api/cliente/assistente/chat`, integrando a empresa do usuário à injeção de contexto no `AssistenteTutoriaService`.
- **Contrato OpenAPI**: Especificação de `especificacao/back/paths/cliente-assistente-chat.yaml` indexado no `swagger.yaml`.
