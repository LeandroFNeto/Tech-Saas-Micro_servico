# Spec Delta

## ADDED Requirements

### Requirement: Rotação das chaves de contatos
O sistema SHALL recifrar nome e WhatsApp dos contatos e o título das reservas, e SHALL recalcular o índice do número, quando as chaves antigas forem informadas no boot, concluindo antes de aceitar tráfego e falhando fechado em configuração incompleta ou dado que não abre.

Prova: `RotacaoChavesContatosTest`

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
