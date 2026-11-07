# Tasks

## 1. Contrato OpenAPI (Contract-First)

- [x] 1.1 Atualizar `especificacao/back/swagger.yaml` e criar especificação de endpoints de contatos com listagem paginada (parâmetros `pagina`, `tamanho`), endpoint pontual de revelação de contato limitado por cota e rota de exclusão/opt-out, validando sintaxe YAML via script de verificação OpenAPI.
- [x] 1.2 Declarar schemas de DTOs de contato no OpenAPI contendo nome de exibição/sugestão, número de telefone mascarado por padrão (`+55 (11) 9****-1234`), data da última interação e status de opt-out, assegurando conformidade de contrato.

## 2. Backend Java e Segurança LGPD

- [x] 2.1 Atualizar persistência de contatos em `ServicoContato.java` e `Contato.java` para capturar e persistir o nome do remetente (pushName da mensagem ou sugestão de identidade) recebido no webhook do WhatsApp, verificando com teste unitário.
- [x] 2.2 Implementar `ContatoDTO` com inclusão do nome de exibição/sugestão e mascaramento automático de número no backend antes do retorno HTTP, validando formatação segura.
- [x] 2.3 Implementar limitador de cota anti-vazamento em memória/filtro por usuário permitindo no máximo 10 revelações ou obtenções de contatos completos por hora, retornando HTTP 429 Too Many Requests ao tentar o 11º contato, validado com testes unitários.
- [x] 2.4 Implementar `ControllerContato.java` mapeando os endpoints `/api/v1/contatos` para consulta paginada, revelação controlada por cota e exclusão/opt-out, validando conformidade com OpenAPI.
- [x] 2.5 Implementar lógica de opt-out e exclusão de contato no `ServicoContato.java` (removendo ou anonimizando registro e limpando hash de busca) e integrar verificação de comandos ("SAIR", "CANCELAR", "STOP") no fluxo do webhook.

## 3. Frontend Angular (Painel do Cliente)

- [x] 3.1 Criar serviço Angular de contatos (`ContatoService`) para consumo das rotas paginadas da API de contatos, revelação controlada por cota e tratamento de erros de limite (HTTP 429).
- [x] 3.2 Implementar tela de contatos no painel do cliente (`features/client/contatos`) exibindo tabela com nome/sugestão do contato, telefone mascarado, indicador da cota horária restante, botão de revelação pontual (máx 10/hora) e botão de exclusão LGPD, sem permitir exportação massiva.
- [x] 3.3 Adicionar rota e item de navegação no menu do painel do cliente para a aba de Contatos, validando navegação limpa e responsiva.
- [x] 3.4 Escrever testes de componente Angular (Jasmine/Karma) cobrindo a renderização de nomes, números mascarados, consumo da cota de 10 contatos/hora e exclusão.

## 4. Testes Integrados e Conformidade

- [x] 4.1 Criar cenários BDD (Cucumber) cobrindo a coleta de nome e número no webhook, deduplicação HMAC, listagem mascarada, bloqueio na 11ª revelação de contato por hora e opt-out.
- [x] 4.2 Executar suíte completa de testes de regressão backend (`mvn clean test`) e frontend (`npm test -- --watch=false`), garantindo zero quebras e 100% de sucesso.
- [x] 4.3 Executar auditoria de qualidade (`powershell -ExecutionPolicy Bypass -File scripts/auditar-kirito.ps1 -ComRegressao`) garantindo conformidade total de código limpo e regras do projeto.

## Workflow follow-up

- Arquivar a change via `/opsx-archive` ou fluxo correspondente após aprovação e implementação.
- Atualizar documentação e central de tarefas se aplicável.
