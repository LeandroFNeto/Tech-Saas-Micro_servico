# Papéis da equipe de IA

O procedimento está em `.agents/skills/horadoshow/SKILL.md`. A lista de skills do assunto está em `.agents/skills/horadoshow/acopladas.md`. O quadro está em `openspec/central-tarefas.md`.

## Ordem

Pesquisa e proposta acontecem antes do chamado. `horadoshow <nome>` abre `openspec/changes/<nome>/` e encaminha, nesta ordem: programação, testes, qualidade, segurança, revisão, deploy.

A programação só começa com `aprovado` gravado pela pessoa na linha da central.

## Tabela de Modelos e Equivalências

Quando a tarefa for executada no Antigravity, utilize a coluna do seu ambiente atual (ou os equivalentes disponíveis no seletor de modelos da IDE):

| Etapa | Modelo na IDE / Usuário | Subagente Antigravity | Cursor | Claude |
|---|---|---|---|---|
| Pesquisa | `Gemini 3.8 Flash Medium` | `flash` | `cursor-grok-4.6-low` | `claude-haiku-5-5` |
| Proposta | `Claude Sonnet 5.5 Medium` / `Gemini 4 Argon` | `inherit` / `pro` | `grok-4.7-high-fast` | `claude-sonnet-5-5` |
| Programação | `Claude Sonnet 5.5 Medium` | `inherit` / `pro` | `claude-sonnet-5-5-high` | `claude-sonnet-5-5` |
| Testes | `Gemini 4 Argon Medium` / `Gemini 3.8 Flash` | `flash` / `inherit` | `gemini-4-argon-medium` | `claude-sonnet-5-5` |
| Qualidade | `Claude Sonnet 5.5 Medium` / `GPT-OSS 120B` | `inherit` / `pro` | `gpt-5.6-sol-medium` | `claude-sonnet-5-5` |
| Segurança | `Claude Opus 5.5 Medium` / `Claude Sonnet 5.5` | `pro` / `inherit` | `claude-opus-5-5-medium` | `claude-opus-5-5` |
| Revisão e spec definitiva | `Claude Opus 5.5 Medium` / `Claude Sonnet 5.5` | `pro` / `inherit` | `claude-opus-5-5-medium` | `claude-opus-5-5` |
| Deploy | `Gemini 3.8 Flash Medium` | `flash_lite` / `flash` | `cursor-grok-4.6-low` | `claude-haiku-5-5` |
| Explicação | `Gemini 3.8 Flash Medium` | `flash` | `cursor-grok-4.6-low` | `claude-haiku-5-5` |

Caso algum modelo não esteja disponível diretamente na conta/sessão, utiliza-se automaticamente o equivalente mais próximo da tabela (por exemplo, `Claude Sonnet 5.5 Medium` para raciocínio/código complexo e `Gemini 3.8 Flash Medium` para tarefas mecânicas/leitura).

## Regras de trabalho

- **Ponytail ativo desde o início**: Sempre que a IA iniciar qualquer etapa, aplica de imediato o modo Ponytail (lazy senior dev). O melhor código é o que nunca foi escrito: resolver com o menor diff possível, sem abstrações ou arquivos não solicitados, priorizando stdlib e padrões já existentes no projeto.
- **Sem comentários no código**. O comportamento está na spec (`openspec/`), não no código. Vale para código novo e para trechos alterados; comentário existente só sai quando o arquivo for mexido.
- **Explicação para a pessoa**: concisa e direta.
- **Pesquisa no código pelo Graphify**. Para achar arquivo, símbolo ou dependência, use `graphify query "<pergunta>"`, `graphify path "<A>" "<B>"` ou `graphify explain "<conceito>"` antes de busca cega. Depois de criar ou alterar arquivo, rode `graphify update .` uma vez no fim do turno ou da etapa.
- **Parada**: No máximo 2 voltas. A terceira reprovação para e chama a pessoa. Tela sem rota, autenticação ou dado não chama o subagente de segurança. A revisão grava a spec definitiva. O deploy só arquiva e sobe: front em `producao` (Vercel), código completo em `main` (Hostinger).
