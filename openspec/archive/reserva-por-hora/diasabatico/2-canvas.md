# Canvas: reserva-por-hora (bloco Funcionamento)

Canvas: "Painel gamb — telas" (https://claude.ai/artifact/J891R7bxYLZtgv2LJsn47p), versão 29.

## O que desenhei
- Board `ClienteChatbot.dc.html` (tela /cliente/chatbot, perfil `cliente-perfil`): nova aba "Funcionamento" entre "Configurações do Robô" e "Configuração".
- Boards novos que mostram só essa aba: `ClienteFuncionamento.dc.html` (1280) e `ClienteFuncionamentoMobile.dc.html` (375), com título "Proposta: bloco Funcionamento (reserva por hora)".
- Modo simples: Abre às, Fecha às e Duração do atendimento (30 min a 12 h, de 30 em 30). Botão "Personalizar" abre e fecha o resto.
- Personalizar: horário por dia (Segunda a Domingo e Feriado, chave aberto/fechado, Abre, Fecha e Duração com "Usar a geral"), Intervalo entre atendimentos (0 a 120), Estado (UF, as 27), Dias especiais (lista com data, selo "Fechado" ou "10:00 às 16:00", motivo e Remover) e o formulário "Novo dia especial" (data de hoje até 180 dias, motivo até 120 letras, chave "Fechado o dia todo", Abre e Fecha).
- Aviso "Há reservas nesta data" com a lista dos eventos da agenda, avisando que nada foi cancelado.
- Estados no Tweaks (`estadoFuncionamento`): normal, carregando, vazio, erro, sucesso, salvarDesabilitado e reservasNaData. O celular abre em reservasNaData.

## Decisões de design
- O bloco virou uma aba própria. Recomendo assim: o robô e o funcionamento ficam curtos e fáceis de achar. Outra opção seria colocar dentro de "Configurações do Robô", mas a aba fica longa demais no celular.
- Mesmo visual da tela: cartão branco, roxo da marca nos textos, laranja accent só no "Salvar funcionamento". "Personalizar" e "Adicionar dia especial" são botões secundários.
- Dia fechado fica com fundo mais claro e campos travados, para ler de longe que não abre.
- Selo laranja claro para horário próprio e roxo claro para fechado: diferem em cor e em claridade.
- Aviso de reservas em laranja claro (atenção, não erro). Erro de carregar em vermelho claro com "Tentar de novo".
- No celular cada dia empilha: nome e chave em cima, Abre, Fecha e Duração embaixo.

## O que o Angular precisa saber
- Lê e salva por `GET/PUT /empresas/{sessao}/funcionamento`; dias especiais por `POST /dias-especiais` e `DELETE /dias-especiais/{id}`.
- Sem horário salvo vale 08:00 às 22:00 e 60 min: o estado vazio mostra isso em texto.
- Modo simples grava o mesmo abre/fecha nos 7 dias e no feriado; duração vazia no perfil vai como `null`.
- Salvar fica desabilitado quando fechar não é depois de abrir, com a mensagem embaixo do campo.
- `reservasNaData` da resposta do POST alimenta o aviso; a data mínima e máxima do campo vêm de hoje e hoje + 180.
- Textos novos vão para `cliente-perfil.content.json`; classes novas para `telas.design.json` (o perfil ainda não tem entrada lá).
- [PLACEHOLDER] usados: [MOTIVO] e [EVENTO DA AGENDA].

Estado: aprovado
