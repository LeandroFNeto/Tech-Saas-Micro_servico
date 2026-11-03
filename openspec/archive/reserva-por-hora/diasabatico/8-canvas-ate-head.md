# Canvas até HEAD (3d2e0a7)

Base: canvas em eefb1f6 → código em 3d2e0a7 + disco (shell.component.html só perdeu comentários, sem mudança visual). Canvas: https://claude.ai/artifact/J891R7bxYLZtgv2LJsn47p (versão 33).

## O que faltava ou divergia

| Board | Faltava / divergia | Commit |
|---|---|---|
| Tutoriais (cliente) | tela inteira | 3d2e0a7 |
| Assistente do cliente | componente inteiro (Suporte / Simulador Virtual) | 3d2e0a7 |
| Laboratório (admin) | tela inteira (saúde dos módulos, chat, modo Sandbox) | c447acb |
| Menu do cliente (Painel, Chatbot, Automação) | link Tutoriais; aba de baixo dizia "Chatbot", código diz "Configuração do chatbot" | 3d2e0a7 |
| Admin · Empresas | botão "Diagnóstico & IA", "Cadastrar cliente" some no nível leitura, botão Excluir e modal (só super admin), textos reais de status, grade e botões menores, estados carregando/vazio/erro | 3aedde0, c9b6eb9, 23e11ea, c447acb |
| Admin · Editar cliente | chave "Liberar previsão do tempo", chave "Reserva automática pelo bot", Salvar some no nível leitura, bloco "Redefinir senha de acesso" (só super admin) | 3aedde0, 23e11ea |
| Admin · Avisos | Editar/Excluir e formulário só para super admin | 23e11ea |
| Cliente · Painel | assistente embutido; chave de previsão travada com aviso do admin | 3aedde0, 3d2e0a7 |
| Funcionamento (dentro do Chatbot) | por dia no celular: 2 colunas e Duração na linha inteira; intervalo de 1 em 1 minuto | f0364ad |
| Automação | conferido: todos os textos do content.json já estavam no board | fd642e6 |

## O que mudou por board

- Novos: ClienteTutoriais + Mobile 375, AssistenteCliente + Mobile 375, AdminLaboratorio + Mobile 375 (linha nova em y 8100, título acima).
- AdminEmpresas: ajuste `nivel` (SUPER_ADMIN, ADMIN_OPERACIONAL, ADMIN_LEITURA), ajuste `estado` (normal, carregando, vazio, erro, excluindo) e `modal`.
- AdminClienteForm e Mobile: `nivel` e `previsaoLiberada`. AdminAvisos: `nivel`.
- ClientePainel e Mobile: `previsaoLiberada`, `assistenteModo`, `assistenteEstado`; mobile cresceu para 3700.
- ClienteChatbot e ClienteAutomacao: menu com Tutoriais.

## O que não deu para fazer

- Não renderizei para conferir (regra do pedido).
- Os boards do celular dependem do desktop importado; alturas podem sobrar.
- Mantive o código onde ele foge do padrão: texto branco sobre laranja no assistente e no "Abrir Sessão Virtual de Testes" tem contraste baixo; ícones 🧪/📖 do Angular viraram desenho de traço (o canvas não usa emoji).
- Textos vindos do banco (avisos, respostas da IA, contagens) ficaram como [PLACEHOLDER].
- ClientePainel desktop segue com moldura 1500; o conteúdo novo rola dentro dela para não sobrepor a linha de baixo.

Estado: aprovado
