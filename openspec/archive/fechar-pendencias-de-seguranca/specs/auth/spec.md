# Spec Delta

## ADDED Requirements

### Requirement: AUTH-06 Limite de tentativas de login
O sistema SHALL limitar o login a 5 falhas por e-mail e IP em 15 minutos, respondendo 429 com `Retry-After` até a janela passar, e zerar a contagem quando o login der certo.

Contrato: `especificacao/back/paths/auth-login.yaml`
Prova: `ControllerAuthTest`, `src/test/resources/features/auth.feature`

#### Scenario: Senha errada repetida
- **WHEN** o mesmo e-mail erra a senha 5 vezes do mesmo IP em 15 minutos
- **THEN** a sexta tentativa SHALL responder 429 com `Retry-After`, mesmo com a senha certa

#### Scenario: Login certo zera a contagem
- **GIVEN** que o e-mail errou a senha 3 vezes
- **WHEN** o login dá certo
- **THEN** a contagem de falhas desse e-mail e IP SHALL voltar a zero

#### Scenario: IP atrás de proxy confiável
- **GIVEN** que o pedido chega por um proxy listado em `PROXIES_CONFIAVEIS`
- **WHEN** o limite é calculado
- **THEN** o IP usado SHALL ser o do `X-Forwarded-For`; vindo de outro endereço, o cabeçalho SHALL ser ignorado

#### Scenario: E-mail fora do log
- **WHEN** o login é recusado ou bloqueado
- **THEN** o log SHALL trazer o e-mail só mascarado e nunca a senha

### Requirement: AUTH-07 Nenhuma conta com senha conhecida
O sistema SHALL criar contas sem senha conhecida, exigir senha nova com 10 caracteres ou mais e não subir com senha do admin curta ou chave do JWT ausente ou curta.

Contrato: `especificacao/back/schemas/auth/AlterarSenhaDTO.yaml`
Prova: `AdminInitializerTest`, `TokenServiceTest`, `src/test/resources/features/auth.feature`

#### Scenario: Seed de cliente legado
- **WHEN** o boot cria a conta de uma empresa já cadastrada
- **THEN** a senha SHALL ser aleatória, sem aparecer no log, e o cliente SHALL entrar só depois de o admin definir a senha

#### Scenario: Conta com a senha antiga
- **GIVEN** que `SENHA_LEGADA_A_BLOQUEAR` está definida no boot
- **WHEN** uma conta de cliente confere com esse valor
- **THEN** a senha SHALL ser trocada por uma aleatória e o log SHALL trazer só a quantidade e os e-mails mascarados

#### Scenario: Admin inicial sem senha configurada
- **WHEN** `ADMIN_DEFAULT_PASSWORD` está vazia
- **THEN** o admin inicial SHALL não ser criado e o log SHALL avisar; com menos de 12 caracteres a aplicação SHALL não subir

#### Scenario: Chave do JWT
- **WHEN** `JWT_SECRET_KEY` falta ou tem menos de 32 bytes
- **THEN** a aplicação SHALL não subir

#### Scenario: Senha nova curta
- **WHEN** a troca ou a redefinição de senha recebe menos de 10 caracteres
- **THEN** a API SHALL responder 400

### Requirement: AUTH-08 Superfície HTTP mínima
O sistema SHALL responder erro sem mensagem interna nem rastro de pilha, aceitar só as origens de CORS configuradas, manter a documentação da API fechada sem a opção ligada e comparar o `x-admin-token` em tempo constante.

Contrato: nenhum novo (respostas de erro de todos os paths)
Prova: `SecurityConfigTest`, `CorsConfigTest`, `JwtAuthenticationFilterTest`, teste do tratador de erros

#### Scenario: Exceção interna
- **WHEN** uma rota lança uma exceção não tratada
- **THEN** a API SHALL responder 500 com "Erro interno", sem a mensagem nem o rastro da exceção

#### Scenario: Erro pensado para o usuário
- **WHEN** uma rota recusa o pedido com uma mensagem do próprio sistema
- **THEN** o corpo SHALL trazer essa mensagem em `message`, que o front continua mostrando

#### Scenario: CORS
- **WHEN** um navegador de origem fora de `CORS_ALLOWED_ORIGINS` chama a API
- **THEN** a API SHALL recusar a origem; `*` na configuração SHALL ser ignorado com aviso no log

#### Scenario: Documentação fechada
- **WHEN** `SWAGGER_HABILITADO` não está `true`
- **THEN** `/v3/api-docs` e `/swagger-ui` SHALL responder 401 ou 404
