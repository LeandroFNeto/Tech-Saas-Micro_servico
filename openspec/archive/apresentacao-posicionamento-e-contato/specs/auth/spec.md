## ADDED Requirements

### Requirement: Apresentação do posicionamento da Gamb
A página pública de apresentação SHALL conter a seção "Quem somos", que diz que a Gamb é uma startup formada por um grupo pequeno de desenvolvedores, que atende por plano de assinatura com acompanhamento contínuo, cresce junto com o negócio do cliente e atende poucos clientes por vez. A página SHALL NOT informar valor nem faixa de preço, SHALL NOT prometer prazo, vaga ou garantia e SHALL NOT citar nome de empresa cliente. A página SHALL manter-se sem consumir API.

Contrato: nenhum. Rota do front, sem rota de API.
Prova: `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts`

#### Scenario: Visitante lê quem é a Gamb
- **GIVEN** um visitante sem sessão
- **WHEN** abre `/`
- **THEN** vê a seção "Quem somos" com a Gamb como startup, grupo pequeno, plano de assinatura e poucos clientes por vez

#### Scenario: Página não informa preço nem prazo
- **GIVEN** um visitante sem sessão
- **WHEN** abre `/`
- **THEN** o texto da página não contém "R$", valor de plano, prazo de implantação nem nome de cliente

#### Scenario: Página sem chamada de API
- **GIVEN** um visitante sem sessão
- **WHEN** abre `/` sem abrir o chat
- **THEN** a página não faz nenhuma requisição à API

### Requirement: Separação entre o que existe hoje e o que está em desenvolvimento
A página de apresentação SHALL separar o que a Gamb entrega hoje do que está em desenvolvimento. "Hoje" SHALL listar o assistente virtual no WhatsApp com conversa humanizada por IA, o controle de reserva automático pela Google Agenda, fotos, preços, regras e avisos, e o painel no celular. "Em desenvolvimento" SHALL listar a nova conexão com outras funcionalidades do WhatsApp do cliente, a integração com o que ele já usa, as automações, o IoT e a análise das câmeras de segurança que ele já tem. O item de câmeras SHALL dizer que é ferramenta de apoio à decisão, que analisa sem guardar informação de ninguém e que serve à eficiência do estabelecimento e à satisfação do cliente. Nenhum item em desenvolvimento SHALL ser apresentado como produto pronto.

Contrato: nenhum. Rota do front, sem rota de API.
Prova: `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts`

#### Scenario: Visitante vê o que existe hoje
- **GIVEN** um visitante sem sessão
- **WHEN** abre `/` e chega à seção "Hoje e o que vem"
- **THEN** vê o assistente virtual, a reserva automática pela Google Agenda e a conversa humanizada por IA como entregas de hoje

#### Scenario: Visitante vê o que está em desenvolvimento
- **GIVEN** um visitante sem sessão
- **WHEN** abre `/` e chega à seção "Hoje e o que vem"
- **THEN** vê integração, automações, IoT e câmeras de segurança sob o selo "Em desenvolvimento"

#### Scenario: Câmeras apresentadas como apoio à decisão
- **GIVEN** um visitante sem sessão
- **WHEN** lê o item de câmeras
- **THEN** o texto cita tempo de espera e tempo médio de atendimento, diz que não guarda informação de ninguém e não afirma que o produto esteja pronto nem em conformidade legal

### Requirement: Contato da Gamb por WhatsApp na apresentação
A página de apresentação SHALL oferecer o contato da Gamb por WhatsApp no cabeçalho, em uma seção de contato e no rodapé, com o número `(43) 99816-5047` escrito. Todo botão ou link de contato SHALL levar a `https://wa.me/5543998165047` em nova aba. A seção de contato SHALL dizer que o plano de assinatura é combinado em conversa. O número SHALL coincidir com o do chat do site.

Contrato: nenhum. Rota do front, sem rota de API.
Prova: `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts`

#### Scenario: Botão de contato no cabeçalho
- **GIVEN** um visitante sem sessão
- **WHEN** abre `/`
- **THEN** o cabeçalho mostra o botão "Falar com a Gamb no WhatsApp" que leva a `https://wa.me/5543998165047` em nova aba

#### Scenario: Seção de contato completa
- **GIVEN** um visitante sem sessão
- **WHEN** chega à seção de contato
- **THEN** vê o botão do WhatsApp, o número `(43) 99816-5047` escrito e a frase de que o plano é combinado em conversa

#### Scenario: Número igual ao do chat
- **GIVEN** a página de apresentação e o chat do site
- **WHEN** ambos indicam o contato da Gamb
- **THEN** o número é o mesmo, `5543998165047`

#### Scenario: Celular
- **GIVEN** um visitante em tela de 390px
- **WHEN** abre `/`
- **THEN** o botão de contato do cabeçalho cabe sem rolagem horizontal e o botão da seção de contato tem alvo de toque de pelo menos 44px
