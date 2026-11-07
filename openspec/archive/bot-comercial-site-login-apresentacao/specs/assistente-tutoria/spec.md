## ADDED Requirements

### Requirement: Assistente comercial no login e na apresentação
O sistema SHALL responder o visitante do login e da apresentação em modo comercial, a partir de uma base de contexto da empresa: quem é a Gamb (uma startup que fecha negócio pelo WhatsApp), o que ela entrega hoje, o modelo de assinatura com acompanhamento contínuo e como começar. O assistente SHALL NOT incluir dado de empresa no prompt, SHALL NOT informar valor nem faixa de preço e SHALL NOT citar nome de empresa cliente. Pergunta de custo e de contratação SHALL levar ao WhatsApp da Gamb. Assunto fora da Gamb SHALL receber a recusa padrão. Esta regra prevalece sobre o Suporte público no que toca ao conteúdo das respostas do login e da apresentação; o painel e a tela de Tutoriais seguem no Suporte.

Contrato: `especificacao/back/paths/publico-assistente-suporte.yaml`
Prova: `AssistenteTutoriaServiceTest`, `PublicoAssistenteSuporteTest`

#### Scenario: Visitante pergunta quem é a Gamb
- **GIVEN** um visitante sem sessão
- **WHEN** envia "Quem é a Gamb?" ao endpoint público
- **THEN** a resposta conta que a Gamb é uma startup, um grupo pequeno de desenvolvedores que atende por assinatura e acompanha o negócio do cliente, sem dado de empresa

#### Scenario: Visitante pergunta o que a Gamb oferece
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta o que a Gamb oferece
- **THEN** a resposta apresenta o assistente virtual no WhatsApp, o controle de reserva automático pela Google Agenda e a conversa humanizada com IA como a entrega de hoje

#### Scenario: Visitante pergunta o custo
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta quanto custa
- **THEN** a resposta diz que o plano de assinatura é combinado, indica o WhatsApp da Gamb e não contém valor nem faixa de preço

#### Scenario: Visitante pede nome de clientes
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta quais empresas já usam a Gamb
- **THEN** a resposta não cita nenhuma empresa e indica o WhatsApp da Gamb

#### Scenario: Assunto fora da Gamb
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta algo que não é sobre a Gamb
- **THEN** a resposta é a recusa padrão

#### Scenario: Modelo fora do ar
- **GIVEN** o modelo de IA indisponível
- **WHEN** o visitante pergunta o custo
- **THEN** o texto de reserva mantém preço sob consulta e o caminho do WhatsApp da Gamb

#### Scenario: Contato configurado
- **GIVEN** a propriedade do WhatsApp da Gamb preenchida com 5543998165047
- **WHEN** a resposta indica o contato
- **THEN** o texto traz o link `https://wa.me/5543998165047`

#### Scenario: Contato sem número configurado
- **GIVEN** a propriedade do WhatsApp da Gamb vazia
- **WHEN** a resposta indica o contato
- **THEN** o texto diz "WhatsApp da Gamb" sem número e sem link

### Requirement: Leque de soluções apresentado sem prometer o que não existe
O assistente comercial SHALL distinguir o que a Gamb entrega hoje do que pode vir. Integração com as ferramentas que o cliente já usa, automações, IoT e análise das câmeras de segurança que o cliente já tem SHALL ser apresentados como caminho avaliado caso a caso com o cliente, nunca como produto pronto, e o assistente SHALL NOT prometer funcionalidade, prazo ou garantia dessas soluções. Pergunta sobre entrar como cliente SHALL ser respondida dizendo que a Gamb atende poucos clientes por vez e começa por uma conversa pelo WhatsApp, sem confirmar vaga nem cadastrar o visitante.

Contrato: `especificacao/back/paths/publico-assistente-suporte.yaml`
Prova: `AssistenteTutoriaServiceTest`

#### Scenario: Visitante pergunta de IoT
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta se a Gamb trabalha com IoT
- **THEN** a resposta diz que isso é avaliado conforme a necessidade do negócio, não afirma que já exista produto pronto e indica o WhatsApp da Gamb

#### Scenario: Visitante pergunta de integração com o que já usa
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta se a Gamb integra com os sistemas que ele já usa
- **THEN** a resposta apresenta a integração como caminho conversado caso a caso, sem prometer prazo

#### Scenario: Visitante pergunta que ferramentas a Gamb tem
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta quais ferramentas a Gamb oferece além do assistente
- **THEN** a resposta diz que o assistente virtual é a solução de hoje, que as demais estão em desenvolvimento e que o leque cresce conforme a necessidade dos clientes, sem listar ferramenta futura como disponível

#### Scenario: Visitante pergunta de câmeras
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta se a Gamb consegue usar as câmeras de segurança do comércio dele para medir o atendimento
- **THEN** a resposta diz que isso é avaliado caso a caso sobre o sistema que ele já tem, cita tempo de espera e tempo médio de atendimento como exemplo do que se pode medir, explica que é uma ferramenta de decisão que analisa sem guardar informação de ninguém, para melhorar a eficiência do estabelecimento e a satisfação do cliente, não afirma que exista produto pronto nem conformidade legal e indica o WhatsApp da Gamb

#### Scenario: Visitante pede uma funcionalidade a mais junto ao WhatsApp
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta se a Gamb consegue ligar outra funcionalidade ao WhatsApp dele
- **THEN** a resposta diz que a Gamb monta uma nova conexão conforme a necessidade, sobre o que ele já usa, sem prometer prazo, e indica o WhatsApp da Gamb

#### Scenario: Visitante pergunta como virar cliente
- **GIVEN** um visitante sem sessão
- **WHEN** pergunta como contratar ou se há vaga
- **THEN** a resposta diz que a Gamb atende poucos clientes por vez, convida para uma conversa no WhatsApp e não confirma vaga nem prazo

### Requirement: Sugestões do chat público por tela
O sistema SHALL mostrar sugestões e saudação inicial próprias no login e na apresentação, diferentes das do Suporte do painel. O request público SHALL aceitar o campo opcional `origem` com os valores `LOGIN` e `APRESENTACAO`, e a resposta SHALL devolver as sugestões da origem. Sem `origem`, SHALL valer `APRESENTACAO`. Valor inválido SHALL responder HTTP 400. O painel e a tela de Tutoriais SHALL manter as sugestões atuais.

Contrato: `especificacao/back/schemas/assistente/SuportePublicoRequestDTO.yaml`
Prova: `PublicoAssistenteSuporteTest`, `assistente-cliente.component.spec.ts`, `apresentacao.component.spec.ts`, `login.component.spec.ts`

#### Scenario: Sugestões da apresentação
- **GIVEN** um visitante na apresentação com o chat aberto
- **WHEN** o painel do chat é exibido
- **THEN** as sugestões falam de quem é a Gamb, o que ela oferece, o plano de assinatura e como começar, sem QR Code nem Google Agenda do painel

#### Scenario: Sugestões do login
- **GIVEN** um visitante na tela de login com o chat aberto
- **WHEN** o painel do chat é exibido
- **THEN** as sugestões falam de entrar no painel, senha esquecida e como falar com a Gamb

#### Scenario: Origem informada na resposta
- **GIVEN** um request com `origem` igual a `LOGIN`
- **WHEN** o endpoint responde
- **THEN** as sugestões devolvidas são as do login

#### Scenario: Origem inválida
- **GIVEN** um request com `origem` fora dos valores aceitos
- **WHEN** o endpoint é chamado
- **THEN** o sistema responde HTTP 400

#### Scenario: Painel do cliente mantém as sugestões
- **GIVEN** um cliente logado com o chat aberto
- **WHEN** o painel do chat é exibido
- **THEN** as sugestões continuam as do Suporte do app
