# Spec Delta

## ADDED Requirements

### Requirement: AUTH-09 Entrada de credenciais com tamanho máximo
O sistema SHALL recusar com 400 o login com e-mail acima de 254 caracteres ou senha acima de 128, e SHALL recusar senha nova acima de 128 caracteres.

Contrato: `especificacao/back/schemas/auth/LoginRequestDTO.yaml`, `especificacao/back/paths/auth-login.yaml`
Prova: `ControllerAuthTest`, `src/test/resources/features/auth.feature`

#### Scenario: E-mail longo demais
- **WHEN** o login recebe e-mail com 255 caracteres
- **THEN** a API SHALL responder 400 sem consultar o usuário e sem contar falha

#### Scenario: Senha longa demais
- **WHEN** o login recebe senha com 129 caracteres
- **THEN** a API SHALL responder 400 sem calcular BCrypt

#### Scenario: Senha nova longa demais
- **WHEN** a troca, a redefinição ou a criação de senha recebe mais de 128 caracteres
- **THEN** a API SHALL responder 400

### Requirement: AUTH-10 Login sem revelar se o e-mail existe
O sistema SHALL executar a mesma comparação BCrypt no login quando o e-mail não existe, respondendo o mesmo 401 usado para senha errada.

Contrato: `especificacao/back/paths/auth-login.yaml`
Prova: `ServicoAuthTest`

#### Scenario: E-mail inexistente
- **WHEN** o login recebe um e-mail que não está cadastrado
- **THEN** o sistema SHALL comparar a senha com um hash de referência de mesmo custo e SHALL responder 401 "Senha incorreta"

### Requirement: AUTH-11 Limite de login com memória e CPU limitadas
O sistema SHALL manter no máximo 10.000 entradas de contagem de falhas, descartando as de falha mais antiga, e SHALL limitar cada IP a 30 falhas em 15 minutos quando `PROXIES_CONFIAVEIS` estiver preenchida.

Contrato: `especificacao/back/paths/auth-login.yaml`
Prova: `LimiteTentativasLoginTest`, `ControllerAuthTest`

#### Scenario: Teto de entradas
- **GIVEN** que o limite tem 10.000 entradas válidas
- **WHEN** uma falha de uma combinação nova de e-mail e IP é registrada
- **THEN** a entrada com a falha mais antiga SHALL sair e o total SHALL continuar em 10.000

#### Scenario: E-mails diferentes do mesmo IP
- **GIVEN** que `PROXIES_CONFIAVEIS` está preenchida
- **WHEN** um IP acumula 30 falhas em 15 minutos com e-mails diferentes
- **THEN** a tentativa seguinte SHALL responder 429 com `Retry-After`, sem calcular BCrypt

#### Scenario: Lista de proxies vazia
- **WHEN** `PROXIES_CONFIAVEIS` está vazia
- **THEN** o teto por IP SHALL não valer e o boot SHALL registrar um aviso no log

### Requirement: AUTH-12 IP do cliente somente de endereço literal
O sistema SHALL interpretar `X-Forwarded-For` e `PROXIES_CONFIAVEIS` apenas como endereço IP literal e SHALL nunca fazer consulta DNS para esses valores.

Prova: `LiteralDeIpTest`, `IpDoClienteTest`

#### Scenario: Nome de host no cabeçalho
- **WHEN** o `X-Forwarded-For` de um proxy confiável traz `cafe.de`
- **THEN** o valor SHALL ser recusado sem consulta DNS e o sistema SHALL usar o IP da conexão

#### Scenario: Forma abreviada ou com zero à esquerda
- **WHEN** o valor é `1.2.3` ou `010.0.0.1`
- **THEN** o valor SHALL ser recusado como IP

#### Scenario: IPv4 dentro de IPv6
- **GIVEN** que `PROXIES_CONFIAVEIS` lista `10.0.0.1`
- **WHEN** a conexão vem de `::ffff:10.0.0.1`
- **THEN** o endereço SHALL ser tratado como `10.0.0.1` e o proxy SHALL ser reconhecido como confiável
