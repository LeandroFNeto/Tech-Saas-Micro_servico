# Base de Conhecimento e Contexto Operacional do GAMB

> **Finalidade do Documento**: Este arquivo serve como introdução oficial ao sistema GAMB e como base de conhecimento viva (prompt context / knowledge base) para o Assistente de Inteligência Artificial e para a equipe de desenvolvimento. Ele documenta todas as regras de negócio, histórias de usuário, requisitos e instruções passo a passo para configuração e diagnóstico do aplicativo. Deve ser expandido e atualizado continuamente a cada nova evolução da plataforma.

---

## 1. Visão Geral do Sistema (O que é o GAMB)

O **GAMB** (Gerenciador de Atendimento para Meios de Hospedagem e Bens / Pequenos Negócios) é uma plataforma SaaS com microserviços e assistente virtual integrado ao WhatsApp, projetada para automatizar o atendimento ao cliente e o gerenciamento de reservas de estabelecimentos comerciais (como locação de piscinas, quiosques, chácaras, quadras esportivas e prestadores de serviço com agendamento).

### 1.1 Objetivos Principais
- **Atendimento Ágil**: Resposta automatizada imediata no WhatsApp aos clientes, diminuindo o tempo de espera e o trabalho manual do proprietário.
- **Sincronização de Agenda**: Consulta e solicitação de reservas integradas ao Google Agenda, evitando conflitos de horários e duplicidade de locações.
- **Painel Administrativo Web**: Interface intuitiva para o proprietário gerenciar fotos, regras, preços, cardápios e acompanhar a conexão de seu WhatsApp via QR Code.
- **Isolamento Multi-Tenant**: Cada empresa possui dados, configurações e regras estritamente isoladas das demais empresas cadastradas.
- **Arquitetura Orientada a Contrato (OpenAPI First)**: Todas as APIs REST e endpoints são formalizados antes de serem codificados.

---

## 2. Perfis de Usuários e Atores

1. **Cliente Final (via WhatsApp)**:
   - Interage exclusivamente pelo número de WhatsApp cadastrado pela empresa.
   - Recebe menus dinâmicos, consulta fotos, valores, regras, localização e disponibilidade de datas/horários.
   - Solicita reservas e recebe instruções claras de confirmação.

2. **Responsável pela Empresa (Proprietário / Gerente)**:
   - Acessa o Painel Web do GAMB com credenciais individuais.
   - Personaliza os dados da empresa (nome, mensagem de saudação, ramo, links de fotos e localização).
   - Conecta a sessão de WhatsApp da empresa através de leitura de QR Code.
   - Consulta comunicados e avisos gerais emitidos pela plataforma.

3. **Administrador da Plataforma (Admin)**:
   - Acesso irrestrito a todas as empresas registradas na plataforma.
   - Cadastra, edita e exclui empresas do SaaS.
   - Configura quais módulos de atendimento estão habilitados para cada empresa (ex.: módulo piscina, módulo cardápio, módulo IA).
   - Cadastra e gerencia os Avisos gerais do sistema.
   - Acessa a **Tela de Admin** contendo o **Botão de Diagnóstico de Módulos** e o **Laboratório de Testes com IA**.

---

## 3. Catálogo de Histórias de Usuário (US01 a US20)

### Épico 1 – Cliente Final (WhatsApp)
- **US01 - Receber menu de atendimento**: O cliente inicia a conversa e recebe um menu dinâmico numerado contendo apenas as opções ativas para a empresa.
- **US02 - Visualizar fotos da empresa**: Envio da foto principal do estabelecimento e oferta de fotos complementares (galeria até 5 imagens).
- **US03 - Consultar informações da empresa**: Exibição dos valores, localização (link do Maps) e regras de locação.
- **US04 - Consultar disponibilidade para reserva**: Verificação de datas e períodos livres conforme a modalidade de locação (diária ou hora). O cliente pode escrever a data do jeito que preferir (`25/12`, `25 de dezembro`, `amanhã`); quando a data fica em dúvida, o atendimento pergunta qual ele quis dizer antes de consultar a agenda.
- **US05 - Solicitar reserva**: Coleta dos dados do solicitante, revisão dos dados e registro da intenção de reserva para processamento com Google Agenda.
- **US06 - Consultar cardápio ou produtos**: Consulta a catálogo de itens ou opções de cardápio caso a empresa utilize esse módulo.

### Épico 2 – Gestão da Empresa (Painel do Cliente)
- **US07 - Entrar no painel**: Autenticação segura por e-mail e senha com JWT.
- **US08 - Alterar senha**: Troca de senha da conta com validação de políticas de segurança.
- **US09 - Atualizar informações da empresa**: Edição de dados cadastrais, regras de negócio e tabela de preços.
- **US10 - Gerenciar conexão do WhatsApp**: Inicialização de sessão, visualização do QR Code e monitoramento do status da conexão (Conectado / Desconectado).
- **US11 - Consultar avisos ativos**: Visualização de comunicados, novidades e alertas gerais do sistema.

### Épico 3 – Administração da Plataforma
- **US12 - Cadastrar empresa**: Registro de novos estabelecimentos com definição de plano e ramo.
- **US13 - Consultar empresas**: Listagem completa de clientes corporativos com controle de acesso administrativo.
- **US14 - Configurar módulos da empresa**: Habilitação seletiva de módulos (ex.: agendamento, galeria, catálogo, IA).
- **US15 - Administrar avisos**: Criação, edição e exclusão de comunicados gerais com controle de status ativo/inativo.

### Épico 4 – Qualidade, Evolução e IA
- **US16 - Acompanhar indicadores de atendimento**: Métricas de mensagens trocadas, horas economizadas e reservas solicitadas.
- **US17 - Administrar planos de assinatura**: Vinculação de limites e módulos aos planos contratuais.
- **US18 - Habilitar respostas com IA**: Ativação de atendimento humanizado com IA para empresas elegíveis.
- **US19 - Proteger recebimento de mensagens**: Filtro de segurança anti-loop (ignora mensagens do próprio bot, grupos e formatos inválidos).
- **US20 - Manter contrato e testes da API**: Manutenção contínua do Swagger/OpenAPI e testes automatizados.

---

## 4. Requisitos Funcionais e Não Funcionais

### Requisitos Funcionais Relevantes (RF)
- **RF01**: Menu dinâmico de WhatsApp com numeração automática baseada nos módulos habilitados.
- **RF03**: Integração do fluxo de reservas com consulta e sincronização do Google Agenda.
- **RF04**: Painel web com conexão do WhatsApp via QR Code e personalização de mensagens e regras.
- **RF05**: Gerenciador de comunicados e tutoriais refletindo atualizações no atendimento.
- **RF10**: Configuração de ramo de atuação e ativação de módulos premium, incluindo atendimento por IA.

### Requisitos Não Funcionais Críticos (RNF)
- **RNF01**: Clean Architecture e padrões SOLID (Strategy, Factory, Observer, Singleton).
- **RNF02 (OpenAPI Contract-First)**: A especificação OpenAPI em `especificacao/back/` é a Fonte Única de Verdade. Qualquer endpoint novo ou modificado deve nascer no YAML antes do código Java.
- **RNF03**: Separação clara entre frontend Angular, backend Spring Boot e WPPConnect.
- **RNF04 (Otimização de Tokens IA)**: Camada de consulta em grafo (GraphQL `/api/ia-query`) para injetar na IA apenas os dados solicitados, reduzindo drasticamente o consumo de tokens.
- **RNF05**: Autenticação com JWT, hashing de senhas com BCrypt e seeding de dados mestre.
- **RNF06**: Isolamento estrito entre empresas (multi-tenant) e prevenção de loops em grupos.

---

## 5. Guia Passo a Passo de Configuração para o Usuário

### 5.1 Como Conectar o WhatsApp da Empresa
1. Acesse o Painel do GAMB com seu usuário e senha.
2. Na página inicial / Meu Perfil, localize a seção **Conexão WhatsApp**.
3. Clique em **Iniciar Conexão** para gerar o QR Code.
4. Abra o WhatsApp no seu smartphone, vá em **Aparelhos Conectados** > **Conectar um Aparelho** e aponte a câmera para o QR Code da tela.
5. Aguarde até que o status mude para **Conectado**. O bot passará a responder os clientes automaticamente.

### 5.2 Como Configurar Horários e Modalidade de Locação
1. No menu lateral, acesse **Configurações do Estabelecimento**.
2. Escolha entre locação por **Diária** (datas cheias) ou **Por Hora**.
3. Defina os horários de início e término disponíveis para atendimento.
4. Salve as alterações para que a IA e o menu de disponibilidade no WhatsApp passem a usar a nova grade.

### 5.3 Como Atualizar Fotos e Tabela de Preços
1. No painel de informações, insira o link da **Foto Principal** (imagem de destaque).
2. Adicione até 5 links de imagens para a **Galeria de Fotos Adicionais**.
3. Atualize o campo **Tabela de Preços / Valores** com as regras vigentes (ex.: dias de semana vs. finais de semana/feriados).

---

## 6. Módulo de Diagnóstico e Assistente de Tutoria (Tela de Admin)

### 6.1 O Botão de Diagnóstico de Módulos (Health Check Geral)
- **Localização**: Fica acessível diretamente na **Tela de Admin** do painel web.
- **Funcionamento**: Ao ser acionado pelo administrador, executa uma verificação não destrutiva e com timeout curto (2 segundos) nos seguintes subsistemas:
  1. **Banco de Dados (PostgreSQL)**: Checagem de integridade das tabelas e conectividade.
  2. **Módulo de Autenticação (Auth)**: Validação de emissão e assinatura de tokens JWT.
  3. **Módulo de Avisos (Avisos)**: Consulta aos comunicados ativos.
  4. **Módulo de Empresas (Empresa)**: Acesso e integridade cadastral das empresas.
  5. **Módulo de Agenda**: Conectividade com a infraestrutura de reservas.
  6. **Módulo WhatsApp (WPPConnect)**: Verificação da saúde do serviço de mensageria.
  7. **Módulo de Inteligência Artificial (Gemini)**: Verificação de disponibilidade e cota da API.
- **Resultado Visual**: O painel exibe cartões com indicadores coloridos (Verde = Operacional, Amarelo = Degradado, Vermelho = Indisponível) com relatório explicativo para resolução rápida.

### 6.2 Assistente de Tutoria e Dúvidas com IA
- **Como a IA responde**: O assistente utiliza este documento (`docs/base-conhecimento-gamb.md`) em conjunto com os avisos ativos para esclarecer qualquer dúvida sobre:
  - "Como faço para cadastrar um novo aviso?"
  - "Por que meu WhatsApp desconectou e como reconectar?"
  - "Como configurar reserva por hora em vez de diária?"
  - "Quais são as diferenças de permissão entre o Responsável e o Administrador?"
- **Comutador de Modo**:
  - **Modo Diagnóstico**: Foco na saúde técnica dos módulos e execução de testes.
  - **Modo Atendimento Sandbox**: Foco em simular o que o cliente final veria no WhatsApp, testando o fluxo de linguagem natural e o menu de atendimento.

---

## 7. Instruções para a IA (Diretrizes de Contexto e Atendimento)

Quando a IA estiver atuando como assistente do GAMB:
1. **Linguagem**: Sempre responda em Português do Brasil (pt-BR) de forma educada, prestativa e objetiva.
2. **Base de Verdade**: Suas respostas sobre o funcionamento do aplicativo devem se basear estritamente neste documento e nos avisos cadastrados no sistema.
3. **Segurança**: Nunca invente rotas ou exponha credenciais, chaves de API (`adminApiKey`), senhas ou identificadores sensíveis de calendário (`googleCalendarId`).
4. **Resolução de Problemas**: Quando o usuário relatar falha, oriente o passo a passo de verificação (ex.: conferir QR Code, checar status no botão de diagnóstico ou verificar o preenchimento de campos obrigatórios).
5. **Evolução Contínua**: À medida que novas histórias de usuário e modificações forem implementadas, este documento deve ser atualizado para refletir o comportamento real do software.
