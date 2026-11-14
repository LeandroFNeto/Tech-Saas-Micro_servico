# Spec Delta

## MODIFIED Requirements

### Requirement: Diagnóstico integrado de integridade dos módulos
O sistema SHALL disponibilizar endpoint e rotina de diagnóstico administrativo para checar a saúde operacional de todos os módulos principais do sistema (auth, avisos, agenda, empresa, whatsapp e ia), retornando o status consolidado de cada serviço. O módulo ia SHALL aparecer como degradado, com o motivo, quando a guarda de cota (IA-06) suspende o modelo por pausa após 429, teto de gasto diário, teto de chamadas por dia ou teto de chamadas por minuto, sem dado de cliente na mensagem. O módulo whatsapp SHALL refletir a rota de saúde do gateway: indisponível quando o gateway não responde e degradado quando alguma sessão de empresa ativa não está conectada, só com nomes de sessão e sem número. Sessão em `INITIALIZING` (reconectando) conta como desconectada, então o degradado pode aparecer por alguns segundos durante uma reconexão.

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

#### Scenario: Gateway do WhatsApp fora do ar
- **GIVEN** que a rota de saúde do gateway não responde
- **WHEN** o administrador executa a checagem geral de módulos
- **THEN** o módulo whatsapp SHALL aparecer como indisponível e o status geral SHALL ser crítico

#### Scenario: Sessão de empresa desconectada
- **GIVEN** que o gateway responde e uma sessão de empresa ativa não está conectada
- **WHEN** o administrador executa a checagem geral de módulos
- **THEN** o módulo whatsapp SHALL aparecer como degradado com a quantidade e os nomes das sessões que não estão entre as conectadas informadas pelo gateway (aguardando QR, reconectando ou desconhecidas), sem número de telefone

#### Scenario: Gateway sem rota de saúde
- **GIVEN** que o backend aponta para o WPPConnect, que não tem a rota de saúde
- **WHEN** o administrador executa a checagem geral de módulos
- **THEN** o módulo whatsapp SHALL aparecer como operacional com a mensagem "gateway sem rota de saúde"
