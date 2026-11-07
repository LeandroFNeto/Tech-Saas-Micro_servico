# Proposal

## Why

A interação com clientes via canal WhatsApp gera a necessidade operacional de registrar os números de telefone para fins legítimos de atendimento, suporte e agendamento. No entanto, a disponibilização desses dados no painel web (Gamb site) expõe o negócio a riscos de conformidade com a LGPD (Lei 13.709/2018), especialmente no que tange a vazamentos, raspagem automatizada (scraping) e disparos em massa não autorizados. Esta proposta estabelece a arquitetura de proteção de dados, limitadores de consumo, mascaramento visual, gestão de retenção e mecanismos de opt-out para garantir a salvaguarda dos dados pessoais dos clientes.

## What Changes

- **Coleta e Criptografia em Repouso**: Captura do número e nome/identificação do cliente (pushName do WhatsApp ou nome sugerido na conversa) no webhook, armazenando os dados com cifragem reversível (AES) e indexação por HMAC com salt para deduplicação rápida sem exposição em texto plano.
- **Exibição Mascarada por Padrão e Identificação**: Na aba de contatos do painel Gamb, exibe o nome de exibição/sugestão e o telefone mascarado por padrão (exemplo: `+55 (11) 9****-1234`), permitindo saber quem mandou mensagem sem expor o número integralmente.
- **Cota Rígida Anti-Vazamento (Máx 10 Contatos/Hora)**: Trava de proteção que limita a no máximo 10 revelações ou obtenções de contatos completos por hora por usuário, impedindo downloads ou disparos em massa não autorizados.
- **Prevenção de Raspagem e Vazamento em Massa**: Ausência de exportação massiva irrestrita (sem CSV em lote); paginação estrita e bloqueio com HTTP 429 quando a cota horária for atingida.
- **Direito ao Esquecimento e Opt-out (LGPD)**: Mecanismo para exclusão lógica ou anonimização definitiva do contato caso o titular solicite revogação/opt-out (via comando no WhatsApp ou ação explícita pelo operador no painel).
- **Ciclo de Retenção Automatizado**: Eliminação programada de contatos inativos cujo período sem interação ultrapasse a janela de retenção configurada da empresa.

## Capabilities

### New Capabilities
- `contatos`: Gestão do ciclo de vida, armazenamento criptografado, exibição mascarada, governança LGPD e proteção anti-vazamento de contatos de clientes capturados via WhatsApp.

### Modified Capabilities
<!-- Nenhuma especificação de requisito pré-existente sofre alteração de contrato comportamental -->

## Impact

- **Especificação OpenAPI**: Novo grupo de rotas `/api/v1/contatos` em `especificacao/back/` para listagem paginada, consulta pontual e solicitação de exclusão/opt-out.
- **Backend Java**: Expansão de `ServicoContato.java`, criação de `ControllerContato.java`, DTOs seguros com campos mascarados e filtros de taxa/limitação de requisições.
- **Frontend Angular**: Implementação da tela/aba de contatos no painel do cliente (`features/client/contatos`) com tabela paginada, números mascarados e controles de exclusão individual conforme LGPD.
- **Segurança & Infraestrutura**: Uso obrigatório de chaves configuradas em variáveis de ambiente (`CONTATOS_CHAVE_CRIPTO`, `CONTATOS_CHAVE_INDICE`) e expurgo periódico (`RETENCAO_MESES`).
