# Design

## Context

O Tech-SaaS possui módulos desacoplados no backend (Spring Boot) e frontend (Angular), incluindo autenticação, avisos, empresas, WhatsApp e IA. Conforme descrito em `proposal.md`, é necessário um laboratório de testes interno (módulo de uso próprio) para aferir a saúde do sistema e experimentar a assistência com IA orientada a avisos e tutoriais. Foi criado o documento `docs/base-conhecimento-gamb.md` como especificação viva do sistema, contendo todas as histórias de usuário (US01 a US20), requisitos funcionais/não-funcionais e instruções de configuração para servir de contexto perene à IA.

## Goals / Non-Goals

**Goals:**
- Prover botão de diagnóstico diretamente na **Tela de Admin** do frontend Angular para disparar checagens de integridade em todos os módulos.
- Prover endpoint REST administrativo `/admin/assistente-tutoria/diagnostico` que realiza checagens não destrutivas em cada subsistema (Auth, Avisos, Agenda, Empresa, WhatsApp, IA).
- Prover endpoint REST `/admin/assistente-tutoria/chat` que orquestra a IA com injeção de contexto de `docs/base-conhecimento-gamb.md` e dos avisos ativos para esclarecer dúvidas e guiar configurações.
- Disponibilizar interface Angular administrativa com comutador de modo (Diagnóstico vs. Atendimento Sandbox) permitindo testar a saúde geral ou interagir com o assistente.
- Seguir estritamente o princípio OpenAPI Contract-First (`especificacao/back/`).

**Non-Goals:**
- Não criar infraestrutura pesada de banco vetorial (RAG com pgvector/Pinecone); o contexto de tutoria e avisos é injetado diretamente no prompt respeitando a janela do Gemini Flash.
- Não alterar as rotas públicas de WhatsApp nem o fluxo produtivo de agendamento de clientes.
- Não expor o laboratório ou qualquer rota de diagnóstico a usuários não autenticados como administradores.

## Decisions

### 1. Contrato OpenAPI First
- **Decisão**: Criar a especificação do endpoint em `especificacao/back/paths/admin-assistente-tutoria.yaml` e schemas em `especificacao/back/schemas/assistente/` antes do código Java.
- **Alternativas consideradas**: Criar direto no Controller Java. Rejeitado por violar as diretrizes do repositório (`openapi-contract-first.md`).

### 2. Base de Conhecimento Estruturada (`docs/base-conhecimento-gamb.md`)
- **Decisão**: Centralizar a documentação de regras de negócio, histórias de usuário (US01 a US20), arquitetura e guias de configuração em `docs/base-conhecimento-gamb.md`. O backend carrega esse recurso junto aos avisos ativos para alimentar as instruções do assistente IA.
- **Alternativas consideradas**: Fragmentar o contexto em múltiplos arquivos soltos ou hardcoded no código Java. Rejeitado para permitir expansão contínua e manutenção centralizada sem recompilação de código.

### 3. Diagnóstico Resiliente na Tela de Admin
- **Decisão**: O botão de diagnóstico é posicionado na tela de administração. A checagem de cada módulo (banco, auth, avisos, ia, whatsapp) executa testes leves com timeout máximo de 2 segundos por módulo para resposta rápida do diagnóstico, apresentando status visual por meio de cards coloridos.
- **Alternativas consideradas**: Acoplar ao Spring Boot Actuator `/health`. Rejeitado por não cobrir particularidades dos fluxos funcionais do SaaS (ex.: sessão WhatsApp e cota IA).

### 4. Controle Comutador no Frontend
- **Decisão**: Implementar no componente Angular um interruptor (toggle button) que altera a visualização entre o painel de status/diagnóstico de módulos e o chat de teste/sandbox de atendimento ao cliente.
- **Alternativas consideradas**: Telas separadas em rotas distintas. Rejeitado para atender ao requisito do usuário de ter um botão no mesmo módulo de teste para alternar rapidamente entre conferir se tudo funciona e testar o atendimento.

## Risks / Trade-offs

- **[Cota da API Gemini]** → Testes contínuos no laboratório podem consumir a cota gratuita.
  *Mitigação*: Cache de respostas de tutoriais comuns e fallback determinístico para mensagens de tutoria offline se o modelo retornar 429.
- **[Lentidão na checagem externa do WhatsApp]** → A API externa do WhatsApp pode atrasar a resposta do diagnóstico.
  *Mitigação*: Timeout rígido de 2s com captura de exceção e retorno de status `DEGRADADO` ou `FALHA` sem travar os outros módulos.
