# Tasks

## 1. Contrato OpenAPI First

- [x] 1.1 Definir a especificação OpenAPI dos endpoints `/admin/assistente-tutoria/diagnostico` e `/admin/assistente-tutoria/chat` em `especificacao/back/paths/admin-assistente-tutoria.yaml`, indexar em `especificacao/back/swagger.yaml` e declarar os schemas em `especificacao/back/schemas/assistente/`; verificar conformidade executando script de validação do OpenAPI.

## 2. Backend - Diagnóstico e Assistente de Tutoria

- [x] 2.1 Implementar `DiagnosticoModulosService` para checagem de saúde de cada subsistema (Auth, Avisos, Agenda, Empresa, WhatsApp, IA) com timeout de 2 segundos; verificar aprovação dos cenários em `DiagnosticoModulosServiceTest`.
- [x] 2.2 Implementar leitor e injetor de contexto baseado em `docs/base-conhecimento-gamb.md` e avisos ativos no `AssistenteTutoriaService`, com integração ao Gemini Flash e fallback offline resiliente; verificar aprovação dos cenários em `AssistenteTutoriaServiceTest`.
- [x] 2.3 Implementar `AdminAssistenteTutoriaController` mapeando os endpoints com validação de `x-admin-token`; verificar aprovação dos testes de integração `AdminAssistenteTutoriaControllerTest` com retornos 200 e 401.

## 3. Frontend - Botão de Diagnóstico na Tela de Admin e Comutador

- [x] 3.1 Integrar o botão de diagnóstico na **Tela de Admin** do Angular com comutador de modo (Modo Diagnóstico vs Modo Atendimento Sandbox) e visualizador de saúde dos módulos em cards coloridos; verificar alternância de modos e renderização com testes Jasmine.
- [x] 3.2 Implementar o serviço Angular `AssistenteTutoriaService` e a caixa de diálogo de chat com a IA contextualizada na base de conhecimento e avisos; verificar fluxo de envio/resposta e execução do diagnóstico com testes unitários no frontend.

## 4. Integração e Validação

- [x] 4.1 Executar a suíte de testes de regressão do backend (JUnit) e frontend (Karma/Jasmine) garantindo que nenhum contrato ou comportamento existente foi quebrado.
