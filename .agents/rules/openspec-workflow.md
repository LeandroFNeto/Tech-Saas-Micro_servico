# OpenSpec Workflow

O comportamento vigente está em `openspec/specs/`. O procedimento de propor, aplicar e documentar está em `.agents/skills/openspec/SKILL.md`.

## Gate de feature e tela

Quando o pedido for uma funcionalidade nova ou uma tela nova, não escreva código-fonte, YAML de contrato nem componente na primeira resposta.

1. Leia o `spec.md` do domínio em `openspec/specs/`.
2. Crie `openspec/changes/<nome-da-feature>/` com `proposal.md`, `design.md` e `tasks.md`, no formato da skill.
3. Pare e peça revisão desses três arquivos.
4. Codifique somente depois de aprovação explícita do usuário sobre a pasta `changes/`.
5. O código segue `openspec/specs/` (stack, OpenAPI contract-first e fronteiras já documentadas). Ao terminar, o requisito entra no `spec.md` do domínio e a pasta vai para `openspec/archive/`.

Correção que só restaura um cenário já escrito no spec, ajuste de bug e pergunta não passam por esse gate.
