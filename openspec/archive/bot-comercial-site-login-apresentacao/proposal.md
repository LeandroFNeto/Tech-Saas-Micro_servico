# Proposal

## Why

O chat do login e da apresentação responde hoje como o Suporte de dentro do app: as sugestões falam de QR Code, Google Agenda e tutoriais. Quem chega ao site ainda não é cliente e quer saber quem é a Gamb, o que ela oferece, como é o custo e como contratar. A Gamb não se apresenta como uma aplicação fechada: é uma startup, um grupo pequeno de desenvolvedores que atende por plano de assinatura, cresce junto com o negócio de cada cliente e leva tecnologia a quem ainda não a aproveita, montando um ecossistema que funciona sobre o que o cliente já usa. O negócio é fechado pelo WhatsApp, e o assistente virtual é a primeira solução desse leque. O bot do site precisa contar essa história, sem prometer o que ainda não existe.

## What Changes

- **Bot comercial no site**: o endpoint público passa a responder em modo `COMERCIAL`, com uma base de contexto da empresa em vez da base de suporte do painel.
- **Posicionamento**: a Gamb é apresentada como parceira de tecnologia por assinatura, com acompanhamento contínuo e evolução conforme a necessidade do negócio do cliente. Hoje a entrega é o assistente virtual no WhatsApp com controle de reserva automático pela Google Agenda e conversa humanizada com IA; quando o cliente precisa de outra funcionalidade junto ao WhatsApp dele, a Gamb monta uma nova conexão. Esse leque (integração com o que o cliente já usa, automações, IoT e possibilidades físicas, como usar as câmeras de segurança que restaurantes e comércios já têm para replay automático e para medir o atendimento: quanto tempo cada cliente espera, quem já foi atendido, tempo médio e meta de tempo de espera) é avaliado caso a caso. Essas ferramentas são de apoio à decisão: analisam, não guardam informação de ninguém e existem para melhorar a eficiência do estabelecimento e a satisfação do cliente, e o bot o apresenta como caminho, nunca como produto pronto.
- **Leque em construção**: o bot é honesto sobre o estágio. As outras ferramentas estão em desenvolvimento e o leque ainda é pequeno, mas está crescendo; o que existe hoje é o assistente virtual, e o resto é conversado e construído com o cliente.
- **Crescimento controlado**: a Gamb atende poucos clientes de cada vez, como laboratório de teste inicial. O bot diz que a entrada é por conversa e não abre cadastro nem promete vaga.
- **Preço só sob consulta**: o bot nunca cita valor; explica que o plano de assinatura é combinado conforme o negócio e leva o interessado ao WhatsApp da Gamb (55 43 99816-5047), que é também o caminho de contratação.
- **Limites do que o bot promete**: sem prazo de implantação, sem nome de empresa cliente e sem recurso que não existe. Segue valendo a regra de não levar dado de empresa, a recusa padrão para assunto fora da Gamb e a trava contra pedido de burla.
- **Sugestões por tela**: o login mostra sugestões de acesso e de quem ainda não é cliente; a apresentação mostra sugestões sobre quem é a Gamb, o que ela oferece, como funciona o plano e como começar. A saudação inicial também muda por tela.
- **Dentro do app nada muda**: o painel e a tela de Tutoriais mantêm o Suporte e as sugestões atuais.
- **Contato configurável**: o número da Gamb vem de configuração, com o valor 5543998165047 nos ambientes.

## Capabilities

### New Capabilities

### Modified Capabilities
- `assistente-tutoria`: o assistente público do login e da apresentação passa a ser comercial, com base de contexto da empresa, plano por assinatura, preço sob consulta e sugestões por tela.

## Impact

- Backend: `AssistenteTutoriaService` (resposta comercial, prompt, fallback, sugestões por origem), `SuportePublicoRequestDTO` (campo `origem`), `PublicoAssistenteSuporteController`, nova base de contexto em `src/main/resources`, propriedade do contato da Gamb.
- Contratos: `especificacao/back/paths/publico-assistente-suporte.yaml` e `especificacao/back/schemas/assistente/SuportePublicoRequestDTO.yaml`.
- Front: `assistente-cliente` e `chat-flutuante` (saudação e sugestões iniciais como entrada), `login` e `apresentacao` (conteúdo em `especificacao/front/*.content.json`).
- Depende de `ajustes-visuais-chat-flutuante-tutoriais` (já aplicada): o endpoint público, o limite por IP e a trava de escopo vêm de lá. Como ela ainda não foi arquivada, esta change adiciona o requisito novo em vez de modificar o antigo.
- O número 55 43 99816-5047 é o mesmo da change `bot-whatsapp-gamb-suporte-teste`, que fecha a sessão de WhatsApp dele.
