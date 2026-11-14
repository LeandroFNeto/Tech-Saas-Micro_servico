# fechar-pendencias-de-seguranca

## Problema

A revisão de segurança que criou `.cursor/rules/seguranca.mdc` achou pontos do projeto que descumprem as regras. Os mais graves abrem acesso sem invasão nenhuma, só com o que está no repositório:

- **Senha fixa no código para contas de cliente.** `AdminInitializer.SENHA_CLIENTES_LEGADOS` é a senha de toda conta de cliente criada pelo seed, e o e-mail dessas contas é previsível (`<sessao>@gamb.com.br`). Quem lê o código entra no painel de qualquer empresa legada que não trocou a senha.
- **Valores padrão para segredos.** `admin.default.password` (`SenhaMaster123`) e `jwt.secret` têm padrão em `application.properties` e no `AdminInitializer`. Sem a variável, o sistema sobe com a senha do admin e a chave do JWT conhecidas; com a chave do JWT, qualquer um assina um token de admin.
- **Banco e gateway expostos.** O `docker-compose.yml` publica `5432` (Postgres) e `21465` (WPPConnect) no host.
- **CORS aberto.** `cors.allowed-origins` vale `*` sem a variável.

Também há pontos de risco alto e médio:
- `server.error.include-message=always` devolve a mensagem interna do erro;
- o login não tem limite de tentativas;
- o `x-admin-token` é comparado com `equals`;
- o `ddl-auto=update` vale em produção;
- o Swagger fica público;
- o front não tem cabeçalhos de segurança;
- a senha mínima é de 6 caracteres;
- o limitador por IP usa o IP do proxy;
- o fork do WPPConnect registra número e payload no log;
- `gamb.contato.whatsapp` tem um número real como padrão.

## O que muda

### Entra
- Conta de cliente do seed sem senha conhecida: senha aleatória que ninguém vê, com redefinição pelo admin. Bloqueio, no primeiro boot, das contas que ainda usam a senha antiga.
- Limite de tentativas de login por e-mail e IP, com 429.
- IP do cliente lido do proxy só quando o proxy está na lista de confiáveis, valendo para o login e para o assistente público.
- Cabeçalhos de segurança na hospedagem do front.
- Migração de banco versionada (Flyway) com a linha de base do esquema atual.

### Altera
- Segredos sem valor padrão: a aplicação não sobe sem `JWT_SECRET_KEY` (32 bytes ou mais), e sem `ADMIN_DEFAULT_PASSWORD` (12 caracteres ou mais) o admin inicial não é criado e um aviso vai para o log.
- CORS só com as origens configuradas; `*` é recusado.
- Erro sem mensagem interna; as mensagens para o usuário saem só dos tratadores do próprio sistema.
- `x-admin-token` comparado em tempo constante.
- Swagger desligado por padrão, ligado só com `SWAGGER_HABILITADO=true`.
- `ddl-auto` passa a `validate`, e o esquema muda só por migração.
- Senha nova de cliente e do admin com 10 caracteres ou mais.
- `docker-compose.yml` sem porta publicada para o banco e o gateway.
- Fork do WPPConnect com log em `info` e sem número nem payload.
- `gamb.contato.whatsapp` sem padrão no repositório.

### Sai
- A constante `SENHA_CLIENTES_LEGADOS` e os padrões de senha e de chave do JWT.

## Fora de escopo

- Trocar o JWT do `localStorage` por cookie `httpOnly` (pede proteção CSRF e mudança no front). Fica como exceção nos Riscos, mitigada pela CSP.
- Remover o cabeçalho legado `x-admin-token`.
- Autenticação em dois fatores.
- A migração para o Baileys, que já prevê o gateway sem porta e sem log de dado pessoal; aqui só se corrige o fork atual.

## Domínios
- spec: `openspec/specs/auth/spec.md`
- spec: `openspec/specs/desenvolvimento/spec.md`
- spec: `openspec/specs/whatsapp/spec.md`
- regra: `.cursor/rules/seguranca.mdc`
