# Proposal

## Why

O assistente de ajuda está fixo na tela do painel, com os modos Suporte e Simulador juntos, e manda nome, ramo e sessão da empresa ao modelo mesmo quando a dúvida é só sobre o app. A tela de Tutoriais mistura chat e guias, e a Apresentação no celular ficou amarelada demais. Esta change tira o chat da tela, o transforma num botão flutuante com o bot de Suporte, define o que esse bot pode e não pode responder e garante que ele nunca leve dado de empresa ao modelo.

## What Changes

- **Bot de Suporte em botão flutuante**: botão redondo de 56 px com o logo gamb, no canto inferior direito, sempre fechado ao abrir a tela. Ao clicar, abre o painel acima do botão, com cabeçalho escuro, logo e botão Fechar. No celular o botão sobe 76 px para não cobrir as abas de baixo. O chat embutido do painel é removido.
- **Escopo do Suporte**: ajuda sobre o próprio app. Pode explicar onde fica cada função, como fazer uma tarefa (QR Code, agenda, horários, fotos), o que os avisos e tutoriais ativos dizem e apontar a tela certa. Não responde sobre dados de nenhuma empresa, nem de outro cliente, nem assuntos fora do app, e não executa ações no sistema.
- **Sem dado de empresa**: o prompt do Suporte passa a levar só a base de conhecimento e os avisos ativos. Nome, ramo, sessão e demais campos da empresa deixam de ser enviados. O escopo e essa regra entram no contrato e ganham teste de vazamento. O mesmo bot serve depois ao número de WhatsApp da Gamb.
- **Simulador fora do botão**: o painel flutuante mostra só o Suporte. O modo de teste virtual continua no backend sem mudança e vai para a change do bot do WhatsApp.
- **Onde aparece**: painel do cliente (substitui o assistente embutido), login e apresentação. Não aparece na tela de Tutoriais, onde o chat já é o conteúdo principal, nem na área do admin, que só tem o módulo Assistente.
- **Suporte público**: o login e a apresentação usam o mesmo bot, sem sessão, por um endpoint novo sem autenticação, com limite por IP. A apresentação ganha a dica "Para mais informações, fale com o chat agora", com seta apontando para o botão.
- **Tela de Tutoriais**: o chat vem primeiro (seção `#chat`, sem botão Fechar). Abaixo, uma grade de cartões, um por tema, cada um com ícone de traço, título curto, resumo de uma linha, duração, número de passos e selo (Novo, Em andamento, Concluído). O tutorial aberto mostra passos numerados, passo atual em destaque, barra de progresso, Anterior/Próximo e "Perguntar ao assistente sobre este passo", que só preenche o chat, sem enviar. O progresso fica guardado só no navegador. Tutoriais continua no menu lateral do cliente.
- **Assistente aberto**: perguntas rápidas que preenchem o campo e botão Enviar com texto escuro sobre laranja.
- **Apresentação no celular**: menos amarelo no fundo. O laranja dos halos cai de .28 para .10 (topo), .35 para .14 (atrás do celular), .25 para .12 (anel) e .22 para .08 (bloco final). A imagem do WhatsApp fica sem camada por cima. O desktop não muda.

## Capabilities

### New Capabilities

### Modified Capabilities
- `assistente-tutoria`: a interface do assistente passa a ser um botão flutuante fechado por padrão com o bot de Suporte; o Suporte ganha escopo definido e prompt sem dado de empresa; passa a existir um endpoint público de Suporte para login e apresentação; a tela de Tutoriais passa a ter chat primeiro e guias individuais com passos e progresso.

## Impact

- Front (Angular): `shared/components/assistente-cliente`, novo componente de chat flutuante, `features/client/tutoriais`, `features/client/painel`, `features/auth` (login), `features/apresentacao`, `shared/components/shell`.
- Backend: `AssistenteTutoriaService` (prompt do Suporte sem dado de empresa e trava de escopo), novo endpoint público de Suporte com limite por IP e testes de vazamento.
- Contratos: novo YAML do endpoint público e ajuste de `cliente-assistente-chat.yaml` em `especificacao/back/paths/`; `especificacao/front/telas.design.json` (gradientes da apresentação só no celular) e os `*.content.json` das telas afetadas.
- Fora desta change: o bot do WhatsApp da Gamb e o bot de teste para módulos novos, que ficam numa change própria e reaproveitam o escopo do Suporte definido aqui.
