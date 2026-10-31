# Design

## Decisão

1. **Enum de Papéis (`Role`):**
   - Evoluir `com.example.demo.model.Role` para suportar:
     - `SUPER_ADMIN` (com `ADMIN` mantido como alias/equivalente para compatibilidade)
     - `ADMIN_OPERACIONAL`
     - `ADMIN_LEITURA`
     - `CLIENTE`
   - O JWT gerado no login inclui a claim `role` com o nível específico correspondente.

2. **Matriz de Autorização por Nível:**
   - **`SUPER_ADMIN` / `ADMIN`:**
     - Acesso a todas as rotas e métodos (`GET`, `POST`, `PUT`, `DELETE`).
     - Apenas este nível pode executar `DELETE /empresas/{sessao}`.
     - Apenas este nível pode gerenciar `/admin/avisos` (`POST`, `PUT`, `DELETE`).
     - Apenas este nível pode executar reset de senha de empresas (`POST /empresas/{sessao}/reset-senha`).
   - **`ADMIN_OPERACIONAL`:**
     - Pode listar e consultar empresas (`GET /empresas`, `GET /empresas/{id}`, etc.).
     - Pode criar e atualizar configurações de empresas (`POST /empresas`, `PUT /empresas/{sessao}/admin`).
     - Pode configurar funcionamento e dias especiais (`PUT /empresas/{sessao}/funcionamento`, etc.).
     - Responde com `403 Forbidden` caso tente excluir empresa ou criar avisos globais.
   - **`ADMIN_LEITURA`:**
     - Pode apenas consultar (`GET`) rotas administrativas (`/empresas`, `/empresas/{sessao}`, `/whatsapp/{sessao}/status`, `/avisos`).
     - Responde com `403 Forbidden` para qualquer operação `POST`, `PUT` ou `DELETE`.

3. **Mapeamento Spring Security Authorities:**
   - Usuário `SUPER_ADMIN` recebe: `ROLE_ADMIN`, `ROLE_SUPER_ADMIN`.
   - Usuário `ADMIN_OPERACIONAL` recebe: `ROLE_ADMIN`, `ROLE_ADMIN_OPERACIONAL`.
   - Usuário `ADMIN_LEITURA` recebe: `ROLE_ADMIN`, `ROLE_ADMIN_LEITURA`.
   - Ao manter `ROLE_ADMIN` como autoridade base compartilhada, todas as rotas que hoje apenas exigem "ser admin" continuam funcionando sem quebra imediata, e as rotas críticas aplicam restrições adicionais específicas (ex.: `@PreAuthorize("hasRole('SUPER_ADMIN')")`).

4. **Compatibilidade com Header Legado `x-admin-token`:**
   - O cabeçalho estático `x-admin-token` configurado via variável de ambiente confere nível `SUPER_ADMIN`.

5. **Front-end Admin (Angular):**
   - O `AuthService` expõe o nível específico do admin (`papel: 'SUPER_ADMIN' | 'ADMIN_OPERACIONAL' | 'ADMIN_LEITURA' | 'CLIENTE'`).
   - Telas administrativas aplicam diretivas estruturais ou signals para desabilitar/ocultar botões de exclusão para quem não é `SUPER_ADMIN`, e desabilitar formulários de edição para quem for `ADMIN_LEITURA`.

## Arquivos
- Contrato:
  - `especificacao/back/schemas/auth/LoginResponseDTO.yaml` (atualização do enum e documentação dos papéis)
  - `especificacao/back/paths/empresas-sessao.yaml` (DELETE documenta exigência de SUPER_ADMIN)
  - `especificacao/back/paths/admin-avisos.yaml` (documenta exigência de SUPER_ADMIN)
- Java:
  - `model/Role.java`
  - `config/AdminInitializer.java`
  - `security/JwtAuthenticationFilter.java`
  - `servico/ServicoAcessoEmpresa.java`
  - `controller/ControllerEmpresa.java`
  - `controller/ControllerAviso.java`
- Front:
  - `frontend/src/app/core/services/auth.service.ts`
  - `frontend/src/app/features/admin/` (ajuste de botões e permissões na visualização)
- Prova:
  - `src/test/resources/features/auth.feature` (cenários de login por nível)
  - `src/test/resources/features/empresas.feature` (cenários de permissão DELETE e PUT por nível)
  - `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`
  - `src/test/java/com/example/demo/controller/ControllerAvisoTest.java`

## Alinhamento
- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java
- Sem quebra dos fluxos existentes (compatibilidade com `ADMIN` e `x-admin-token`)

## Riscos
- **Compatibilidade com clientes legados:** Qualquer sistema que dependa estritamente da string `"ADMIN"` no payload de resposta do login precisa ser preservado; por isso `ADMIN` e `SUPER_ADMIN` devem ser tratados de forma interoperável.
- **Auditoria de segurança:** Evitar vazamento de privilégios onde um `ADMIN_OPERACIONAL` consiga alterar o próprio papel ou de outros para `SUPER_ADMIN`.
