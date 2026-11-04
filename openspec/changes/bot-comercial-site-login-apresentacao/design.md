## Context

O endpoint `POST /api/publico/assistente/suporte` chama `responderSuporte`, que monta um prompt com a base de conhecimento do painel e os avisos ativos. Isso serve a quem já usa o app, mas o visitante do login e da apresentação é, em geral, um interessado. A apresentação (`apresentacao.content.json`) fala só do produto de hoje: atendimento automático no WhatsApp, fotos, preços, regras, datas livres da Google Agenda e painel no celular.

A Gamb se define como uma startup que fecha negócio pelo WhatsApp, vende por plano de assinatura e acompanha o cliente no tempo. O assistente virtual é a primeira solução; o que vem depois nasce da necessidade do negócio do cliente, montando um ecossistema sobre o que já existe no mundo dele, sem empilhar aplicativos novos, e podendo chegar a IoT e a possibilidades físicas, como usar o sistema de câmeras de segurança que restaurantes e comércios já têm para acompanhar o atendimento. A carteira é pequena de propósito, como laboratório de teste inicial.

## Goals / Non-Goals

**Goals:**
- Responder o visitante sobre quem é a Gamb, o que oferece hoje, o leque de tecnologia que pode vir, o modelo de assinatura e como começar.
- Sugestões e saudação diferentes no login e na apresentação.
- Manter intactos o isolamento de dados, o limite por IP e a recusa para assunto fora da Gamb.

**Non-Goals:**
- Formulário de interesse, captura de lead ou qualquer gravação de dado do visitante.
- Tabela de planos, preço ou limite de vagas no bot.
- Documentar ou prometer produto de integração ou IoT: eles ficam como caminho avaliado caso a caso.
- Mudar o Suporte do painel e da tela de Tutoriais.
- O bot de WhatsApp da Gamb (change `bot-whatsapp-gamb-suporte-teste`), que poderá reaproveitar a resposta comercial.

## Decisions

1. **Modo `COMERCIAL` próprio, não um prompt de Suporte com mais texto.** O endpoint público chama `responderComercial(mensagem, origem)`. O Suporte do painel continua em `responderSuporte`. Separar evita que o visitante receba passo a passo do painel e que o cliente receba conversa de venda.
2. **Base de contexto em arquivo de recurso (`assistente/base-comercial.md`).** O texto da empresa fica num arquivo editável sem mexer em código e é injetado no prompt. Seções: quem é a Gamb, o que existe hoje, o que pode vir, como funciona a assinatura, como começar, o que o bot não promete.
3. **Separar "hoje" de "caminho".** O arquivo marca duas camadas. Hoje: assistente virtual no WhatsApp, controle de reserva automático pela Google Agenda, conversa humanizada com IA, fotos, preços, regras e avisos, painel no celular. Caminho, avaliado caso a caso com o cliente: nova conexão com outras funcionalidades junto ao WhatsApp dele, integração com as ferramentas que ele já usa, automações, IoT e análise das câmeras de segurança que ele já tem (replay automático, tempo de espera de cada cliente, quem já foi atendido, tempo médio e meta de espera). O prompt manda afirmar só a primeira camada como entrega e tratar a segunda como conversa de necessidade, sem funcionalidade, prazo ou garantia.
4. **Prompt com PODE e NÃO PODE.** PODE: contar quem é a Gamb e como trabalha (startup, grupo pequeno, assinatura, acompanhamento contínuo, crescer junto com o negócio, ecossistema sobre o que o cliente já usa), explicar o assistente de hoje, apresentar o caminho como possibilidade, indicar o WhatsApp da Gamb para valor e contratação. NÃO PODE: citar valor ou faixa de preço, prometer prazo de implantação, citar nome de empresa cliente, afirmar que integração ou IoT já estão prontos, comparar com concorrente, prometer vaga, trazer dado de qualquer empresa. Fora da Gamb, devolve a recusa fixa. O prompt continua sem `Empresa`.
5. **Estágio dito com honestidade.** O leque de ferramentas ainda é pequeno e está em desenvolvimento. Pergunta sobre o que mais existe recebe a resposta de que o assistente virtual é a solução de hoje, que as demais estão sendo desenvolvidas e que o leque cresce junto com as necessidades dos clientes. O bot não lista ferramenta futura como disponível.
6. **Crescimento controlado dito com naturalidade.** Pergunta sobre entrar, contratar ou vaga recebe a resposta de que a Gamb atende poucos clientes por vez, acompanha de perto e começa por uma conversa pelo WhatsApp. O bot não confirma vaga nem cadastra ninguém.
7. **Preço sob consulta.** Pergunta de custo recebe resposta fixa no espírito de "o plano de assinatura é combinado conforme o seu negócio; fale com a Gamb pelo WhatsApp". O texto de reserva sem IA faz o mesmo, então a regra vale mesmo com o modelo fora do ar.
8. **Campo `origem` opcional no request (`LOGIN` ou `APRESENTACAO`).** Sem o campo, vale `APRESENTACAO`. A origem escolhe as sugestões devolvidas na resposta; o texto do prompt é o mesmo. Valor inválido responde 400.
9. **Saudação e sugestões iniciais vêm do conteúdo da tela.** `assistente-cliente` ganha entradas `saudacao` e `sugestoesIniciais`; login e apresentação as leem dos seus `*.content.json` e passam pelo `chat-flutuante`. Sem a entrada, o componente mantém o texto atual, e o painel não muda.
10. **Contato por configuração.** Propriedade `gamb.contato.whatsapp` (variável `GAMB_CONTATO_WHATSAPP`), valor 5543998165047, que gera o link `https://wa.me/5543998165047`. Propriedade vazia: o prompt e o texto de reserva citam só "WhatsApp da Gamb". O número não vai fixo no código nem no arquivo de contexto.
11. **Conteúdo das sugestões.** Apresentação: "Quem é a Gamb?", "O que vocês oferecem?", "Como funciona o plano de assinatura?", "Como começo?". Login: "Como entro no painel?", "Esqueci minha senha", "Ainda não sou cliente, como falo com a Gamb?", "Quem é a Gamb?". Esqueci a senha leva ao WhatsApp da Gamb, porque o fluxo de recuperação não existe no app.

## Risks / Trade-offs

- [O modelo pode inventar serviço, integração ou prazo] -> a base separa "hoje" de "caminho", o prompt lista o que pode afirmar, e os testes cobrem preço, prazo, nome de cliente, IoT e vaga.
- [Câmera envolve imagem de pessoas] -> o bot apresenta a análise de câmeras como ferramenta de decisão que analisa sem guardar informação de ninguém, com o fim de melhorar a eficiência do estabelecimento e a satisfação do cliente. Isso é o princípio de trabalho da Gamb, não um produto pronto: o bot não afirma conformidade legal, não descreve funcionamento técnico e deixa os detalhes para a conversa no WhatsApp.
- [Texto vender mais do que a Gamb entrega] -> o caminho aparece sempre condicionado a conversa e avaliação; o arquivo é a única fonte e muda por edição dele.
- [Conteúdo envelhece quando a oferta crescer] -> basta editar `base-comercial.md`; nenhuma tela ou código muda.
- [Limite por IP atrás de proxy] -> o endpoint lê `getRemoteAddr()`, que atrás do nginx pode juntar todos os visitantes num IP. Fica como verificação nas tarefas, sem mudar o limite aqui.
- [Requisito antigo de Suporte público] -> o requisito novo vale para o login e a apresentação; depois do arquivamento da change anterior o texto antigo deve ser relido junto com este.

## Open Questions

- Quais exemplos de "caminho" a Gamb quer citar de fato além de integração com o que o cliente já usa e IoT, e com que palavras.
