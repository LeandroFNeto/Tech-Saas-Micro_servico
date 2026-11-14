# Spec Delta

## ADDED Requirements

### Requirement: DEV-13 API acessível só pelo proxy
A API SHALL ficar inalcançável diretamente pela porta `8080` no IP público do servidor, e `PROXIES_CONFIAVEIS` em produção SHALL listar só o IP exato do proxy.

Prova: `docker compose config`, conferência no deploy

#### Scenario: Acesso direto à porta
- **WHEN** alguém chama a porta `8080` pelo IP público do servidor
- **THEN** a conexão SHALL ser recusada, e a API SHALL continuar respondendo pelo proxy com HTTPS

#### Scenario: Atualização do ambiente sem IP do proxy
- **WHEN** o script de atualização do `.env` recebe Enter na pergunta do IP do proxy
- **THEN** o script SHALL manter o valor atual e SHALL não gravar a faixa inteira da rede Docker

### Requirement: DEV-14 Scripts de ambiente validam o que gravam e não exibem segredo
Os scripts de ambiente SHALL gravar o número da Gamb somente com dígitos, começando em 55, com 12 ou 13 dígitos, e o script de rotação SHALL trocar as chaves de contatos sem exibir nenhum valor.

Prova: execução dos scripts contra um `.env` temporário

#### Scenario: Número da Gamb inválido
- **WHEN** a pessoa informa um número com letras, sem 55 ou com tamanho errado
- **THEN** o script SHALL perguntar de novo e SHALL não gravar o valor

#### Scenario: Valor inválido já gravado
- **WHEN** o `.env` já traz um `GAMB_CONTATO_WHATSAPP` inválido
- **THEN** o script SHALL listar uma pendência sem exibir o valor

#### Scenario: Rotação de chaves preparada
- **WHEN** o script de rotação roda em um `.env` com as chaves atuais
- **THEN** as chaves atuais SHALL ir para as variáveis `_ANTIGA`, as novas SHALL ser geradas e nenhum valor SHALL aparecer na tela

#### Scenario: Rotação já preparada
- **WHEN** o `.env` já traz variáveis `_ANTIGA`
- **THEN** o script SHALL parar sem alterar o arquivo
