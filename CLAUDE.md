# Instruções para o Claude neste repositório

- Comportamento do sistema: `openspec/specs/README.md` e o `spec.md` do domínio. Feature nova passa por `openspec/changes/<nome>/` antes de virar código.
- Equipe de agentes, ordem e modelos (colunas Cursor e Claude): `.cursor/rules/papeis-ia.mdc` e `.cursor/skills/horadoshow/SKILL.md`. No Claude, use sempre a coluna Claude.
- Skill nova que escolhe modelo nasce com as duas colunas (Cursor e Claude). Ver "Skill nova" em `.cursor/rules/papeis-ia.mdc`.
- Regras de trabalho (sem comentários no código, explicação no modelo mais barato): seção "Regras de trabalho" em `.cursor/rules/papeis-ia.mdc`. No Claude, explicação vai para um subagente `Agent` com `model: haiku`.
- Para achar arquivo, símbolo ou dependência, use sempre o Graphify (`graphify query`, `path`, `explain`) antes de Glob, Grep ou Read, e rode `graphify update .` no fim do turno quando criar ou alterar arquivo. Todo subagente recebe essa regra no prompt. Ver `.cursor/rules/graphify.mdc` e a seção "Regras de trabalho" de `.cursor/rules/papeis-ia.mdc`.
- Proposta, change e spec: `.cursor/skills/openspec/SKILL.md`.
- Situação do projeto (etapa, o que falta, paleta, fontes) em página web: `.cursor/skills/painel/SKILL.md`.
- As skills `horadoshow`, `diasabatico`, `openspec` e `painel` existem em `.claude/skills/` só como ponte para `.cursor/skills/`. Mudança de conteúdo vai no arquivo do Cursor.
- Tela, layout e estilo do front: `.cursor/skills/diasabatico/SKILL.md`.
