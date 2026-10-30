# Central de Ajuda e Guia de Comandos — Tech-SaaS

Este documento é a referência central e rápida de todos os comandos, skills e automações disponíveis no projeto **Tech-SaaS Micro-Serviço**.

---

## 1. OpenSpec (`/openspec`)

Controla o ciclo de vida de especificações e mudanças de funcionalidades do projeto (Domain Specs e Changes).

| Comando | Descrição |
|---|---|
| `/openspec` | Abre o planejamento e aplicação de mudanças no OpenSpec. |
| `/openspec proposal <nome>` | Inicia o modo **propor** (cria `proposal.md`, `design.md` e `tasks.md` dentro de `openspec/changes/<nome>/`). |
| `/openspec apply <nome>` | Inicia o modo **aplicar** (codifica apenas após aprovação formal da pasta `changes/<nome>/`). |
| `/openspec archive <nome>` | Conclui e consolida a funcionalidade nas especificações definitivas (`openspec/specs/`) e arquiva a change. |

> **Regra Importante:** Toda nova funcionalidade ou tela obrigatoriamente passa pelo gate do OpenSpec (`proposal.md` -> aprovação -> código).

---

## 2. Orquestração da Equipe de Agentes (`/horadoshow`)

Coordena a atuação automatizada dos papéis de IA no projeto em etapas sequenciais com divisão de responsabilidades.

| Comando | Descrição |
|---|---|
| `/horadoshow <nome-da-tarefa>` | Executa o fluxo completo da tarefa: **Programação** -> **Testes** -> **Qualidade** -> **Segurança** -> **Revisão** -> **Deploy**. |

- **Modo Ponytail Ativo:** Ao iniciar qualquer etapa, a IA já aplica automaticamente o modo Ponytail (foco na menor solução funcional, stdlib primeiro e sem complexidade desnecessária).
- Cada etapa atualiza seu status no arquivo `openspec/central-tarefas.md`.
- Se os testes falharem, um relatório é salvo em `teste-erro.md` e o fluxo é corrigido antes de avançar.
- A segurança é auditada antes da revisão e da consolidação final da spec.

---

## 3. Frontend Visual & UI (`/diasabatico`)

Foco exclusivo no acabamento visual do frontend Angular antes de qualquer integração com APIs ou backend.

| Comando | Descrição |
|---|---|
| `/diasabatico <nome-da-tarefa>` | Executa as fases de acabamento visual: HTML, Tailwind CSS, layout, estados de componentes e navegação entre telas. |

- Não mexe em chamadas HTTP, services ou lógica de backend.
- Contexto é mantido em `openspec/changes/<nome>/diasabatico/`.

---

## 4. Painel de Status do Projeto (`/painel`)

Gera uma visão geral executiva do estado do sistema.

| Comando | Descrição |
|---|---|
| `/painel` | Gera a página web e resumo do projeto: status de tarefas, pendências de changes, equipe de IA, paleta de cores, tipografia e requisitos de specs. |

---

## 5. Modo Senior Minimalista (`/ponytail`)

Conjunto de ferramentas globais instaladas para garantir o menor diff seguro, código enxuto e prevenção de over-engineering.

| Comando | Descrição |
|---|---|
| `/ponytail` | Consulta ou ativa o modo lazy senior dev (foco no menor código funcional). |
| `/ponytail lite` | Sugere opções menores de implementação e deixa a decisão explícita. |
| `/ponytail full` | Nível padrão: aplica a solução mais enxuta com stdlib e aproveitamento do código existente. |
| `/ponytail ultra` | Nível agressivo: recusa abstrações e exige a menor modificação estrita. |
| `/ponytail off` | Desativa o modo Ponytail na sessão. |
| `/ponytail-review` | Revisão de código e diffs focando em simplicidade, segurança e menor alteração necessária. |
| `/ponytail-audit` | Auditoria completa do repositório procurando complexidade desnecessária, bloat e código morto. |
| `/ponytail-debt` | Lista atalhos técnicos e pendências marcadas com `shortcut:`. |
| `/ponytail-gain` | Exibe o relatório de economia de código, tokens e performance. |
| `/ponytail-help` | Guia rápido de referência dos comandos do Ponytail. |

---

## 6. Navegação e Grafo de Conhecimento (`graphify`)

Utilitário de navegação inteligente do código baseado em AST para evitar buscas cegas.

| Comando | Descrição |
|---|---|
| `graphify query "<pergunta>"` | Consulta estruturada de arquitetura ou fluxo no grafo de conhecimento. |
| `graphify path "<A>" "<B>"` | Encontra o caminho de dependências entre dois símbolos ou componentes. |
| `graphify explain "<conceito>"` | Explica os nós e conexões de um conceito no sistema. |
| `graphify update .` | Atualiza o grafo em `graphify-out/` após alterações nos arquivos do projeto. |

---

## 7. Testes de Caixa Branca & Robustez (`/kirito`)

Especialista em teste de caixa branca e blindagem de código (Dual Wielding: Back-end Java e Front-end Angular).

| Comando | Descrição |
|---|---|
| `/kirito` | Inicia o caçador de bugs de caixa branca para mapear branches, condições de borda e lacunas de teste. |
| `/kirito <classe-ou-arquivo>` | Forja testes de caixa branca específicos cobrindo branches, limites numéricos/temporais e falhas simuladas. |
| `/kirito full` | Executa auditoria e prova completa de robustez no backend (`mvn test`) e frontend (`npm test`). |

- **Foco Estrito:** Cobertura de branches, valores limítrofes (`24:00`, `23:59`, `null`), resiliência a falhas externas e transições de estado.
- **Modelos:** Roda no `Claude Sonnet 5.5 Medium` / subagente `pro` no Antigravity ou `claude-sonnet-5-5` no Cursor/Claude.

---

## Manutenção deste Arquivo

Conforme definido na regra do repositório em `.agents/rules/ajuda-comandos.md`:
Sempre que uma nova skill, comando ou rotina for adicionada ou alterada no projeto Tech-SaaS, este arquivo `AJUDA.md` **deve ser mantido e atualizado no mesmo turno**.
