# ia-cliente-refletir-base-de-conhecimento

## Problema

A IA que atende o cliente final no WhatsApp (empresas com `usaIA` ligado) não passa a ideia do GAMB descrita em `docs/base-conhecimento-gamb.md`. O prompt (`ServicoIA.construirPromptEmpresa`) tem só o nome, o ramo, os preços, as regras e o mapa da empresa, e uma lista de instruções fixa. Lendo o código:

- **Comportamento engessado.** A IA responde como roteiro: o prompt limita a 2 a 3 frases curtas, manda cumprimentar "calorosamente pelo nome da empresa" e fechar sempre com o mesmo lembrete de opções. O resultado é uma resposta repetida e mecânica, parecida com um menu com outras palavras, e não com uma pessoa atendendo. O documento pede atendimento humanizado (US18, RF10).
- **Data só no formato exato.** Quando o cliente pergunta uma data, o bot só aceita `DD-MM-AAAA` (ou com `/`). "25 12 2026", "25.12", "25 de dezembro", "dia 25", "amanhã" ou "sábado" recebem "Não entendi essa data", mesmo que o cliente tenha pedido algo simples, e uma data no meio de uma frase cai na IA, que não consulta a agenda.
- **Menu inventado.** A instrução 4 manda sempre lembrar "1 para ver a agenda, 2 para pesquisar data ou 3 para solicitar reserva". O documento (US01, RF01) diz que o menu é dinâmico e só tem as opções ativas da empresa. Numa empresa sem agenda, ou com a ordem diferente, a IA indica a opção errada.
- **Sem a ideia do produto.** A IA não sabe que o atendimento existe para responder rápido, evitar conflito de horário e levar o cliente à reserva pela Google Agenda (seção 1.1), nem que reserva é uma solicitação registrada e conferida, não uma confirmação dada por ela.
- **Sem a base de verdade.** As diretrizes da seção 7 do documento (responder só com o que está cadastrado, não inventar, não expor credencial nem ID de agenda, orientar passo a passo) valem para a IA do GAMB, mas a IA do cliente final não recebe equivalente.
- **Sem o que a empresa oferece.** Os módulos ativos (fotos, localização, preços, reserva, cardápio) e o modo de locação (diária ou por hora) não entram no prompt, então a IA não sabe o que pode oferecer.

## O que muda

### Entra
- Um texto de orientação do atendimento ao cliente final, extraído do documento base (ideia do produto e regras de conduta), guardado em `src/main/resources/assistente/` e carregado pela IA.
- Persona humana no prompt: uma pessoa da equipe da empresa atendendo, que lê a intenção do cliente, responde primeiro o que foi perguntado, acompanha o tom dele (formal ou informal), varia o jeito de falar e só lembra do menu quando ajuda, não em toda resposta.
- Leitura de data em qualquer jeito de escrever (`25/12/2026`, `25-12-26`, `25.12`, `25 12 2026`, `25 de dezembro`, `2026-12-25`, `amanhã`, `depois de amanhã`, `hoje`), inclusive no meio de uma frase. A data entendida vai para o sistema do bot (consulta na agenda ou reserva), sem a IA inventar disponibilidade.
- Quando a data é duvidosa (só o dia, dia da semana, data sem ano que já passou neste ano, data impossível), o bot pergunta "você quer dizer sábado, 07-11-2026?" e segue com o *sim* ou com a data corrigida.
- Prompt montado com o menu real da empresa (as opções ativas e a numeração do `ServicoMenu`), os módulos ativos e o modo de locação.
- Regras de conduta no prompt: usar só dados cadastrados, não inventar preço, horário nem disponibilidade, não confirmar reserva (só orientar a solicitá-la pelo menu), não expor credencial nem identificador de agenda, encaminhar ao atendente humano quando não souber.

### Altera
- `ServicoIA.construirPromptEmpresa`: troca o roteiro rígido (2 a 3 frases, sempre fechar com o menu) por orientação de tom; troca a instrução fixa de opções 1, 2 e 3 pelo menu real; acrescenta ideia do produto, módulos e conduta.
- IA-04 e IA-05 continuam valendo; entram os requisitos IA-07 e IA-08.
- `LocacaoStrategy`: o estado de espera de data passa a usar o leitor de datas e ganha o estado de confirmação.

### Sai
- A frase fixa "digite 1, 2 ou 3" e o fechamento obrigatório com o lembrete do menu em toda resposta.

## Capabilities

### Modified Capabilities
- `ia`: requisitos novos IA-07 (a IA do cliente final reflete o documento base e o menu real da empresa) e IA-08 (data em qualquer formato, com pergunta na dúvida).

## Suposições

- "Documento base" é `docs/base-conhecimento-gamb.md`; "o ponto tratado lá" é lido como as diretrizes da seção 7 e as histórias do cliente final (US01 a US06) e a ideia da seção 1. Se era outro ponto, ajustar antes do apply.
- O documento tem partes internas (painel, admin, diagnóstico) que o cliente final não deve ver; só o recorte do atendimento vai para o prompt.
- Datas são lidas no fuso `America/Sao_Paulo`, no padrão brasileiro dia/mês. Data sem ano usa o ano corrente e, se já passou, pergunta se é o ano seguinte. Número solto ("25") não é data.
- Humano não quer dizer longo: a resposta continua curta para WhatsApp (poucas linhas), sem teto rígido de frases.
- Sem mudar o modelo, a cota nem o timeout (IA-06 segue igual).

## Fora de escopo

- A atendente da Gamb e o bot do login e da apresentação.
- Memória de conversa entre mensagens, novos módulos ou mudar o modelo de IA.
- Dar preço ou prometer prazo; continua proibido.

## Impacto

- Back: `ServicoIA`, `ServicoMenu` (leitura do menu), novo recurso `assistente/atendimento-empresa.md`.
- Spec: `openspec/specs/ia/spec.md`.
- Testes: `ServicoIATest`, `LocacaoStrategyTest`.
