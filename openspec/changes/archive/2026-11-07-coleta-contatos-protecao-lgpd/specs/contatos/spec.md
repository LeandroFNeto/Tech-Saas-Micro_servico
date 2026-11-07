# Spec Delta

## Purpose

Gerencia o ciclo de vida, persistência criptografada, conformidade LGPD, exibição mascarada e prevenção contra raspagem e vazamento de contatos de clientes capturados pelo canal de atendimento WhatsApp.

## ADDED Requirements

### Requirement: Coleta e deduplicação segura no webhook
O sistema SHALL registrar e atualizar contatos do webhook vinculando número e nome informado (pushName do WhatsApp ou sugestão), indexando por HMAC unidirecional para deduplicação e cifrando os dados sensíveis com AES em repouso.

#### Scenario: Novo contato com nome informado recebido via webhook
- **WHEN** uma mensagem válida contendo número e pushName é processada pelo webhook
- **THEN** o sistema SHALL salvar o contato com nome, número criptografado em repouso e índice HMAC

#### Scenario: Mensagem recebida de contato existente
- **WHEN** uma mensagem de um contato já registrado é processada
- **THEN** o sistema SHALL atualizar a data da última interação e o nome se houver nova identificação

### Requirement: Exibição mascarada e identificação no painel
O sistema SHALL exibir no painel a lista paginada com nome ou sugestão do remetente e o número de telefone mascarado por padrão (`+55 (11) 9****-1234`), permitindo identificar a origem sem expor o número completo desnecessariamente.

#### Scenario: Listagem de contatos no painel
- **WHEN** o operador consulta a lista de contatos
- **THEN** o sistema SHALL exibir o nome/sugestão identificado e o telefone com máscara de proteção

#### Scenario: Solicitação de página com limite excessivo
- **WHEN** a requisição informa tamanho de página superior ao limite máximo estipulado
- **THEN** o sistema SHALL limitar ao teto máximo permitido e responder com status 200

### Requirement: Limite anti-vazamento de 10 contatos por hora
O sistema SHALL bloquear exportações em massa e impor limite estrito de no máximo 10 revelações ou obtenções de contatos desmascarados por hora por usuário, impedindo raspagem ou disseminação em massa não autorizada.

#### Scenario: Revelação de contato dentro da cota horária
- **WHEN** o operador solicita a revelação de um contato e consumiu menos de 10 na hora corrente
- **THEN** o sistema SHALL liberar a visualização auditada e decrementar a cota disponível

#### Scenario: Exceder o teto de 10 contatos na mesma hora
- **WHEN** o operador tenta revelar ou obter o 11º contato dentro da mesma janela de 1 hora
- **THEN** o sistema SHALL rejeitar a operação com código HTTP 429 Too Many Requests e aviso de limite

### Requirement: Direito ao esquecimento e opt-out
O sistema SHALL garantir o cancelamento de comunicação e exclusão ou anonimização dos dados do titular quando solicitado via mensagem de WhatsApp pelo cliente ou por comando de exclusão do operador no painel.

#### Scenario: Cliente solicita opt-out pelo WhatsApp
- **WHEN** o cliente envia mensagem contendo comando de cancelamento ("SAIR" ou "CANCELAR")
- **THEN** o sistema SHALL registrar o opt-out, bloquear novos envios e expurgar os dados pessoais

#### Scenario: Operador remove contato pelo painel
- **WHEN** o operador confirma a exclusão do contato no painel
- **THEN** o sistema SHALL remover definitivamente o registro e seu índice da base

### Requirement: Retenção e expurgo periódico de contatos
O sistema SHALL executar rotina programada para eliminar contatos inativos cujo período sem interação supere a janela de retenção configurada para a empresa, eliminando dados pessoais obsoletos.

#### Scenario: Execução da rotina de retenção
- **WHEN** o expurgo automático periódico é disparado
- **THEN** o sistema SHALL remover contatos com última interação anterior ao limite de retenção
