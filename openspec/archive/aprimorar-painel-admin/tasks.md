# Tarefas

## 1. Contrato (OpenAPI First)
- [x] Atualizar `especificacao/back/schemas/EmpresaCreateDTO.yaml`, `EmpresaUpdateAdminDTO.yaml` e `EmpresaResponseDTO.yaml` com o campo `permitePrevisaoTempo`
- [x] Definir DELETE `/empresas/{sessao}` em `especificacao/back/paths/empresas-sessao.yaml`
- [x] Conferir e atualizar `especificacao/back/swagger.yaml` se necessário

## 2. Backend (Java)
- [x] Adicionar `permitePrevisaoTempo` e campos de soft delete (`ativo`, `dataExclusao`) na entidade `Empresa`
- [x] Atualizar DTOs `EmpresaCreateDTO`, `EmpresaUpdateAdminDTO` e `EmpresaResponseDTO`
- [x] Implementar validação no `PUT /empresas/{sessao}/cliente`: bloquear ativação se `permitePrevisaoTempo == false`
- [x] Atualizar `ServicoPrevisaoTempo` / controller para abortar consulta externa se `permitePrevisaoTempo == false`
- [x] Implementar soft delete em `ServicoEmpresa` e `ControllerEmpresa` (DELETE `/empresas/{sessao}`) desconectando o WhatsApp e marcando `ativo = false`
- [x] Filtrar empresas inativas nas listagens padrão

## 3. Frontend (Angular)
- [x] Atualizar `especificacao/front/cliente-painel.content.json`
- [x] Atualizar formulário admin de empresa (`empresa-form`) com o toggle `permitePrevisaoTempo`
- [x] Adicionar botão e modal de confirmação de exclusão da empresa no painel administrativo
- [x] Atualizar tela do painel do cliente (`/cliente/painel`): toggle de previsão desabilitado com aviso explicativo quando `permitePrevisaoTempo == false`

## 4. Prova (Testes)
- [x] Cenários Cucumber em `empresas.feature`:
  - Bloqueio de ativação da previsão pelo cliente quando não liberada pelo admin
  - Empresas existentes com previsão mantêm funcionamento
  - Consulta de previsão retorna `disponivel = false` sem bater na API externa quando permissão é revogada
  - Exclusão lógica (soft delete) da empresa e desconexão da sessão
- [x] Testes unitários JUnit em `ServicoPrevisaoTempoTest` e `ControllerEmpresaTest`
- [x] Testes de componentes Angular no painel do cliente e no dashboard admin

## 5. Fechamento
- [x] Atualizar `openspec/specs/empresa/spec.md` com os novos requisitos
- [x] Atualizar `openspec/central-tarefas.md`
- [x] Mover esta pasta para `openspec/archive/aprimorar-painel-admin/`
