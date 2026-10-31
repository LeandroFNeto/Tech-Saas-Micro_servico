# Tarefas

## Antes da programação (decisões e alinhamento)

- [x] Validar a nomenclatura final dos níveis (`SUPER_ADMIN`, `ADMIN_OPERACIONAL`, `ADMIN_LEITURA`).
- [x] Definir a forma de atribuição do nível inicial para novos administradores.

## Programação

- [x] Contrato YAML: atualizar `especificacao/back/schemas/auth/LoginResponseDTO.yaml` e documentar restrições de nível em `paths/empresas-sessao.yaml` e `paths/admin-avisos.yaml`
- [x] Java: atualizar enum `Role.java` e geração de authorities no `JwtAuthenticationFilter.java`
- [x] Java: seed do master com `SUPER_ADMIN` em `AdminInitializer.java`
- [x] Java: travas de autorização em `ControllerEmpresa.java` (DELETE restrito a `SUPER_ADMIN`, mutações bloqueadas para `ADMIN_LEITURA`)
- [x] Java: travas de autorização em `ControllerAviso.java` (gestão restrita a `SUPER_ADMIN`)
- [x] Front: tipagem em `auth.service.ts` e bloqueio condicional de ações no painel admin
- [x] Prova: cenários de BDD em `auth.feature` e `empresas.feature`, e testes unitários em `ControllerEmpresaTest` e `ControllerAvisoTest`

## Fechamento (não é da programação)

- [x] Revisão: atualizar `openspec/specs/auth/spec.md` (novo requisito AUTH-05), `empresa/spec.md` e `avisos/spec.md`
- [x] Deploy: mover esta pasta para `openspec/archive/definir-niveis-admin/`
