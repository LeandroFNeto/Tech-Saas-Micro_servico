# Tarefas

## 1. Contrato
- [x] 1.1 `auth-login.yaml`: resposta 429 com cabeçalho `Retry-After`; `AlterarSenhaDTO.yaml` e o schema do reset de senha com `minLength: 10`; corpo de erro como schema nomeado, se ainda não houver; `$ref` em `swagger.yaml`

## 2. Críticos
- [x] 2.1 `AdminInitializer`: seed de cliente com senha aleatória de 32 caracteres, sem log; bloqueio das contas que conferem com `SENHA_LEGADA_A_BLOQUEAR` quando a variável existir, com log só da quantidade e dos e-mails mascarados; remover `SENHA_CLIENTES_LEGADOS`; verificar com `AdminInitializerTest`
- [x] 2.2 `AdminInitializer` e `application.properties`: sem padrão para a senha do admin; vazia não cria o admin e avisa no log, com menos de 12 caracteres não sobe; verificar com `AdminInitializerTest`
- [x] 2.3 `TokenService` e `application.properties`: `jwt.secret` sem padrão e recusa com menos de 32 bytes; verificar com `TokenServiceTest`
- [x] 2.4 `docker-compose.yml` sem `ports` no banco e no gateway; `docker-compose.override.example.yml` com o banco em `127.0.0.1`; `.gitignore` com `docker-compose.override.yml`; verificar com `docker compose config`
- [x] 2.5 `CorsConfig` e `application.properties`: sem padrão, `*` ignorado com aviso; verificar com `CorsConfigTest`

## 3. Superfície HTTP
- [x] 3.1 `include-message`, `include-stacktrace` e `include-binding-errors` em `never`; `@RestControllerAdvice` com o corpo de erro nomeado para `ResponseStatusException` e validação, e "Erro interno" no 500; verificar que o front continua mostrando as mensagens do sistema (`auth.interceptor.ts`, `painel.component.ts`) e que exceção interna não vaza
- [x] 3.2 Swagger por `SWAGGER_HABILITADO` (padrão `false`) e `SecurityConfig` liberando a documentação só com a opção ligada; verificar com `SecurityConfigTest` (401 com a opção desligada)
- [x] 3.3 `JwtAuthenticationFilter`: `x-admin-token` com `MessageDigest.isEqual`; verificar com `JwtAuthenticationFilterTest`

## 4. Login e IP
- [x] 4.1 `IpDoCliente` com `PROXIES_CONFIAVEIS` e uso no `LimitadorPorIp`; verificar com `IpDoClienteTest` (proxy confiável, proxy não confiável, sem lista)
- [x] 4.2 Limite de 5 falhas por e-mail e IP em 15 min, 429 com `Retry-After`, contagem zerada no login certo; verificar com `ControllerAuthTest` e cenário em `auth.feature`
- [x] 4.3 Senha nova com 10 caracteres ou mais nos DTOs; verificar com teste de 400 em `auth.feature` e no teste do reset

## 5. Banco
- [x] 5.1 Flyway no `pom.xml`, `V1__linha_de_base.sql` gerado do esquema de produção, `baseline-on-migrate=true` e `ddl-auto=validate`; verificar subindo a aplicação contra uma cópia do banco e com o teste de integração do esquema (verificação pendente em 9.3)

## 6. Front
- [x] 6.1 Inventariar as origens usadas pelo front (API, fontes, imagens das fotos, scripts) e gravar a CSP e os demais cabeçalhos em `vercel.json`; verificar numa pré-visualização da Vercel sem erro de CSP no console, com login, painel, QR Code e galeria funcionando (verificação pendente em 9.3)

## 7. Gateway atual
- [x] 7.1 `wppconnect-server`: `log.level` em `info`, sem `console.log(contact)` em `statusConnection.ts` e sem o objeto do erro na falha do webhook em `functions.ts`; verificar com teste em `wppconnect-server/src/tests` e conferindo o log do container (verificação pendente em 9.3)
- [x] 7.2 `wppconnect-server`: `returnError` e o `/send-mentioned` em `messageController.ts` (sem `error: error` na resposta 500) e os demais `logger.error` do caminho de envio e do webhook (`statusConnection.ts`, `functions.ts`) registram só a descrição segura do erro (`descreverErroSeguro`: tipo, `code`, HTTP), e a resposta 500 não devolve mais o objeto do erro; verificado com `returnErrorLog.test.ts` (jest); log do container ainda a conferir (verificação pendente em 9.3)

## 8. Configuração
- [x] 8.1 `application.properties`: `gamb.contato.whatsapp` sem padrão; `.env.example` com `SWAGGER_HABILITADO`, `PROXIES_CONFIAVEIS`, `SENHA_LEGADA_A_BLOQUEAR` e `GAMB_CONTATO_WHATSAPP` com exemplos falsos; verificar relendo o arquivo

## 9. Fechamento
- [x] 9.1 `mvn test` completo e `npm test -- --watch=false --browsers=ChromeHeadless` em `frontend/`
- [x] 9.2 Conferir a change contra `.cursor/rules/seguranca.mdc`, seção por seção, na etapa de Segurança
- [ ] 9.3 Deploy e verificação: subir a aplicação com `validate` contra uma cópia do banco de produção (pg_dump) antes de ligar em produção; conferir o console de uma pré-visualização da Vercel (login, painel, QR Code, galeria e upload do Cloudinary) sem erro de CSP; conferir o log do container wppconnect sem número; conferir no `.env` do servidor `JWT_SECRET_KEY` (32 bytes ou mais), `CORS_ALLOWED_ORIGINS` e `PROXIES_CONFIAVEIS` com a faixa real da rede Docker; rodar uma vez com `SENHA_LEGADA_A_BLOQUEAR`, redefinir a senha dos clientes legados avisados e remover a variável
- [x] 9.4 Sincronizar `auth`, `desenvolvimento` e `whatsapp` em `openspec/specs/` à mão (títulos sem o prefixo `Requirement:`) e mover esta pasta para `openspec/archive/fechar-pendencias-de-seguranca/` (spec gravada pela revisão; DEV-11 já existia, o requisito entrou como DEV-12; falta mover a pasta no deploy)
