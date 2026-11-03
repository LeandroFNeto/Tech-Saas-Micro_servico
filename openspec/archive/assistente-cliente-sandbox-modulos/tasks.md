# Tasks

## 1. Contrato OpenAPI First

- [x] 1.1 Definir o endpoint `/api/cliente/assistente/chat` em `especificacao/back/paths/cliente-assistente-chat.yaml`, indexar em `especificacao/back/swagger.yaml` e reaproveitar os schemas do assistente; validar sintaxe com script de validação de OpenAPI.

## 2. Backend - Suporte ao Cliente e Sessão Virtual de Testes

- [x] 2.1 Estender `AssistenteTutoriaService` com o método `responderCliente`, garantindo injeção de dados da empresa logada e modo `TESTE_VIRTUAL` para simular os módulos e comandos sem requerer sessão física de WhatsApp; verificar com testes unitários em `AssistenteTutoriaServiceTest`.
- [x] 2.2 Criar endpoint `POST /api/cliente/assistente/chat` no controller de automação do cliente ou controller dedicado com autenticação de usuário da empresa; verificar aprovação dos testes de integração em `ClienteAssistenteChatTest`.

## 3. Frontend - Tela Separada de Tutoriais e Navegação

- [x] 3.1 Criar o componente `TutoriaisComponent` em `frontend/src/app/features/client/tutoriais/` com guias ilustrados passo a passo de configuração e integração de avisos/tutoriais do sistema; verificar renderização e comportamento com testes Jasmine em `tutoriais.component.spec.ts`.
- [x] 3.2 Atualizar as rotas do cliente em `client.routes.ts` e adicionar a navegação de "Tutoriais" na barra lateral e abas móveis do `ShellComponent`; verificar conformidade dos links de menu com testes Jasmine em `shell.component.spec.ts`.

## 4. Frontend - Assistente e Simulador na Área do Cliente

- [x] 4.1 Integrar componente/widget de chat no painel do cliente permitindo alternar entre modo de suporte operacional e sessão virtual de testes de novos módulos; verificar comportamento com testes unitários Jasmine.

## 5. Verificação e Regressão

- [x] 5.1 Executar a suíte completa de testes do backend e frontend e validação de código limpo sem comentários.
