# Spec Delta

## ADDED Requirements

### Requirement: Sessão da atendente da Gamb no WhatsApp
O sistema SHALL tratar todo contato da sessão da atendente da Gamb como cliente e oferecer tirar uma dúvida ou marcar reunião. Na dúvida, o bot SHALL responder só com a base de conhecimento e os avisos ativos, SHALL NOT enviar ao modelo nem devolver dado de empresa e SHALL NOT executar ação no sistema. Mensagem fora do escopo SHALL receber a recusa padrão do Suporte. A mensagem filtrada ou recusada também responde 200 ao webhook.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `AtendenteGambStrategyTest`, `src/test/resources/features/webhook.feature`

#### Scenario: Cliente tira dúvida no número da Gamb
- **GIVEN** uma mensagem recebida na sessão da atendente da Gamb
- **WHEN** o cliente escolhe tirar dúvida e pergunta como conectar o WhatsApp
- **THEN** o bot responde com o passo a passo da base de conhecimento

#### Scenario: Cliente pede dado de empresa no número da Gamb
- **GIVEN** uma mensagem recebida na sessão da atendente da Gamb
- **WHEN** o cliente pede preços, reservas ou dados de uma empresa
- **THEN** o bot recusa e orienta a usar o painel, sem revelar dado de empresa

### Requirement: Reunião com a equipe da Gamb pelo WhatsApp
O sistema SHALL permitir que o cliente marque reunião com a equipe informando o motivo (ajuste ou nova inovação), a data e o horário. Com o horário livre na Google Agenda da Gamb, o sistema SHALL criar o evento e convidar por e-mail todos os responsáveis cadastrados naquele momento. Com o horário ocupado, o sistema SHALL oferecer outras opções e SHALL NOT criar evento. Sem responsáveis cadastrados ou sem agenda conectada, o sistema SHALL informar que o agendamento está indisponível e SHALL manter a dúvida funcionando. Ao mudar a lista de responsáveis, o sistema SHALL atualizar só os convidados das reuniões futuras já marcadas e SHALL NOT cancelar nem remarcar reunião. O e-mail dos responsáveis SHALL NOT aparecer em log.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `AtendenteGambStrategyTest`

#### Scenario: Cliente marca reunião em horário livre
- **GIVEN** responsáveis cadastrados e a Google Agenda da Gamb conectada
- **WHEN** o cliente escolhe marcar reunião, informa motivo, data e horário livres e confirma o resumo
- **THEN** o evento é criado, os responsáveis recebem o convite por e-mail e o bot responde com data e horário da reunião

#### Scenario: Cliente escolhe horário ocupado
- **GIVEN** um horário já ocupado na Google Agenda da Gamb
- **WHEN** o cliente informa esse horário
- **THEN** o bot oferece outros horários e nenhum evento é criado

#### Scenario: Agendamento indisponível
- **GIVEN** a lista de responsáveis vazia
- **WHEN** o cliente escolhe marcar reunião
- **THEN** o bot informa que o agendamento está indisponível e a opção de dúvida continua respondendo

### Requirement: Limite diário de mensagens da atendente
O sistema SHALL limitar o número de mensagens por número de cliente por dia na sessão da atendente. Acima do limite, o bot SHALL responder uma mensagem fixa e SHALL NOT chamar o modelo.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `AtendenteGambStrategyTest`

#### Scenario: Cliente passa do limite diário
- **GIVEN** um número que atingiu o limite diário
- **WHEN** envia outra mensagem à sessão da atendente
- **THEN** o bot responde a mensagem fixa e o modelo não é chamado

### Requirement: Sessão de teste de módulos da Gamb
O sistema SHALL entregar as mensagens da sessão de teste da Gamb ao bot de teste somente quando o número estiver na lista de autorizados. O bot SHALL listar os módulos experimentais com `/modulos` e executar um deles com `/modulo <nome> <texto>`. Número não autorizado SHALL receber uma resposta fixa e nenhum módulo SHALL rodar. O módulo experimental SHALL responder de forma simulada e SHALL NOT gravar em dados de empresas reais.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `TesteModulosStrategyTest`

#### Scenario: Número autorizado lista os módulos
- **GIVEN** um número autorizado na sessão de teste
- **WHEN** envia `/modulos`
- **THEN** o bot responde com o nome e a descrição de cada módulo registrado

#### Scenario: Número autorizado executa um módulo
- **GIVEN** um módulo experimental registrado
- **WHEN** o número autorizado envia `/modulo <nome> <texto>`
- **THEN** o bot responde com a resposta simulada do módulo, sem gravar em empresa real

#### Scenario: Número não autorizado
- **GIVEN** um número fora da lista de autorizados
- **WHEN** envia qualquer mensagem à sessão de teste
- **THEN** o bot responde a mensagem fixa e nenhum módulo é executado
