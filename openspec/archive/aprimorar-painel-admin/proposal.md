# aprimorar-painel-admin

## Problema

1. **Controle de Previsão do Tempo pelo Admin**: Atualmente, qualquer cliente pode ativar a previsão do tempo no perfil se informar uma cidade, gerando chamadas à API externa sem controle do administrador do SaaS. É necessário que o admin controle se a empresa tem acesso a esse recurso (`permitePrevisaoTempo`). Uma vez liberado pelo admin, o cliente pode ativar ou desativar na sua sessão (nascendo desativado em novas empresas/liberações até o cliente definir a localização e ligar). Se não estiver liberado pelo admin, a API externa não deve ser consultada.
2. **Exclusão/Desativação de Empresa**: O painel admin não possui ação para excluir/desativar uma empresa cancelada, mantendo dados ativos e sessões zumbis do WhatsApp em execução. A exclusão física direta violaria integridade com agendamentos e tabelas de auditoria (LGPD).

## O que muda

### Entra
- Campo `permitePrevisaoTempo` gerenciado pelo admin no cadastro e na edição da empresa (`EmpresaUpdateAdminDTO` e `EmpresaResponseDTO`).
- Regra de negócio: se liberado pelo admin, nasce desativado (`previsaoAtiva = false`) até o cliente informar a cidade e ligar no perfil; empresas existentes que já têm a previsão ativa permanecem ativas.
- Checagem prévia no backend antes de consultar a API externa de previsão: se `permitePrevisaoTempo == false`, responde imediatamente `disponivel = false`.
- Ajuste no perfil do cliente (`/cliente/perfil`): toggle de previsão habilitado apenas se o admin tiver liberado a empresa; caso contrário, exibe aviso explicativo e campo bloqueado.
- Exclusão lógica (Soft Delete) de empresa: endpoint DELETE `/empresas/{sessao}` com `ativo = false`, data de exclusão e desconexão imediata da sessão WhatsApp.
- Botão e modal de confirmação de exclusão da empresa no painel administrativo.

### Altera
- `EmpresaCreateDTO`, `EmpresaUpdateAdminDTO` e `EmpresaResponseDTO` passam a contemplar `permitePrevisaoTempo`.
- PUT `/empresas/{sessao}/cliente`: rejeita ativação de previsão se o admin não tiver liberado `permitePrevisaoTempo`.
- `GET /empresas/{sessao}/previsao`: não consome a API da OpenWeather se a empresa não tiver permissão.
- `openspec/specs/empresa/spec.md`: requisitos EMP-01, EMP-02, EMP-05 e novos requisitos de exclusão/desativação e permissão de previsão.

### Sai
- Consulta à API externa de previsão para empresas sem permissão concedida pelo admin.
- Necessidade de intervenção direta no banco de dados para desativar empresas e desconectar seus bots.

## Fora de escopo
- Sistema de múltiplos perfis/escopos de administradores (RBAC) — delegado para change própria futura.
- Redesenho completo e filtros avançados do painel de administração — delegado para change própria futura.
- Exclusão física/purge de histórico de auditoria (`AuditoriaAcessoContato`).

## Domínios
- spec: `openspec/specs/empresa/spec.md`
