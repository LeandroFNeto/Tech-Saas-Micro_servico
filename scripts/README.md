# Scripts de Automação Local (Economia de Tokens)

Este diretório reúne scripts utilitários executáveis para que tarefas repetitivas, mecânicas ou de validação sejam resolvidas diretamente pelo computador com **custo zero de tokens de LLM**.

Diretriz: **Scripts-First** ([.agents/rules/economia-tokens-scripts.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/.agents/rules/economia-tokens-scripts.md)).

---

## Catálogo de Scripts

| Script | Finalidade | Como Executar |
|---|---|---|
| `verificar-codigo-limpo.ps1` | Valida se há comentários proibidos (`//` ou `/* */`) em arquivos `.java`, `.ts`, `.html` e `.css`. Por padrão inspeciona apenas arquivos alterados/novos do git. | `powershell -ExecutionPolicy Bypass -File scripts/verificar-codigo-limpo.ps1` (ou com `-Todos`) |
| `auditar-kirito.ps1` | Auditoria estática de caixa branca: detecta métodos `@Test` vazios, métodos de teste Java sem `@Test`, unidades Angular sem `.spec.ts`, `fit`/`xit`/`@Disabled` esquecidos e valida código limpo (opcionalmente roda regressão com `-ComRegressao`). | `powershell -ExecutionPolicy Bypass -File scripts/auditar-kirito.ps1` (ou com `-ComRegressao`) |
| `testar-tudo.ps1` | Roda as suítes de testes do Backend (Maven/JUnit/BDD) e Frontend (Karma/Jasmine Headless) em sequência. | `powershell -ExecutionPolicy Bypass -File scripts/testar-tudo.ps1` (ou `-ApenasBackend` / `-ApenasFrontend`) |
| `deploy-hostinger.sh` | Atualiza o código no VPS Hostinger via Git e sobe os containers com Docker Compose (`bot-java`, `postgres`, `wppconnect`). | `bash scripts/deploy-hostinger.sh` (executado no VPS ou acionado pelo GitHub Actions) |

---

## Regras para Adicionar Novos Scripts

1. **Sem comentários** no código (`//`, `/* */` ou `#`).
2. Utilizar runtimes existentes do projeto (PowerShell, Node.js, Maven/Java, Bash).
3. Retornar `exit 0` em caso de sucesso e `exit 1` (ou `!= 0`) em caso de erro.
4. Registrar o script imediatamente neste `README.md` e em `AJUDA.md`.
