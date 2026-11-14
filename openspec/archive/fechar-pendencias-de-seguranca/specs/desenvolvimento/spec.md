# Spec Delta

## ADDED Requirements

### Requirement: DEV-12 Regras de segurança e configuração segura
O desenvolvimento SHALL seguir `.cursor/rules/seguranca.mdc`, e a configuração de produção SHALL falhar fechada: segredo sem valor padrão, serviço interno sem porta publicada, esquema do banco só por migração versionada e front com cabeçalhos de segurança.

Prova: `docker compose config`, teste de esquema com Flyway em `validate`, pré-visualização da Vercel, etapa de Segurança da `horadoshow`

#### Scenario: Segredo ausente
- **WHEN** um segredo obrigatório falta no ambiente
- **THEN** a aplicação SHALL não subir ou o recurso SHALL ficar desligado com aviso no log, nunca com um valor padrão

#### Scenario: Serviços internos
- **WHEN** o `docker-compose.yml` sobe
- **THEN** o banco e o gateway do WhatsApp SHALL ficar sem porta publicada no host

#### Scenario: Mudança de esquema
- **WHEN** uma change altera o banco
- **THEN** a mudança SHALL vir numa migração Flyway nova, e a aplicação SHALL validar o esquema no boot sem alterá-lo

#### Scenario: Cabeçalhos do front
- **WHEN** a Vercel entrega qualquer página do front
- **THEN** a resposta SHALL trazer `Content-Security-Policy`, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy` e `Strict-Transport-Security`

#### Scenario: Change sem cumprir uma regra
- **WHEN** uma change não consegue cumprir uma regra de `seguranca.mdc`
- **THEN** a exceção SHALL estar nos Riscos do `design.md` com motivo e prazo, ou a etapa de Segurança SHALL reprovar
