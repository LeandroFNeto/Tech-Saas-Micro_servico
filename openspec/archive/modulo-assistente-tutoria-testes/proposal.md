# Proposal

## Why

Desenvolvedores e administradores do Tech-SaaS necessitam de um ambiente controlado (sandbox de uso próprio) para testar a integração da IA com a base de conhecimento do sistema (avisos, tutoriais e parâmetros de configuração). Atualmente, não há uma interface dedicada para validar se todos os módulos do sistema estão operacionais ou para experimentar fluxos de tutoria e atendimento assistido sem expor clientes em produção nem depender de soluções improvisadas em canais reais.

## What Changes

- Inclusão do **Botão de Diagnóstico de Módulos** diretamente na **Tela de Admin** do painel web para acionamento de testes de integridade de todos os subsistemas em lote.
- Criação de um módulo e interface de uso próprio (Laboratório / Sandbox de Testes do Admin) restrito a administradores e desenvolvedores.
- Integração da IA com o documento introdutório e base de conhecimento viva (`docs/base-conhecimento-gamb.md`) e com o acervo de avisos ativos para esclarecer dúvidas operacionais, explicar funções do sistema e orientar configurações (ex.: conexão do WhatsApp, modalidades de locação, dados cadastrais).
- Inclusão de um controle comutador (botão / seletor de modo) no painel de administração com dois modos de operação:
  - **Modo Diagnóstico (Testes de Módulos)**: dispara checagens de integridade em lote para verificar se todos os módulos essenciais (Auth, Avisos, Agenda, Empresa, WhatsApp, IA) estão funcionando e exibe o relatório de status com indicadores visuais.
  - **Modo Atendimento Assistido (Sandbox do Cliente)**: simula conversas de suporte e tutoria da IA para validação prévia de atendimento e dúvidas antes da ativação aos clientes.
- Exposição de endpoints seguros de diagnóstico de módulos e de chat assistido restritos ao perfil administrativo.

## Capabilities

### New Capabilities

- `assistente-tutoria`: Módulo de uso interno e testes para diagnóstico de integridade dos módulos e assistência inteligente contextualizada em avisos e tutoriais de configuração da plataforma.

### Modified Capabilities

- Nenhuma capability existente tem seus requisitos alterados. O módulo consome os serviços existentes em modo de leitura e diagnóstico sem quebrar as regras de `ia`, `avisos` ou `empresa`.

## Impact

- **Documentação de Contexto Vivo**: Criação e contínua expansão de `docs/base-conhecimento-gamb.md` como base de referência da IA para regras de negócio (US01 a US20, RFs e RNFs) e tutoriais de configuração.
- **Frontend (Angular)**: Inclusão do botão de diagnóstico na tela de admin, painel de status com indicadores visuais e componente de chat com a IA para esclarecer dúvidas e simular atendimentos.
- **Backend (Spring Boot)**: Novo controller/serviço administrativo para orquestrar diagnóstico dos subsistemas e prover o chat com IA alimentado pela base de conhecimento e avisos.
- **Contratos de API (OpenAPI Contract-First)**: Especificação dos endpoints em `especificacao/back/paths/admin-assistente-tutoria.yaml` e schemas correspondentes em `especificacao/back/schemas/assistente/`.
- **Dependências & Segurança**: Exigência de permissão administrativa (`x-admin-token` ou Bearer com perfil ADMIN) para impedir acesso público ao ambiente de testes.
