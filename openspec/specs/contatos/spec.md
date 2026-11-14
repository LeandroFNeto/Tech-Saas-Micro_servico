# contatos Specification

## Purpose
Gerencia o ciclo de vida, persistência criptografada, conformidade LGPD, exibição mascarada e prevenção contra raspagem e vazamento de contatos de clientes capturados pelo canal de atendimento WhatsApp.

## Requirements

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
O sistema SHALL garantir o cancelamento de comunicação e exclusão ou anonimização dos dados do titular quando solicitado via mensagem de WhatsApp pelo cliente ou por comando de exclusão do operador no painel. No opt-out, a linha do contato SHALL guardar só o índice do número e a marca de não contatar.

Contrato: `especificacao/back/paths/empresas-contatos-numero.yaml`
Prova: ainda não escrita; prevista em `ServicoContatoTest`, `ServicoRespostaLembreteTest` e `ControllerClienteAutomacaoTest` (change `expurgar-contato-no-opt-out`).

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

### Requirement: Retenção e expurgo periódico de contatos
O sistema SHALL executar rotina programada para eliminar contatos inativos cujo período sem interação supere a janela de retenção configurada para a empresa, eliminando dados pessoais obsoletos.

#### Scenario: Execução da rotina de retenção
- **WHEN** o expurgo automático periódico é disparado
- **THEN** o sistema SHALL remover contatos com última interação anterior ao limite de retenção

### Requirement: Rotação das chaves de contatos
O sistema SHALL recifrar nome e WhatsApp dos contatos e o título das reservas, e SHALL recalcular o índice do número, quando as chaves antigas forem informadas no boot, concluindo antes de aceitar tráfego e falhando fechado em configuração incompleta ou dado que não abre.

Prova: ainda não escrita; prevista em `RotacaoChavesContatosTest` (change `endurecer-login-e-gateway`).

#### Scenario: Rotação completa
- **GIVEN** que as chaves atuais e as variáveis `CONTATOS_CHAVE_CRIPTO_ANTIGA` e `CONTATOS_CHAVE_INDICE_ANTIGA` estão definidas
- **WHEN** a aplicação sobe
- **THEN** os valores SHALL ficar cifrados com a chave nova, a busca pelo número SHALL encontrar o contato pelo índice novo e o log SHALL trazer só as quantidades por tabela

#### Scenario: Retomada depois de falha
- **GIVEN** que uma rotação parou no meio
- **WHEN** a aplicação sobe de novo com as mesmas variáveis
- **THEN** as linhas já rotacionadas SHALL ficar como estão e as demais SHALL ser rotacionadas

#### Scenario: Valor que não abre com nenhuma chave
- **WHEN** um valor cifrado não abre com a chave nova nem com a antiga
- **THEN** a aplicação SHALL não subir e o log SHALL trazer só a tabela e o `id`, sem o conteúdo

#### Scenario: Configuração incompleta
- **WHEN** só uma das variáveis antigas está definida, ou a antiga é inválida, ou é igual à atual
- **THEN** a aplicação SHALL não subir e nenhuma linha SHALL ser alterada

#### Scenario: Sem chave antiga
- **WHEN** as duas variáveis antigas estão vazias
- **THEN** nenhuma linha SHALL ser lida nem alterada pela rotação

#### Scenario: Nenhuma chave padrão
- **WHEN** as chaves de contatos não estão configuradas
- **THEN** a gravação de contatos SHALL ficar desligada com aviso no log, sem usar valor escrito no código
