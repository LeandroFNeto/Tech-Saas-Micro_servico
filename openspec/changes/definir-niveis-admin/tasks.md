# Tarefas

## Antes da programação (decisões e alinhamento)

- [ ] Validar a nomenclatura final dos níveis (`SUPER_ADMIN`, `ADMIN_OPERACIONAL`, `ADMIN_LEITURA`).
- [ ] Definir a forma de atribuição do nível inicial para novos administradores.

## Programação

- [ ] Contrato YAML: atualizar `especificacao/back/schemas/auth/LoginResponseDTO.yaml` e documentar restrições de nível em `paths/empresas-sessao.yaml` e `paths/admin-avisos.yaml`
- [ ] Java: atualizar enum `Role.java` e geração de authorities no `JwtAuthenticationFilter.java`
- [ ] Java: seed do master com `SUPER_ADMIN` em `AdminInitializer.java`
- [ ] Java: travas de autorização em `ControllerEmpresa.java` (DELETE restrito a `SUPER_ADMIN`, mutações bloqueadas para `ADMIN_LEITURA`)
- [ ] Java: travas de autorização em `ControllerAviso.java` (gestão restrita a `SUPER_ADMIN`)
- [ ] Front: tipagem em `auth.service.ts` e bloqueio condicional de ações no painel admin
- [ ] Prova: cenários de BDD em `auth.feature` e `empresas.feature`, e testes unitários em `ControllerEmpresaTest` e `ControllerAvisoTest`

## Fechamento (não é da programação)

- [ ] Revisão: atualizar `openspec/specs/auth/spec.md` (novo requisito AUTH-05), `empresa/spec.md` e `avisos/spec.md`
- [ ] Deploy: mover esta pasta para `openspec/archive/definir-niveis-admin/`
