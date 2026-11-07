# Spec Delta

## ADDED Requirements

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
