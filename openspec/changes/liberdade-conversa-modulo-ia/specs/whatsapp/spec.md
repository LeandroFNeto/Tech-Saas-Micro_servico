# Spec Delta

## MODIFIED Requirements

### Requirement: WPP-12 Sessão da atendente da Gamb no WhatsApp
O sistema SHALL entregar a mensagem da empresa interna de ramo reservado `ATENDENTE_GAMB` à `AtendenteGambStrategy`, sem mudar o roteamento nem as strategies das empresas clientes (WPP-01). Todo contato dessa sessão é tratado como cliente. A primeira mensagem, e `0` em qualquer passo, abre o menu "Tirar uma dúvida" (`1`), "Marcar reunião" (`2`) ou "Conhecer a Gamb" (`3`); quando a primeira mensagem é um cumprimento, o bot cumprimenta de volta antes do menu (IA-09). Na opção `3`, e no texto livre que pergunta do que se trata a Gamb, o que ela oferece ou como ela resolve o problema do cliente (no menu e na dúvida), o bot responde pela base comercial da Gamb: quem é, o que existe hoje e o que está em desenvolvimento, ligando o problema descrito ao que existe hoje, sem informar valor ou faixa de preço, sem prometer prazo, vaga ou garantia, sem citar cliente nem comparar com concorrente, e termina convidando a digitar *2* para marcar reunião. Assunto fora da Gamb recebe a recusa comercial. Na dúvida, o bot responde pelo Suporte de `AssistenteTutoriaService`, só com a base de conhecimento e os avisos ativos, sem devolver dado de empresa e sem executar ação no sistema. A resposta conversa como uma pessoa da equipe: responde primeiro o que foi perguntado, acompanha o jeito de escrever do cliente e varia a forma de falar, sem roteiro fixo, dentro do mesmo escopo. Texto livre no menu que não seja opção nem pergunta comercial segue a conversa livre (IA-09) em vez de repetir o menu. Mensagem fora do escopo recebe a recusa padrão do Suporte seguida de "Para consultar os dados da sua empresa, use o painel do app.". Mensagem filtrada, recusada ou respondida pelo bot devolve 200 ao webhook. O número sai mascarado e o texto só pelo tamanho no log (WPP-06).

#### Scenario: primeira mensagem abre o menu
- **WHEN** chega POST `/webhook/whatsapp` com `type=chat` e texto "Oi" na sessão da atendente
- **THEN** a API SHALL responder 200 e o bot SHALL cumprimentar e apresentar o menu de dúvida, reunião e conhecer a Gamb

#### Scenario: cliente tira dúvida no número da Gamb
- **WHEN** o cliente na opção de dúvida pergunta como conectar o WhatsApp
- **THEN** o bot SHALL responder com o passo a passo da base de conhecimento

#### Scenario: cliente pede dado de empresa no número da Gamb
- **WHEN** o cliente na opção de dúvida pede preços, reservas ou dados de uma empresa
- **THEN** o bot SHALL responder a recusa padrão do Suporte e orientar a usar o painel, sem consultar nem devolver dado de empresa

#### Scenario: strategies das empresas não mudam
- **WHEN** a `FactoryModulo` busca o ramo "Locação"
- **THEN** a strategy retornada SHALL continuar a `LocacaoStrategy`

#### Scenario: cliente pergunta do que se trata a Gamb
- **WHEN** o cliente no menu digita *3* ou escreve "o que vocês fazem?"
- **THEN** o bot SHALL responder quem é a Gamb e o que ela oferece hoje, sem valor, prazo nem nome de cliente, e convidar a digitar *2*

#### Scenario: cliente descreve o problema dele
- **WHEN** o cliente escreve "perco reservas porque demoro a responder, como vocês resolvem?"
- **THEN** o bot SHALL ligar o problema ao assistente no WhatsApp com reserva pela Google Agenda, tratar o que está em desenvolvimento como avaliado caso a caso e não informar preço nem prazo

#### Scenario: pergunta do app continua no Suporte
- **WHEN** o cliente na opção de dúvida pergunta como conectar o WhatsApp no app
- **THEN** o bot SHALL responder pelo Suporte e não pela resposta comercial

#### Scenario: texto livre no menu não repete o menu
- **WHEN** o cliente no menu escreve algo que não é opção nem pergunta comercial
- **THEN** o bot SHALL seguir a conversa livre (IA-09) e não responder apenas o menu
