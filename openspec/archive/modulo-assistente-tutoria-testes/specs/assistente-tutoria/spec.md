# Spec Delta

## Purpose

Disponibiliza um módulo interno e ambiente de testes com IA para diagnóstico da integridade dos serviços e assistência guiada em dúvidas e configurações operacionais baseadas em avisos e tutoriais do sistema.

## ADDED Requirements

### Requirement: Diagnóstico integrado de integridade dos módulos
O sistema SHALL disponibilizar endpoint e rotina de diagnóstico administrativo para checar a saúde operacional de todos os módulos principais do sistema (auth, avisos, agenda, empresa, whatsapp e ia), retornando o status consolidado de cada serviço.

#### Scenario: Execução de diagnóstico com todos os módulos saudáveis
- **GIVEN** que o administrador autenticado solicita a checagem geral de módulos
- **WHEN** a requisição de diagnóstico é processada
- **THEN** o sistema responde status 200 com a lista de módulos contendo status operacional positivo para todos os serviços

#### Scenario: Detecção de módulo com falha no diagnóstico
- **GIVEN** que o serviço de integração com WhatsApp ou IA está temporariamente indisponível
- **WHEN** o administrador executa a checagem geral de módulos
- **THEN** o sistema responde indicando o módulo afetado com status de falha e mensagem explicativa, sem interromper a checagem dos demais

### Requirement: Consulta de avisos e tutoriais via assistente IA
O sistema SHALL processar perguntas do usuário através de um assistente de IA interno que recupera avisos ativos e tutoriais de configuração para gerar respostas precisas e instruções passo a passo.

#### Scenario: Esclarecimento de dúvida sobre configuração do sistema
- **GIVEN** um usuário no módulo de testes solicitando ajuda para configurar horários de locação ou conexão do WhatsApp
- **WHEN** a pergunta é enviada ao assistente de IA
- **THEN** o sistema retorna resposta contextualizada contendo os passos detalhados de configuração baseados nos tutoriais

#### Scenario: Consulta contextualizada a avisos vigentes
- **GIVEN** a existência de avisos ativos cadastrados no sistema
- **WHEN** o usuário questiona a IA sobre manutenções programadas ou comunicados recentes
- **THEN** a IA responde integrando o conteúdo dos avisos vigentes à resposta

### Requirement: Comutador de modo de operação no módulo de teste
O módulo de uso interno SHALL fornecer um botão comutador (toggle) para alternar entre o Modo Diagnóstico (verificação de saúde dos módulos) e o Modo Atendimento Assistido (simulação e teste de atendimento ao cliente).

#### Scenario: Seleção do Modo Diagnóstico
- **WHEN** o usuário seleciona o modo diagnóstico no comutador
- **THEN** o módulo exibe os indicadores de saúde dos serviços e habilita o botão para disparar novos testes de integridade

#### Scenario: Seleção do Modo Atendimento Assistido
- **WHEN** o usuário seleciona o modo atendimento no comutador
- **THEN** o módulo ativa o painel interativo de chat para testes e simulação de respostas da IA aos clientes

### Requirement: Controle de acesso restrito a administradores
O sistema SHALL exigir autenticação administrativa válida via cabeçalho `x-admin-token` ou token JWT de administrador para qualquer chamada aos endpoints e telas do módulo de uso interno.

#### Scenario: Requisição não autorizada ao módulo interno
- **WHEN** uma requisição para diagnóstico ou chat interno é enviada sem credenciais administrativas
- **THEN** o sistema rejeita a chamada respondendo HTTP 401 Unauthorized

#### Scenario: Requisição autorizada por administrador
- **GIVEN** um token administrativo válido informado na requisição
- **WHEN** a requisição de teste ou consulta é enviada
- **THEN** o sistema processa a operação e responde com código HTTP de sucesso
