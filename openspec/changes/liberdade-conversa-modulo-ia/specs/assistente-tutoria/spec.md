# Spec Delta

## MODIFIED Requirements

### Requirement: Diagnóstico integrado de integridade dos módulos
O sistema SHALL disponibilizar endpoint e rotina de diagnóstico administrativo para checar a saúde operacional de todos os módulos principais do sistema (auth, avisos, agenda, empresa, whatsapp e ia), retornando o status consolidado de cada serviço. O módulo ia SHALL aparecer como degradado, com o motivo, quando a guarda de cota (IA-06) suspende o modelo por pausa após 429, teto de gasto diário, teto de chamadas por dia ou teto de chamadas por minuto, sem dado de cliente na mensagem.

Contrato: `especificacao/back/paths/admin-assistente-tutoria-diagnostico.yaml`
Prova: `DiagnosticoModulosServiceTest`, `AdminAssistenteTutoriaControllerTest`

#### Scenario: Execução de diagnóstico com todos os módulos saudáveis
- **GIVEN** que o administrador autenticado solicita a checagem geral de módulos
- **WHEN** a requisição de diagnóstico é processada
- **THEN** o sistema responde status 200 com a lista de módulos contendo status operacional positivo para todos os serviços

#### Scenario: Detecção de módulo com falha no diagnóstico
- **GIVEN** que o serviço de integração com WhatsApp ou IA está temporariamente indisponível
- **WHEN** o administrador executa a checagem geral de módulos
- **THEN** o sistema responde indicando o módulo afetado com status de falha e mensagem explicativa, sem interromper a checagem dos demais

#### Scenario: IA suspensa pela guarda de cota
- **GIVEN** que a guarda de cota suspendeu o modelo
- **WHEN** o administrador executa a checagem geral de módulos
- **THEN** o módulo ia SHALL aparecer como degradado com o motivo da suspensão e o status geral SHALL ser de atenção
