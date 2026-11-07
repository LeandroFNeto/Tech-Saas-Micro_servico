# Economia de Tokens via Scripts e Automação Local (Scripts-First)

Esta regra estabelece a prioridade absoluta de automação determinística local em vez de re-processamento por inteligência artificial. O objetivo é eliminar o consumo recorrente de tokens de LLM para tarefas que computadores executam com mais rapidez, precisão e custo zero.

---

## 1. Princípio Scripts-First

Sempre que uma operação, validação, checagem, transformação de dados, rotina de teste ou fluxo repetitivo puder ser resolvido por um script determinístico:
- **O agente deve criar o script** (caso ainda não exista) na pasta `scripts/`.
- **O script deve ser executado diretamente**, evitando que a IA leia e processe grandes volumes de texto/código no contexto apenas para computação mecânica.
- Uma vez criado o script, futuras execuções utilizam o script pronto, poupando tokens de raciocínio e geração.

---

## 2. Cenários Mandatórios para Criação de Scripts

1. **Validação de Conformidade e Regras do Projeto**:
   - Auditoria de código limpo (verificar se há comentários proibidos `//` ou `/* */`).
   - Checagem de integridade de endpoints com OpenAPI (`swagger.yaml`).
   - Inspeção de tipos e compilação rápida (`tsc --noEmit`).

2. **Baterias de Testes e Provas**:
   - Execução combinada de testes de frontend e backend em um único comando de saída determinística.
   - Verificação de quebra de regressão e geração de relatórios de falha (`teste-erro.md`).

3. **Manutenção de Ambiente e Banco de Dados**:
   - Limpeza de artefatos temporários, reinicialização de containers Docker (Postgres, WPPConnect).
   - Execução de migrações ou geração de seeds de dados de teste.

4. **Transformação e Análise de Dados**:
   - Extração de métricas, contagem de linhas, varredura de pendências `shortcut:`.
   - Processamento de logs e payloads de teste.

---

## 3. Diretrizes de Qualidade dos Scripts

- **Runtimes Nativos do Workspace**: Utilizar exclusivamente ferramentas já instaladas no ambiente do projeto (PowerShell no Windows, Node.js/npm, Maven/Java). Não instalar ferramentas pesadas de terceiros para automações simples.
- **Idempotência e Códigos de Saída**: O script deve poder rodar repetidas vezes sem efeitos colaterais. Deve emitir código de saída `0` para sucesso e `!= 0` para erro, facilitando a interpretação imediata pelo agente.
- **Código Limpo Sem Comentários**: Scripts criados seguem a regra geral do repositório: sem comentários (`#`, `//` ou `/* */`). O código deve ser conciso e autoexplicativo.
- **Centralização em `scripts/`**: Todo script reutilizável deve residir na raiz em `scripts/` e estar listado em `scripts/README.md`.
- **Atualização de `AJUDA.md`**: Conforme a regra de central de comandos, qualquer novo script criado deve ser registrado em [AJUDA.md](file:///c:/Projeto-futuro-junior/Tech-Saas-Micro_servico/AJUDA.md) no mesmo turno.
