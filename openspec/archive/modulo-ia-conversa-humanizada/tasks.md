# Tasks

## 1. Contexto Multi-Tenant e Prompt Sanitizado

- [x] 1.1 Criar builder de prompt no `ServicoIA` para compor o contexto exclusivo da empresa da sessão (nome, regras, tabela de preços, endereço e fotos) sem expor segredos e verificar com teste unitário.
- [x] 1.2 Configurar timeout de 3 segundos no `RestTemplate` e tratamento resiliente de erro de cota (HTTP 429) no `ServicoIA`, verificando com teste de simulação de falha.

## 2. Integração com o Fluxo de Atendimento WhatsApp

- [x] 2.1 Conectar o `ServicoIA` no processamento de mensagens em estado `INICIO` quando a empresa possuir `usaIA = true` e verificar atendimento de saudações e dúvidas com mock da API.
- [x] 2.2 Implementar fallback transparente para o menu determinístico padrão caso a IA falhe ou estoure o timeout e verificar com teste de integração.

## 3. Validação e Qualidade

- [x] 3.1 Adicionar testes em `ServicoIATest` e `ControllerWhatsappTest` cobrindo isolamento multi-tenant, resposta humanizada e fallback da cota.
- [x] 3.2 Executar a suíte completa de testes unitários e BDD (`CucumberTest`) garantindo zero regressões.
