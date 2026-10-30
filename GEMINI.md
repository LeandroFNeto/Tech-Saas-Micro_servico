# Diretrizes do Projeto (Antigravity & Gemini)

Este arquivo define as regras e convenções essenciais do repositório para o Gemini / Antigravity.

## 1. OpenAPI Contract-First
- A Fonte Única de Verdade da API HTTP é `especificacao/back/`, indexada em `especificacao/back/swagger.yaml`.
- O YAML sempre é atualizado **antes** do código Java.
- Se houver divergência entre Java e YAML, o Java deve ser corrigido.
- Detalhes em: [.agents/rules/openapi-contract-first.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/rules/openapi-contract-first.md).

## 2. OpenSpec Workflow
- Mudanças e novas funcionalidades são planejadas primeiro em `openspec/changes/<nome>/` (`proposal.md`, `design.md`, `tasks.md`).
- A implementação só inicia após aprovação explícita.
- Especificações vigentes em `openspec/specs/` e controle em `openspec/central-tarefas.md`.
- Detalhes em: [.agents/rules/openspec-workflow.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/rules/openspec-workflow.md).

## 3. Código Limpo e Sem Comentários
- Sem comentários no código (`//` ou `/* */`), nem em código novo nem em trechos alterados. O comportamento e as decisões pertencem às especificações (`openspec/`), não ao código.
- Comentários antigos só são removidos quando o arquivo for alterado.

## 4. Skills Disponíveis no Workspace
- `/openspec`: Planejar, propor e aplicar mudanças no OpenSpec ([.agents/skills/openspec/SKILL.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/skills/openspec/SKILL.md)).
- `/horadoshow`: Fluxo orquestrado da equipe de agentes ([.agents/skills/horadoshow/SKILL.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/skills/horadoshow/SKILL.md)).
- `/painel`: Gerar e visualizar o painel web da situação do projeto ([.agents/skills/painel/SKILL.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/skills/painel/SKILL.md)).
- `/diasabatico`: Foco visual no front Angular ([.agents/skills/diasabatico/SKILL.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/skills/diasabatico/SKILL.md)).
- `/ponytail`: Modo senior minimalista, auditoria e revisão de código enxuto (`/ponytail`, `/ponytail-review`, `/ponytail-audit`).
- `/kirito`: Teste de caixa branca e blindagem de robustez ([.agents/skills/kirito/SKILL.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/skills/kirito/SKILL.md)).

## 5. Central de Ajuda e Comandos (AJUDA.md)
- O arquivo [AJUDA.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/AJUDA.md) é a referência rápida e central de todos os comandos do Tech-SaaS.
- Deve ser mantido e atualizado sempre que um novo comando ou skill for criado/alterado.
- Detalhes em: [.agents/rules/ajuda-comandos.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/rules/ajuda-comandos.md).
