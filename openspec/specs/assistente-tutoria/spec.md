# assistente-tutoria Specification

## Purpose
Disponibiliza um módulo interno e ambiente de testes com IA para diagnóstico da integridade dos serviços e assistência guiada em dúvidas e configurações operacionais baseadas em avisos e tutoriais do sistema.

## Requirements

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

### Requirement: Tela dedicada de tutoriais no painel do cliente
O sistema SHALL disponibilizar uma rota e tela independente na área do cliente (`/cliente/tutoriais`) com navegação acessível no menu principal, apresentando guias práticos e ilustrados de configuração (WhatsApp, horários, fotos, agendamento) e listando tutoriais ativos cadastrados na plataforma.

#### Scenario: Visualização da tela de tutoriais pelo cliente
- **GIVEN** que o cliente está autenticado no painel
- **WHEN** acessa o menu "Tutoriais" (`/cliente/tutoriais`)
- **THEN** o sistema exibe a lista de guias práticos com instruções passo a passo e atalhos para acionar a IA em caso de dúvidas

#### Scenario: Integração de avisos do tipo TUTORIAL na tela
- **GIVEN** a existência de comunicados ativos do tipo TUTORIAL cadastrados pelo admin
- **WHEN** a tela de tutoriais do cliente é carregada
- **THEN** os tutoriais cadastrados aparecem integrados na lista de guias disponíveis

### Requirement: Assistente de tutoria e suporte no painel do cliente
O sistema SHALL disponibilizar endpoint autenticado e interface no painel do cliente permitindo que o usuário da empresa envie dúvidas sobre configurações, regras, avisos e conexão do WhatsApp, recebendo respostas orientadas pela base de conhecimento do sistema e pelos dados do seu próprio estabelecimento.

#### Scenario: Cliente tira dúvida sobre conexão do WhatsApp
- **GIVEN** que o responsável pela empresa está autenticado no painel do cliente
- **WHEN** envia a pergunta "Como conecto o meu WhatsApp?"
- **THEN** o sistema responde com o passo a passo de geração e leitura do QR Code no menu de conexão

#### Scenario: Isolamento de dados na resposta ao cliente
- **GIVEN** um cliente autenticado na Empresa X
- **WHEN** o cliente faz uma pergunta sobre dados cadastrais ou regras de locação
- **THEN** a IA responde usando exclusivamente os dados da Empresa X e omitindo informações de outras empresas

### Requirement: Sessão virtual de testes para simulação de novos módulos
O sistema SHALL fornecer um canal de sessão virtual no assistente que permite ao usuário e ao desenvolvedor simular conversas de clientes finais e testar módulos existentes ou novas funções em homologação sem exigir pareamento ou criação de sessões reais de WhatsApp no WPPConnect.

#### Scenario: Teste simulado de agendamento em sessão virtual
- **GIVEN** que o usuário ativa a simulação de teste no assistente
- **WHEN** envia uma mensagem simulada de cliente final solicitando consulta de agenda
- **THEN** o assistente simula o comportamento do bot respondendo com o fluxo do módulo de agendamento da empresa sem disparar mensagens em WhatsApp físico

#### Scenario: Teste de nova função experimental
- **GIVEN** um novo módulo ou funcionalidade em teste registrado no sistema
- **WHEN** o usuário envia comando de teste na sessão virtual
- **THEN** o assistente processa a regra do módulo correspondente e devolve a resposta simulada sem requerer QR Code
