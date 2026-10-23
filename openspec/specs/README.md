# Contratos de comportamento

Esta pasta é a fonte da verdade do OpenSpec. Cada domínio fica numa subpasta com o contrato consolidado do que o sistema faz: entradas, saídas e cenários observáveis.

Uma feature nova não começa aqui e não gera código direto. Ela passa por `openspec/changes/`, nesta ordem:

1. `proposal.md` — por que a mudança existe e o que muda.
2. `design.md` — como implementar.
3. `tasks.md` — a lista do que codar e como verificar.

Só depois de aplicada a mudança o contrato entra em `specs/`. O histórico da mudança vai para `openspec/archive/`.
