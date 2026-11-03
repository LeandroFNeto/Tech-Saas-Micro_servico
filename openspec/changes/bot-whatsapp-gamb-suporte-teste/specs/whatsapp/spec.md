# Spec Delta

## ADDED Requirements

### Requirement: Sessão de Suporte da Gamb no WhatsApp
O sistema SHALL entregar as mensagens recebidas na sessão de Suporte da Gamb ao bot de Suporte, que responde só com a base de conhecimento e os avisos ativos. O bot SHALL NOT enviar ao modelo nem devolver dado de empresa e SHALL NOT executar ação no sistema. Mensagem fora do escopo SHALL receber a recusa padrão do Suporte. A mensagem filtrada ou recusada também responde 200 ao webhook.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `SuporteGambStrategyTest`, `src/test/resources/features/webhook.feature`

#### Scenario: Cliente tira dúvida no número da Gamb
- **GIVEN** uma mensagem recebida na sessão de Suporte da Gamb
- **WHEN** o cliente pergunta como conectar o WhatsApp
- **THEN** o bot responde com o passo a passo da base de conhecimento

#### Scenario: Cliente pede dado de empresa no número da Gamb
- **GIVEN** uma mensagem recebida na sessão de Suporte da Gamb
- **WHEN** o cliente pede preços, reservas ou dados de uma empresa
- **THEN** o bot recusa e orienta a usar o painel, sem revelar dado de empresa

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
