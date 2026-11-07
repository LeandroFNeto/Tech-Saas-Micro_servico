# Spec Delta

## ADDED Requirements

### Requirement: Atendimento Humanizado de Saudações e Dúvidas com IA
O sistema SHALL responder saudações e dúvidas básicas da empresa em linguagem natural quando a empresa possuir `usaIA = true` e o cliente estiver no estado inicial de atendimento.

#### Scenario: Cliente envia saudação informal
- **WHEN** cliente envia "Bom dia, tudo bem?" no WhatsApp para empresa com `usaIA = true`
- **THEN** o sistema responde cordialmente com mensagem humanizada contendo o nome da empresa e orientando as opções de agendamento e informações disponíveis

#### Scenario: Cliente pergunta sobre regras ou valores da empresa
- **WHEN** cliente pergunta "Qual o valor do espaço e o que inclui?"
- **THEN** o sistema utiliza exclusivamente os dados de tabela de preços e regras cadastrados daquela empresa para responder de forma concisa e humanizada

### Requirement: Isolamento Estrito de Contexto Multi-Tenant da Sessão
O sistema SHALL restringir todas as informações enviadas no prompt da IA aos dados da empresa vinculada à sessão ativa do WhatsApp, sem incluir segredos nem dados de outras empresas.

#### Scenario: Consulta sem vazamento entre empresas
- **WHEN** a IA é acionada para responder a um cliente na sessão da Empresa A
- **THEN** o prompt contém exclusivamente dados cadastrais públicos da Empresa A e omite credenciais, senhas e identificadores de calendário

### Requirement: Resiliência e Fallback de Cota Gratuita
O sistema SHALL realizar chamadas ao modelo leve de IA respeitando a cota gratuita da API, abortando a chamada após 3 segundos e caindo em fallback determinístico caso a API falhe ou a cota atinja o limite.

#### Scenario: Falha ou esgotamento de cota da IA aciona menu padrão
- **WHEN** a chamada ao modelo Gemini falhar, estourar timeout de 3 segundos ou retornar erro de cota
- **THEN** o sistema responde com o menu principal determinístico sem travar a conversa do cliente
