# Template do padrão visual

Board `Template.dc.html` (1280, fluido) e espelho `TemplateMobile.dc.html` (375, importa o Template) no canvas "Painel gamb — telas" (https://claude.ai/artifact/J891R7bxYLZtgv2LJsn47p). Só referência: não vira tela Angular, não tem rota, não entra no build.

## O que tem
- Faixa e selo "TEMPLATE — padrão visual (não vai para produção)".
- 1 Marca: logo (asset já existente `/_blob/2afaeee994f3399be9d349cc4eb1cf72`), "gamb" em Cinzel com degradê cobre, regras (mínimo 40 px, fundo brand-900, laranja só na ação principal, texto escuro sobre laranja).
- 2 Tokens: brand 50–900 e accent 50–800 com uso, risco e situacaoReserva, Inter/Cinzel com escala, espaçamento, raios, sombras, foco 2 px accent-400 e contraste 4.5:1.
- 3 Casca do cliente e do admin, desktop (menu lateral 248 px) e celular (topo + abas de 60 px), com os itens reais.
- 4 Componentes: cabeçalho, cartão, abas, botões (primário, secundário, perigo, desabilitado, carregando), campos com label, ajuda e erro (16 px), chave, seleção, tabela com rolagem interna, selos, modal, avisos, lista vazia.
- 5 Estados: normal, carregando, vazio, erro com "Tentar de novo", sucesso, desabilitado.
- 6 Como criar um board novo: passos e o que nunca fazer.

## Regras de uso
- Board novo copia o `<helmet>` do Template e só usa os tokens dele.
- Valor mudou no código (`telas.design.json`, `styles.css`, `shell.content.json`)? Atualize o Template.

## Fora
- Itens do menu do admin não estão em `shell.content.json` (fixos no `shell.component.html`); o Template mostra os reais, mas o content.json não cobre.
- Altura dos frames não foi medida renderizando (regra de não verificar); TemplateMobile ficou em 13000 por estimativa e os demais mobile mantiveram a altura que já tinham.
- Notas antigas do layout anterior (celular, apresentacao, funcionamento, assistente, template) foram trocadas pelas notas de coluna e de grupo; a nota "titulo" foi mantida no topo.

## Layout
Desktop em x=0, celular em x=1360 (80 px entre colunas, 120 entre linhas, 300 extra acima de cada grupo). Notas: "DESKTOP (1280)" (0, -600), "CELULAR (375)" (1360, -600), grupos Template (-300), Entrada (13120), Cliente (17860), Admin (35640). Todo board desktop tem par mobile.

| Linha | Desktop | Mobile | y |
|---|---|---|---|
| Template | Template (7000) | TemplateMobile (13000) | 0 |
| Apresentação | ApresentacaoDesktop (2100) | Apresentacao (3300) | 13420 |
| Login | Main (820) | MainMobile (900) | 16840 |
| Cliente · Painel | ClientePainel (1500) | ClientePainelMobile (3700) | 18160 |
| Cliente · Chatbot | ClienteChatbot (1500) | ClienteChatbotMobile (3100) | 21980 |
| Cliente · Funcionamento | ClienteFuncionamento (2600) | ClienteFuncionamentoMobile (4200) | 25200 |
| Cliente · Automação | ClienteAutomacao (1400) | ClienteAutomacaoMobile (2000) | 29520 |
| Cliente · Tutoriais | ClienteTutoriais (1900) | ClienteTutoriaisMobile (3000) | 31640 |
| Cliente · Assistente | AssistenteCliente (960 × 620) | AssistenteClienteMobile (760) | 34760 |
| Admin · Empresas | AdminEmpresas (820) | AdminEmpresasMobile (1100) | 35940 |
| Admin · Cliente form | AdminClienteForm (1500) | AdminClienteFormMobile (3300) | 37160 |
| Admin · Avisos | AdminAvisos (900) | AdminAvisosMobile (1900) | 40580 |
| Admin · Laboratório | AdminLaboratorio (1100) | AdminLaboratorioMobile (2000) | 42600 |

Estado: aprovado
