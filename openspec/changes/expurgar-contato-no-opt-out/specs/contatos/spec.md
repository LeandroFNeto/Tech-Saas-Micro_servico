# Spec Delta

## MODIFIED Requirements

### Requirement: Direito ao esquecimento e opt-out
O sistema SHALL garantir o cancelamento de comunicação e exclusão ou anonimização dos dados do titular quando solicitado via mensagem de WhatsApp pelo cliente ou por comando de exclusão do operador no painel. No opt-out, a linha do contato SHALL guardar só o índice do número e a marca de não contatar.

Contrato: `especificacao/back/paths/empresas-contatos-numero.yaml`
Prova: `ServicoContatoTest`, `ServicoRespostaLembreteTest`, `ControllerClienteAutomacaoTest`

#### Scenario: Cliente solicita opt-out pelo WhatsApp
- **WHEN** o cliente envia mensagem contendo comando de cancelamento ("SAIR" ou "CANCELAR") ou responde ao lembrete pedindo para parar
- **THEN** o sistema SHALL registrar o opt-out, bloquear novos envios e apagar WhatsApp, nome e final do telefone, mantendo o índice do número

#### Scenario: Contato expurgado volta a escrever
- **GIVEN** um contato que fez opt-out
- **WHEN** o mesmo número envia nova mensagem
- **THEN** o sistema SHALL atualizar só a data da última interação, sem gravar nome, WhatsApp nem final do telefone, e SHALL manter o bloqueio de envios

#### Scenario: Revelação de contato expurgado
- **WHEN** o operador pede o número completo de um contato que fez opt-out
- **THEN** o sistema SHALL responder 422 sem registrar auditoria nem consumir a cota horária

#### Scenario: Contato expurgado na rotação de chaves
- **WHEN** a rotação das chaves de contatos encontra uma linha sem WhatsApp
- **THEN** o sistema SHALL trocar o índice dessa linha por um valor aleatório, manter a marca de não contatar e não deixar índice calculado com a chave antiga

#### Scenario: Operador remove contato pelo painel
- **WHEN** o operador confirma a exclusão do contato no painel
- **THEN** o sistema SHALL remover definitivamente o registro e seu índice da base
