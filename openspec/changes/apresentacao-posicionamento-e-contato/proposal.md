# Proposal

## Why

A página de apresentação (`/`) fala só do produto de hoje: o assistente que responde e reserva pelo WhatsApp. O chat do site, por sua vez, já conta uma história mais completa: a Gamb é uma startup, um grupo pequeno de desenvolvedores que atende por plano de assinatura, acompanha o negócio do cliente e está construindo um leque de soluções. Quem lê a página e quem conversa com o chat recebem mensagens diferentes, e a página não mostra nenhum caminho de contato. Esta change alinha a página ao chat e coloca o contato da Gamb no WhatsApp à vista.

## What Changes

- **Quem somos**: nova seção que apresenta a Gamb como startup, grupo pequeno de desenvolvedores, plano de assinatura com acompanhamento contínuo, crescimento junto com o negócio do cliente e atendimento a poucos clientes por vez.
- **Hoje e o que vem**: nova seção em duas partes. "Hoje": assistente virtual no WhatsApp com conversa humanizada por IA, controle de reserva automático pela Google Agenda, fotos, preços, regras e avisos, painel no celular. "Em desenvolvimento": nova conexão com outras funcionalidades do WhatsApp do cliente, integração com o que ele já usa, automações, IoT e análise das câmeras de segurança que ele já tem, como ferramenta de apoio à decisão que não guarda informação de ninguém. A parte em desenvolvimento é dita como caminho, sem produto pronto, prazo ou garantia.
- **Contato por WhatsApp**: botão "Falar com a Gamb no WhatsApp" no topo, em uma seção de contato antes do rodapé e o número `(43) 99816-5047` escrito, todos levando a `https://wa.me/5543998165047`. O preço segue sob consulta: a seção diz que o plano é combinado conversando.
- **Texto único com o chat**: os textos das seções e o do chat vêm do mesmo vocabulário (startup, assinatura, hoje, em desenvolvimento), para a página e o bot não se contradizerem.
- **Sem mudança de rota ou de API**: a página continua pública, sem consumir API, com o link Entrar e o redirecionamento de quem tem sessão.

## Capabilities

### New Capabilities

### Modified Capabilities
- `auth`: a página pública de apresentação (AUTH-03) passa a apresentar o posicionamento da Gamb e o contato por WhatsApp.

## Impact

- Front: `apresentacao.component.html` e `.ts`, `especificacao/front/apresentacao.content.json` (textos e número), `especificacao/front/telas.design.json` (classes das seções novas), `apresentacao.component.spec.ts`.
- Contratos: nenhum, é rota do front.
- Produção: o front publica pela branch `producao` (Vercel). A change só chega ao ar depois de levar o front para essa branch.
- Relaciona-se com `bot-comercial-site-login-apresentacao` (mesmo posicionamento) e com `bot-whatsapp-gamb-suporte-teste` (mesmo número).
- O requisito AUTH-03 do spec principal não usa o formato `### Requirement:` do CLI, então esta change adiciona requisitos novos em vez de modificá-lo.
