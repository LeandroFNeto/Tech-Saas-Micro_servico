# 10 · Tutoriais e chat flutuante (canvas versão 38)

## O que mudou por board
- ClienteTutoriais (+ Mobile): o chat vem primeiro (seção `#chat`, sem botão Fechar). Abaixo ficam 5 cartões separados, um por tema: ícone de traço, título curto, uma linha do resumo, `[DURAÇÃO] min`, número de passos, selo (Novo, Em andamento, Concluído) e "Ver tutorial". O tutorial aberto mostra passos numerados, o passo atual em destaque, barra de progresso, Anterior/Próximo e "Perguntar ao assistente sobre este passo", que preenche o chat. Estados: normal, carregando, vazio, erro, sucesso, desabilitado. Tweaks: `tutorialAberto`, `passoAtual`. Sem filtros, porque o código não tem.
- AssistenteCliente: virou o painel aberto. Cabeçalho escuro com logo e Fechar, modos Suporte e Simulador, perguntas rápidas que funcionam, Enviar com texto escuro sobre laranja e modo `publico` (só Suporte).
- AssistenteFlutuante (+ Mobile), novo: botão redondo de 56 px com o logo, no canto inferior direito, rótulo "Abrir chat de ajuda", fechado por padrão. Abre o painel acima do botão. Props: `assistenteAberto`, `publico`, `dica`, `sobreAbas` (76 px de altura no celular por causa das abas).
- ClientePainel (+ Mobile): saiu o assistente embutido. Fica só o botão flutuante (`assistenteAberto` = false).
- Login (Main + Mobile): botão flutuante no modo público.
- Apresentação (+ Desktop): botão flutuante com a dica "Para mais informações, fale com o chat agora" e uma seta apontando para ele.
- Template: novo componente "Chat de ajuda flutuante" com a regra "chat sempre fechado por padrão" e um item novo em "Nunca fazer".
- canvas.json: Template com h 7700, Tutoriais desktop com h 2400, nota "Componentes compartilhados" em y 44720 e boards flutuantes em y 45020. Nenhuma posição antiga mudou.

## Decisões e alternativa
- Recomendada: o tutorial abre abaixo da grade de cartões e o cartão escolhido fica com borda laranja. Alternativa: abrir o tutorial numa gaveta lateral, deixando a grade sempre visível.
- Na tela de Tutoriais não há botão flutuante, porque o chat já é o conteúdo principal. No login e na apresentação o modo Simulador fica escondido, porque exige login.

## Apresentação mobile
- De onde vinha o amarelo: halo laranja `rgba(247,147,30,.28)` no topo (no celular, estreito e alto, ele cobre quase todo o bloco), halo laranja `.35` atrás do celular, anel laranja na moldura e halo `.22` no bloco final. A imagem não tem filtro. O Angular tem os mesmos valores em `telas.design.json` (`apresentacao.*.fundo`, `celular.brilho`, `celular.moldura`).
- Correção (versão 39): a primeira troca foi exagerada (halos todos roxos, camada roxa sobre a imagem, bloco final roxo). Agora só o celular tem menos amarelo no fundo: o laranja continua, mais fraco (topo .28→.10, atrás do celular .35→.14, anel .25→.12, bloco final .22→.08). A camada sobre a imagem foi tirada e a imagem fica como era. O desktop voltou aos valores originais (prop `compacto`: celular = true, `ApresentacaoDesktop` passa false).
- Se aprovado, o front muda só no celular os gradientes de `telas.design.json` (`apresentacao.*.fundo`, `celular.brilho`, `celular.moldura`), por exemplo com prefixo `max-md:`.

## O que o Angular precisará mudar (nada foi alterado no código)
- `tutoriais.component`: o chat vai para o topo. `guias` ganham `tituloCurto`, `duracao` e situação salva por cliente, e o cartão vira item de grade com detalhe separado. Também ganham passo atual, progresso e `usarSugestao(duvidaSugerida + passo)` sem enviar sozinho.
- `assistente-cliente.component`: precisa de `@Input` `publico`, `fechavel` e `@Output` `fechar`, logo no cabeçalho, texto escuro no Enviar e na bolha do usuário e perguntas rápidas que preenchem o campo.
- Componente novo `chat-flutuante` (botão, painel e dica), fechado por padrão. Entra no `painel.component.html` no lugar de `<app-assistente-cliente />`, no login e na apresentação.
- O login e a apresentação não têm sessão, então o backend precisa de um endpoint público para o modo Suporte. Hoje `/cliente/assistente/chat` exige cliente autenticado.

## [PLACEHOLDER] usados
`[DURAÇÃO]`, situação dos tutoriais (dado de exemplo), texto da dica, saudação, descrição e perguntas do modo público, `[RESPOSTA DO ASSISTENTE]`, `[MENSAGEM DO SERVIDOR]`, `[TÍTULO DO TUTORIAL]`.

Estado: aprovado
