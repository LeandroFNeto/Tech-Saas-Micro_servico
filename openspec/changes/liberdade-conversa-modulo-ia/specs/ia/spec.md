# Spec Delta

## MODIFIED Requirements

### Requirement: IA-06 Resiliência e fallback para menu determinístico
O sistema SHALL realizar chamadas ao modelo leve de IA respeitando a cota gratuita da API: aplicar timeout de 3 segundos, limitar as chamadas por minuto e por dia com tetos configuráveis, limitar o gasto estimado por dia (fuso de Brasília) a um teto em reais, com padrão de R$ 15,00, calculado pelos tokens informados em cada resposta do modelo, pausar novas chamadas por um período configurável depois de erro de cota (429) e cair em fallback determinístico caso a API falhe, estoure o timeout, esteja pausada ou o teto tenha sido atingido. O fallback nunca trava o atendimento.

#### Scenario: falha ou esgotamento de cota da IA aciona menu padrão
- **WHEN** a chamada ao modelo Gemini falhar, estourar timeout ou retornar erro de cota (429)
- **THEN** o sistema SHALL responder com o menu principal determinístico sem travar o atendimento

#### Scenario: teto de chamadas atingido
- **WHEN** o teto por minuto ou por dia de chamadas ao modelo foi atingido
- **THEN** o sistema SHALL seguir o fallback determinístico sem chamar o modelo

#### Scenario: teto de gasto diário atingido
- **WHEN** o gasto estimado do dia chega ao teto em reais configurado
- **THEN** o sistema SHALL seguir o fallback determinístico sem chamar o modelo até o dia seguinte e registrar o aviso no log sem dado de cliente

#### Scenario: gasto recomeça no dia seguinte
- **WHEN** o dia muda no fuso de Brasília
- **THEN** o gasto estimado SHALL voltar a zero e o modelo SHALL voltar a ser chamado

#### Scenario: pausa depois do erro de cota
- **WHEN** o modelo retornou 429 e o período de pausa ainda não terminou
- **THEN** o sistema SHALL seguir o fallback sem chamar o modelo e voltar a chamá-lo quando a pausa terminar

## ADDED Requirements

### Requirement: IA-09 Conversa livre com conversa curta local e cota protegida
O sistema SHALL permitir que a IA converse com liberdade, como uma pessoa do negócio, nas sessões que usam o módulo de IA. Cumprimentos e conversa curta (oi, bom dia, boa tarde, boa noite, tudo bem, obrigado, tchau) SHALL ser respondidos sem chamar o modelo, com mais de uma frase possível por situação e sem repetir a mesma frase em mensagens seguidas do mesmo contato. Texto livre fora das opções do menu SHALL seguir para a conversa livre em vez de resultar só em menu ou "Opção inválida"; a IA SHALL adaptar a conversa ao negócio de quem fala, usando só dados cadastrados e sem inventar preço, horário, regra ou disponibilidade. Sem modelo disponível (IA-06), o sistema SHALL responder uma mensagem humana local com o convite a digitar *menu*. Perguntas idênticas SHALL reaproveitar a resposta recente sem nova chamada ao modelo. Mensagens que são opção numérica, "menu", "voltar", "sair", "cancelar", "sim" e "não" continuam tratadas pelo fluxo, sem conversa livre.

#### Scenario: cumprimento sem chamar o modelo
- **WHEN** o cliente escreve "bom dia" ou "oi, tudo bem?" a uma empresa com IA
- **THEN** o sistema SHALL responder um cumprimento natural e oferecer ajuda, sem chamar o modelo

#### Scenario: agradecimento e despedida
- **WHEN** o cliente escreve "obrigado" ou "valeu, tchau"
- **THEN** o sistema SHALL responder cordialmente e deixar a porta aberta, sem chamar o modelo

#### Scenario: variação entre mensagens seguidas
- **WHEN** o mesmo contato cumprimenta duas vezes seguidas
- **THEN** o sistema SHALL responder duas frases diferentes

#### Scenario: texto livre no menu da empresa
- **WHEN** o cliente de uma empresa com IA escreve algo fora das opções do menu
- **THEN** o sistema SHALL seguir a conversa livre e não responder "Opção inválida"

#### Scenario: adaptar a conversa ao negócio
- **WHEN** o prompt da conversa livre é montado
- **THEN** o prompt SHALL orientar a conversa dentro do ramo e dos dados cadastrados do negócio, sem inventar dado

#### Scenario: sem modelo, resposta humana local
- **WHEN** o modelo está indisponível, pausado ou no teto de chamadas
- **THEN** o sistema SHALL responder uma mensagem humana local convidando a digitar *menu*

#### Scenario: pergunta repetida reaproveita a resposta
- **WHEN** a mesma pergunta chega de novo dentro da validade da resposta guardada
- **THEN** o sistema SHALL reaproveitar a resposta sem nova chamada ao modelo

#### Scenario: opção do menu não vira conversa
- **WHEN** o cliente digita um número de opção do menu
- **THEN** o sistema SHALL tratar a opção pelo fluxo e não chamar a conversa livre
