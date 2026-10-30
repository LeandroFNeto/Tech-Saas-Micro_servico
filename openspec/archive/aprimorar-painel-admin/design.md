# Design

## Decisão

1. **Permissão de Previsão do Tempo (`permitePrevisaoTempo`)**:
   - Adicionar `permitePrevisaoTempo` (boolean, default `false`) na entidade `Empresa`. Em empresas pré-existentes no banco que já possuem `previsaoAtiva == true`, `permitePrevisaoTempo` é mantido como `true` para preservar o funcionamento.
   - Para empresas novas ou quando o admin ativa `permitePrevisaoTempo = true`, a previsão da empresa nasce desativada (`previsaoAtiva = false`) até o cliente definir a cidade e salvar no `/cliente/perfil`.
   - Se `permitePrevisaoTempo == false`:
     - O endpoint `GET /empresas/{sessao}/previsao` nem chega a invocar o serviço da OpenWeather; retorna imediatamente HTTP 200 com `disponivel = false` e motivo explicativo.
     - O PUT `/empresas/{sessao}/cliente` rejeita qualquer tentativa de enviar `previsaoAtiva = true` com HTTP 400 ("A previsão do tempo precisa ser liberada pelo administrador.").
   - No frontend do cliente (`/cliente/perfil`): se `permitePrevisaoTempo == false`, o card exibe o toggle desabilitado com aviso claro de que o recurso precisa ser contratado/liberado pelo admin.

2. **Exclusão Lógica de Empresa (Soft Delete) com Desconexão de WhatsApp**:
   - Endpoint: DELETE `/empresas/{sessao}` protegido por `x-admin-token` ou papel `ADMIN`.
   - Comportamento:
     - Marca `ativo = false` e registra `dataExclusao = LocalDateTime.now()` na entidade `Empresa`.
     - Invoca `ServicoWhatsapp.desconectar(sessao)` para encerrar a sessão do WhatsApp imediatamente.
     - Listagens do admin (`GET /empresas`) passam a ignorar empresas inativas por padrão.
     - Usuários vinculados à empresa têm o login bloqueado.
     - Preserva integridade referencial com reservas, dados financeiros e registros de `AuditoriaAcessoContato` (atendendo à LGPD).
   - No frontend do admin (`/admin` e `/admin/empresas/{sessao}`):
     - Botão "Excluir empresa" com modal de confirmação.

## Arquivos
- Contrato:
  - `especificacao/back/paths/empresas-admin.yaml` (atualizado com `permitePrevisaoTempo`)
  - `especificacao/back/paths/empresas.yaml` (DELETE `/empresas/{sessao}` e novos campos no schema)
  - `especificacao/back/schemas/EmpresaUpdateAdminDTO.yaml`
  - `especificacao/back/schemas/EmpresaResponseDTO.yaml`
  - `especificacao/back/schemas/EmpresaCreateDTO.yaml`
- Java:
  - `src/main/java/com/example/demo/model/Empresa.java`
  - `src/main/java/com/example/demo/dto/EmpresaCreateDTO.java`
  - `src/main/java/com/example/demo/dto/EmpresaUpdateAdminDTO.java`
  - `src/main/java/com/example/demo/dto/EmpresaResponseDTO.java`
  - `src/main/java/com/example/demo/controller/ControllerEmpresa.java`
  - `src/main/java/com/example/demo/servico/ServicoEmpresa.java`
  - `src/main/java/com/example/demo/servico/previsao/ServicoPrevisaoTempo.java`
- Front:
  - `especificacao/front/cliente-perfil.content.json`
  - `especificacao/front/admin-empresa-form.content.json`
  - `frontend/src/app/features/client/perfil/perfil.component.html`
  - `frontend/src/app/features/client/perfil/perfil.component.ts`
  - `frontend/src/app/features/admin/dashboard/dashboard.component.html`
  - `frontend/src/app/features/admin/empresa-form/empresa-form.component.html`
  - `frontend/src/app/features/admin/empresa-form/empresa-form.component.ts`
- Prova:
  - `src/test/resources/features/empresas.feature` (cenários de soft delete e controle de permissão da previsão)
  - `src/test/java/com/example/demo/servico/previsao/ServicoPrevisaoTempoTest.java`
  - `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`
  - `frontend/src/app/features/client/perfil/perfil.component.spec.ts`
  - `frontend/src/app/features/admin/empresa-form/empresa-form.component.spec.ts`

## Alinhamento
- Stack de `00-architecture.md` (Java 21, Spring Boot 3, Angular 19+, Tailwind).
- DTO nomeado; sem entidade JPA e sem `Map` no payload.
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`.
- Sem comentários no código (`//` ou `/* */`).

## Riscos
- **Empresas existentes no banco**: na migração/atualização, empresas que já possuem previsão ativa devem ter `permitePrevisaoTempo = true` para evitar quebra no ambiente em execução.
- **Sessão WhatsApp desconectada**: a rotina de exclusão deve tratar adequadamente eventuais timeouts ao chamar o serviço de desconexão do WhatsApp para não impedir a gravação do soft delete no banco.
