# Design

## Context

Conforme estabelecido em `proposal.md`, o Tech-SaaS agora expandirá as capacidades do assistente de tutoria para os clientes logados no painel web (`/cliente`), adicionará uma tela dedicada de tutoriais na navegação do cliente e proverá uma sessão virtual permanente de testes. Atualmente, para testar fluxos conversacionais ou novos módulos, desenvolvedores e proprietários precisavam escanear QR Code e manter uma sessão física do WhatsApp conectada. Com a tela de tutoriais e a sessão virtual de testes, o cliente aprende a operar a plataforma e experimenta o atendimento dos módulos em memória, sem custos adicionais de infraestrutura ou hardware.

## Goals / Non-Goals

**Goals:**
- Criar a rota e o componente Angular `/cliente/tutoriais` (`TutoriaisComponent`) no módulo do cliente com guias visuais e integração com `AvisoService.listarAtivos()` (filtrando por `TUTORIAL`).
- Adicionar o link de navegação "Tutoriais" na barra lateral e na barra inferior móvel do cliente no `ShellComponent`.
- Prover endpoint REST `/api/cliente/assistente/chat` acessível por clientes autenticados via JWT Bearer.
- Reutilizar e estender o `AssistenteTutoriaService` para responder dúvidas operacionais contextualizadas estritamente nos dados da empresa do usuário logado.
- Prover suporte ao modo `TESTE_VIRTUAL`: o assistente processa a mensagem contra as estratégias de atendimento cadastradas (`GerenciadorSessao`, `FactoryModulo`, módulos ativos da empresa) devolvendo o texto de resposta que o cliente final receberia no WhatsApp, eliminando a necessidade de criar sessões reais no WPPConnect para testes.
- Integrar widget de chat responsivo e acessível no painel do cliente (`/cliente`).
- Seguir OpenAPI Contract-First (`especificacao/back/paths/cliente-assistente-chat.yaml`).

**Non-Goals:**
- Não criar instâncias ou sessões no WPPConnect para a sessão virtual de testes; ela é executada totalmente em memória.
- Não misturar contextos entre estabelecimentos (isolamento multi-tenant estrito).

## Decisions

### 1. Injeção de Contexto Específica da Empresa Autenticada
- **Decisão**: A empresa é obtida a partir do `SecurityContextHolder` / `ServicoAcessoEmpresa`. O prompt do Gemini é enriquecido com os dados específicos da empresa (nome, regras, horários, preços) e com os tutoriais gerais do sistema.
- **Alternativas consideradas**: Permitir passar id de empresa na requisição. Rejeitado por abrir brecha de segurança multi-tenant.

### 2. Tela Dedicada de Tutoriais no Frontend do Cliente
- **Decisão**: Criar componente `/cliente/tutoriais` na aba do cliente com cards explicativos passo a passo (QR Code, Horários, Google Agenda, Fotos, Simulação) e listagem dos tutoriais cadastrados no painel administrativo via `AvisoService`.
- **Alternativas consideradas**: Deixar tutoriais apenas dentro do modal de chat. Rejeitado pois o usuário solicitou expressamente uma tela separada na aba do cliente para fácil leitura e consulta.

### 3. Roteamento em Memória na Sessão Virtual de Testes
- **Decisão**: Quando o modo `TESTE_VIRTUAL` estiver selecionado, a mensagem recebida é submetida ao fluxo de regras conversacionais em memória, simulando o webhook do WhatsApp e retornando a resposta exata dos módulos ativos.
- **Alternativas consideradas**: Criar uma sessão mock no WPPConnect. Rejeitado pelo consumo de recursos e dependência de serviços externos.

## Risks / Trade-offs

- **[Confusão entre modo Suporte e modo Teste Virtual]** → O usuário pode não saber se está falando com o suporte da plataforma ou testando seu próprio bot.
  *Mitigação*: Interface com identificadores visuais claros e abas distintas ("Ajuda & Suporte" vs "Simular Atendimento do Meu Bot").
