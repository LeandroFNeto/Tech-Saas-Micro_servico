# Design

## Decisão

### 1. Contas de cliente do seed (crítico)
- `seedClientesJaCadastrados` cria a conta com senha aleatória de 32 caracteres (`SecureRandom`), que não é guardada nem logada. O cliente entra depois que o admin define a senha pela redefinição que já existe (`resetarSenhaDoCliente`).
- Bloqueio das contas antigas: no boot, se `SENHA_LEGADA_A_BLOQUEAR` estiver definida, toda conta `CLIENTE` cuja senha confere com esse valor (`passwordEncoder.matches`) recebe uma senha aleatória. O log registra só a quantidade e os e-mails mascarados. A variável é usada num deploy e depois removida do ambiente; o valor nunca volta ao código.
- A constante `SENHA_CLIENTES_LEGADOS` sai.

### 2. Segredos sem padrão (crítico)
- `jwt.secret=${JWT_SECRET_KEY}` sem padrão; o `TokenService` recusa subir com menos de 32 bytes.
- `admin.default.password=${ADMIN_DEFAULT_PASSWORD:}`: vazia, o `AdminInitializer` não cria o admin e avisa no log; com menos de 12 caracteres, a aplicação recusa subir. O padrão de e-mail pode ficar, porque não é segredo.
- `gamb.contato.whatsapp=${GAMB_CONTATO_WHATSAPP:}`, com o número real só no `.env` do servidor. Vazio, a resposta comercial sai sem o link do WhatsApp, como já faz o `AssistenteTutoriaService` com valor vazio.

### 3. Portas e CORS (crítico)
- `docker-compose.yml`: o `db-bot` e o `wppconnect` sem `ports`. Para acessar o banco localmente, um `docker-compose.override.yml`, fora do git, com `127.0.0.1:5432:5432`; o exemplo vai em `docker-compose.override.example.yml`. A API segue em `8080`, atrás do proxy com HTTPS.
- `cors.allowed-origins=${CORS_ALLOWED_ORIGINS:}`. O `CorsConfig` ignora `*` e avisa no log. Vazio, nenhuma origem externa é aceita.

### 4. Superfície HTTP
- `server.error.include-message=never`, `include-stacktrace=never` e `include-binding-errors=never`. Um `@RestControllerAdvice` devolve `{ "message": "<texto pensado para o usuário>" }` para `ResponseStatusException` e para erros de validação, com o texto do próprio sistema. O front continua lendo `erro.error?.message` (`auth.interceptor.ts`, `painel.component.ts`). Exceção não tratada vira 500 com "Erro interno".
- Swagger: `springdoc.api-docs.enabled` e `springdoc.swagger-ui.enabled` leem `SWAGGER_HABILITADO` (padrão `false`); o `SecurityConfig` só libera `/v3/api-docs/**` e `/swagger-ui/**` quando a opção está ligada.
- `x-admin-token` comparado com `MessageDigest.isEqual`.

### 5. Login
- Limite de 5 falhas por e-mail e IP em 15 minutos, em memória. A sexta tentativa responde 429 com `Retry-After`, mesmo com a senha certa, até a janela passar. Login certo dentro do limite zera a contagem. O e-mail não aparece no log, só mascarado.
- Senha nova (`AlterarSenhaDTO`, `ResetSenhaAdminDTO` e criação de cliente) com 10 caracteres ou mais, no DTO e no YAML.

### 6. IP atrás do proxy
- Um componente `IpDoCliente` lê `X-Forwarded-For` só quando `getRemoteAddr` está em `PROXIES_CONFIAVEIS` (lista de IPs ou CIDR); senão usa `getRemoteAddr`. Vale para o `LimitadorPorIp` (assistente público) e para o limite de login. Sem a lista, o comportamento é o de hoje.

### 7. Banco versionado
- Flyway com `V1__linha_de_base.sql` gerado do esquema atual (`pg_dump --schema-only`), `baseline-on-migrate=true` para o banco que já existe e `ddl-auto=validate`. Toda mudança de esquema daqui em diante é uma migração nova, revisada na change.

### 8. Front
- `vercel.json` ganha `headers` para todas as rotas:
  - `Content-Security-Policy`: `default-src 'self'`; `connect-src 'self'` mais a origem da API; `img-src 'self' data: https:`; `style-src 'self' 'unsafe-inline'` mais a origem das fontes; `font-src` com a origem das fontes; `frame-ancestors 'none'`; `base-uri 'self'`; `form-action 'self'`;
  - `X-Frame-Options: DENY`;
  - `X-Content-Type-Options: nosniff`;
  - `Referrer-Policy: strict-origin-when-cross-origin`;
  - `Strict-Transport-Security: max-age=31536000; includeSubDomains`;
  - `Permissions-Policy: camera=(), microphone=(), geolocation=()`.
- As origens exatas (API, fontes, imagens das fotos) saem do inventário da tarefa 6.1 antes de gravar a CSP.

### 9. Fork do WPPConnect
- `config.ts`: `log.level` em `info`.
- `middleware/statusConnection.ts`: sem `console.log(contact)`.
- `util/functions.ts`: a falha do webhook registra só o código HTTP, sem o objeto do erro, que traz o corpo com a mensagem.

## Arquivos
- Contrato: `especificacao/back/paths/auth-login.yaml` (resposta 429 com `Retry-After`), `especificacao/back/schemas/auth/AlterarSenhaDTO.yaml` e o schema de `ResetSenhaAdminDTO` (`minLength: 10`), `$ref` em `especificacao/back/swagger.yaml` se algum schema for novo.
- Java:
  - `config/AdminInitializer.java`, `servico/TokenService.java`, `config/CorsConfig.java`;
  - `security/SecurityConfig.java`, `security/JwtAuthenticationFilter.java`;
  - controller de login e serviço novo de limite de login, `servico/LimitadorPorIp.java`, componente novo `IpDoCliente`;
  - `@RestControllerAdvice` novo, `dto/auth/AlterarSenhaDTO.java`, `dto/empresa/ResetSenhaAdminDTO.java`;
  - `application.properties`, `pom.xml` (Flyway), `src/main/resources/db/migration/V1__linha_de_base.sql`.
- Front: `vercel.json`.
- Infra: `docker-compose.yml`, `docker-compose.override.example.yml`, `.env.example`, `.gitignore` (override), `wppconnect-server/src/config.ts`, `wppconnect-server/src/middleware/statusConnection.ts`, `wppconnect-server/src/util/functions.ts`.
- Prova:
  - `AdminInitializerTest`, `TokenServiceTest`, `CorsConfigTest`;
  - `ControllerAuthTest` (limite e 429), `SecurityConfigTest` (Swagger fechado), `JwtAuthenticationFilterTest`;
  - teste do `@RestControllerAdvice` (sem mensagem interna), `IpDoClienteTest`;
  - `auth.feature` (login bloqueado e senha curta), teste de migração com Postgres do Testcontainers ou `validate` contra o banco de teste;
  - `wppconnect-server/src/tests` para o log.

## Alinhamento
- Stack de `00-architecture.md`. Flyway é dependência nova, com motivo nesta change (regra 10.4 de `seguranca.mdc`).
- DTO nomeado; sem entidade JPA e sem `Map` no payload. O corpo de erro é um DTO nomeado.
- YAML em `especificacao/back/` antes do Java.
- Cumpre as seções 2, 3, 4, 6, 9 e 10 de `.cursor/rules/seguranca.mdc`.

## Riscos
- [Conta legada sem acesso depois do deploy] → avisar cada cliente legado e redefinir a senha pelo admin antes ou logo depois do deploy; o log lista os e-mails mascarados bloqueados.
- [Deploy sem as variáveis novas não sobe] → é a intenção (falhar fechado); conferir o `.env` do servidor com o `.env.example` antes do deploy.
- [Linha de base do Flyway diferente do banco real] → gerar a V1 a partir do banco de produção e rodar `validate` em cópia antes; `baseline-on-migrate` evita reaplicar a V1 no banco existente.
- [CSP bloqueando recurso do front] → inventário das origens antes e conferência do console do navegador em pré-visualização da Vercel.
- [JWT no `localStorage`] → exceção à troca por cookie, com prazo na próxima change de autenticação; mitigado pela CSP sem script externo nem `unsafe-inline` em `script-src`.
- [Limite de login e IP em memória] → reinício zera as contagens; se houver mais de uma instância, cada uma conta à parte.
- [Proxy não configurado em `PROXIES_CONFIAVEIS`] → todos os visitantes do assistente público dividem o limite, como hoje.
