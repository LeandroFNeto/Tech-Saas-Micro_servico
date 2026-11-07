# Proposal

## Why
Atualmente, o bot de WhatsApp opera exclusivamente sob árvores rígidas de opções numéricas e comandos exatos ("Digite 1, 2 ou 3"). Quando clientes enviam saudações ("Bom dia", "Boa tarde") ou perguntas em linguagem natural ("tem vaga pro próximo sábado?"), o bot não compreende ou reitera o menu inicial de forma mecânica. Esta mudança integra um modelo leve de IA (Google Gemini em cota gratuita / free-tier) para humanizar o atendimento inicial, responder saudações e dúvidas básicas da empresa e direcionar o cliente com base nas informações exclusivas da sua sessão, sem gerar custos de API.

## What Changes
- Integração do `ServicoIA` ao fluxo de entrada de mensagens do WhatsApp para empresas que possuem a flag `usaIA = true`.
- Atendimento humanizado a saudações ("Bom dia", "Boa tarde", "Olá") e perguntas gerais sobre a empresa (horários, localização, regras e serviços).
- Interpretação de períodos e intenções em linguagem natural (ex.: "próximo sábado", "fim de semana") aproveitando a tabela/árvore de disponibilidade já calculada pelo sistema.
- Isolamento estrito multi-tenant: o prompt da IA é abastecido exclusivamente com os dados da empresa dona da sessão ativa (nome, regras, tabela de preços, endereço/mapa), sem vazamento entre empresas e sem exposição de chaves ou calendários.
- Resiliência e Fallback de Custo Zero: limitação de chamadas para respeitar a cota gratuita da API Gemini, com timeout rápido (3 segundos) e fallback silencioso para o menu determinístico padrão caso a cota se esgote ou a API falhe.

## Capabilities

### New Capabilities

### Modified Capabilities
- `ia`: Adiciona requisitos observáveis para conversação humanizada via WhatsApp, injeção de contexto restrito da empresa da sessão, triagem de saudações e fallback de segurança da IA.

## Impact
- **Backend Java**: `ServicoIA`, `ControllerWhatsapp`, estratégias de atendimento (`LocacaoStrategy`, etc.) e DTOs de contexto.
- **APIs externas**: Consumo da API REST gratuita do Google Gemini com prompt enxuto e parametrização segura (`GEMINI_API_KEY`).
- **Banco de dados**: Utilização do campo `usaIA` já existente na entidade `Empresa`.
- **Desempenho e Custos**: Custo zero de LLM através de cache de contexto, prompts concisos e respeito aos limites da cota gratuita do Gemini.
