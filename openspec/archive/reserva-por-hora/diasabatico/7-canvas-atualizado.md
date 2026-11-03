# Canvas atualizado: Automação e Lembretes

Canvas: https://claude.ai/artifact/J891R7bxYLZtgv2LJsn47p (versão 31)

## O que mudou em cada board

- ClientePainel: novo cartão "Automação e Lembretes" com o botão "Automação", ao lado do cartão do chatbot e no mesmo visual. Menu lateral e abas de baixo ganharam "Automação" entre Painel e Configuração do chatbot.
- ClientePainelMobile: só importa o ClientePainel, então recebe tudo sozinho.
- ClienteChatbot: menu lateral e abas de baixo com "Automação" na ordem do shell.content.json.
- ClienteChatbot, aba Funcionamento (vale para ClienteFuncionamento e o mobile, que importam o board):
  - Cores trocadas pelos tokens mais próximos: campo desligado #8a6fa3 virou brand-500, botão desligado #8a6fa3 virou brand-600, dia fechado #faf7fd virou brand-50 e borda do aviso #f5b37a virou accent-300.
  - A aba não tem seletor de estados na tela. O estado só muda pelo painel de propriedades do canvas, como já estava.
  - O modo simples agora lê "Abre às" e "Fecha às" do primeiro dia aberto.
  - "Personalizar" abre sozinho quando os horários salvos não são iguais em todos os dias. No estado vazio ele fica fechado.
- ClienteAutomacao (novo, 1280, x 6430 y 0): abas Lembrete, Reservas e Contatos.
  - Lembrete: modo de envio, antecedência, horário e texto (LembreteConfirmacaoDTO). Com o modo Desligado, só o campo de modo aparece.
  - Reservas: tabela (ReservaResumoDTO) com selo colorido por situação, usando os tokens de situacaoReserva, mais os botões "Avisar agora" e "Ligar contato". O botão de vincular abre o modal (VincularContatoDTO).
  - Contatos: tabela (ContatoResumoDTO) com o número mascarado. "Ver número" mostra o número inteiro (NumeroContatoDTO).
  - Estados: normal, carregando, vazio, erro, sucesso e desabilitado, em cada aba.
- ClienteAutomacaoMobile (novo, 375, x 7790 y 0): importa o ClienteAutomacao e abre na aba Reservas.

## O que não foi possível

- Faltam textos no content.json para três casos, que ficaram como [PLACEHOLDER]: lembrete ainda não configurado, sucesso ao vincular contato e selo "não contatar". Nomes de cliente que não estão no contrato também ficaram como [PLACEHOLDER].
- O shell.content.json já traz "Tutoriais". Esse item não entrou no menu porque o canvas não tem board de tutoriais.
- Nenhum board foi conferido na tela, como o pedido mandava.

Estado: aprovado
