# Design

## Context
O sistema possui a classe `ServicoIA` que encapsula chamadas à API REST do Google Gemini via `RestTemplate`, e a entidade `Empresa` já conta com a flag `usaIA`. No entanto, o `ControllerWhatsapp` e as estratégias de atendimento (`LocacaoStrategy`) ainda não utilizam esse serviço, operando apenas por comandos fixos do menu. Veja `proposal.md` para a motivação.

## Goals / Non-Goals

**Goals:**
- Conectar o `ServicoIA` ao fluxo do WhatsApp quando a empresa estiver com `usaIA = true` e o usuário em estado `INICIO`.
- Alimentar a IA com um prompt conciso contendo unicamente os dados públicos da empresa dona da sessão (nome, regras, tabela de preços, horários cadastrados, fotos e mapas).
- Responder saudações e dúvidas comuns mantendo a cota gratuita da API Gemini (baixo volume de tokens e modelo flash).
- Garantir fallback imediato para o menu numérico padrão em caso de timeout (> 3s), erro 429 (rate limit) ou falha na API.

**Non-Goals:**
- Não substituir o motor transacional de reserva (bloqueio de data, checagem no Google Agenda e persistência continuam determinísticos).
- Não criar assistente com histórico longo de memória para não estourar a cota gratuita de tokens.

## Decisions

### 1. Injeção de Contexto Modular por Sessão
- **Decisão:** Criar um montador de contexto (`PromptEmpresaBuilder`) que extrai os dados sanitizados da `Empresa` (nome, ramo, regras de locação, tabela de preços, link de fotos/mapa) sem dados sensíveis (`googleCalendarId`, senhas, tokens).
- **Alternativa considerada:** Fazer queries GraphQL dinâmicas a cada mensagem. Rejeitado por overhead de latência desnecessário; a entidade `Empresa` já está carregada em memória na sessão.

### 2. Uso do Modelo Flash Leve na Cota Gratuita
- **Decisão:** Configurar URL padrão para `gemini-1.5-flash` ou `gemini-2.5-flash` no `application.properties` com temperatura baixa (0.2) e resposta concisa (máximo 150 tokens).
- **Alternativa considerada:** Usar modelos pesados (Pro/Ultra). Rejeitado por custo e limite restrito de requisições por minuto no tier gratuito.

### 3. Integração com Timeout e Fallback Não-Bloqueante
- **Decisão:** Configurar timeout de conexão e leitura de 3000ms no `RestTemplate` do `ServicoIA`. Em caso de exceção ou cota estourada, o bot responde com o menu tradicional sem interromper a jornada do cliente.
- **Alternativa considerada:** Bloquear o fluxo até a IA responder. Rejeitado para não degradar a experiência do WhatsApp com esperas longas.

## Risks / Trade-offs

- **[Cota gratuita excedida (HTTP 429)]** → *Mitigação:* O `ServicoIA` captura `HttpClientErrorException.TooManyRequests` e cai silenciosamente no menu padrão determinístico.
- **[Alucinação sobre regras ou valores]** → *Mitigação:* System prompt instrui explicitamente a responder estritamente com os dados fornecidos e, se não souber, direcionar para o atendente ou menu.
