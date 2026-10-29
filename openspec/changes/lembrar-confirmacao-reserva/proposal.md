# lembrar-confirmacao-reserva

## Problema

O dono do espaço perde tempo repetindo a mesma rotina: responder data, preço, fotos e regras, e depois descobrir só no dia que a pessoa desistiu, com o espaço parado. Nem todo cliente da plataforma quer reserva automática pelo bot. Muitos só querem tirar o retrabalho de atendimento e continuam fechando a reserva por conta própria, direto no Google Agenda. Também não há registro de quem já conversou com o bot, então cada contato que não fechou na hora se perde.

A reserva do dono já está na agenda dele. O sistema pode ler essa agenda, sugerir o aviso ("tá tudo certo para amanhã?") e, se o dono quiser, enviar sozinho. Quem responde fica visível numa tabela do dia.

## O que muda

### Entra

- **Contatos do cliente.** Todo número que fala com o bot de uma empresa vira um contato daquela empresa: número, nome (quando se conhece), primeira e última conversa. Funciona para qualquer cliente da plataforma, tenha ou não reserva automática. Os contatos pertencem ao cliente: nenhuma consulta cruza empresas, o mesmo número em duas empresas são dois contatos independentes, e a plataforma não lê nem usa esses dados.
- **Reservas lidas da agenda do dono.** O sistema lê os eventos dos próximos 30 dias do Google Agenda da empresa e mantém uma cópia em `reservas`, atualizada de hora em hora e quando o dono abre a tela. Reservas feitas pelo bot (para quem usa reserva automática) entram na mesma tabela.
- **Vínculo com o contato.** O sistema procura um telefone no título ou na descrição do evento (o evento criado pelo bot já leva o telefone). Se acha, liga ao contato (ou cria um). Se não acha, a reserva fica "sem contato" e o dono liga a um contato da lista, ou digita o telefone, com um clique.
- **Lembrete de confirmação** com três modos por empresa, desligado por padrão:
  - `DESLIGADO`;
  - `SUGERIDO`: no horário configurado, as reservas do dia alvo aparecem como "prontas para avisar" e o dono envia com um clique, uma a uma ou todas;
  - `AUTOMATICO`: o sistema envia sozinho no horário configurado.
  - Em qualquer modo ligado, o dono também pode enviar "agora" para uma reserva futura com contato.
- Configuração do lembrete: modo, antecedência em dias (1, 2 ou 3; padrão 1), horário (HH:MM, fuso `America/Sao_Paulo`; padrão 10:00) e texto editável (até 1000 caracteres, com `{nome}`, `{data}` e `{horario}`; texto padrão quando vazio).
- **Resposta em linguagem natural.** O cliente final responde com suas palavras ("pode ser", "confirmo", "preciso mudar a data", "não vou poder"), sem número obrigatório. O sistema entende confirmação, pedido de remarcar ou cancelar e pedido para parar de receber avisos. O que não entende segue o atendimento normal e não muda nada. A resposta só vale quando o cliente não está no meio de outra conversa. O entendimento fica atrás de uma interface, para trocar por IA mais tarde sem mexer no resto.
- **Tela própria "Automação" do cliente** (rota `/cliente/automacao`), com três abas: "Lembrete" (a configuração), "Reservas" e "Contatos". O lembrete não entra na configuração do chatbot. A aba Reservas traz a tabela do dia (hoje, amanhã e próximos 30 dias) e a situação de cada reserva:
  - confirmada pelo cliente;
  - lembrete enviado, aguardando;
  - **sem resposta: risco alto de perda**, em destaque;
  - pediu para remarcar ou cancelar;
  - falha no envio ou na agenda, que precisa de atenção;
  - sem contato (com a ação "ligar contato").
- Aba "Contatos" com a lista de interessados da empresa.
- **Entradas para a tela de Automação**: um botão "Automação" no painel inicial do cliente (`/cliente`, criado em `exibir-previsao-tempo-reservas`) e um item "Automação" no menu lateral.
- **Proteção dos dados dos contatos:**
  - número e nome gravados criptografados;
  - telefone sempre mascarado na tela e na API;
  - o número completo só aparece por ação explícita, um contato por vez, com limite por hora e registro de quem pediu;
  - a equipe da plataforma não tem acesso às rotas de contatos e reservas dos clientes;
  - sem a chave de criptografia configurada, os contatos não são gravados.
- Retenção: contatos sem conversa e reservas encerradas há mais de 12 meses são apagados por rotina diária.

### Altera

- `EMP-02`: `EmpresaUpdateDTO` e `EmpresaResponseDTO` ganham o bloco de lembrete. A regra vigente da reserva automática não muda: `permiteReservaAutomatica` nasce `false`, só o admin altera, ligar exige `googleCalendarId`, e o PUT do cliente rejeita mudança desse campo com 400. O bloco de lembrete é independente dela: o cliente configura o lembrete sem mexer na reserva automática, e o PUT que traz o lembrete não toca nesse campo.
- Navegação do cliente: o painel inicial ganha o botão "Automação" e o menu lateral ganha o item "Automação". A configuração do chatbot (`/cliente/chatbot`) não muda.
- Webhook do WhatsApp: antes de chamar o módulo da empresa, registra o contato e trata a resposta do lembrete.
- Fluxo de locação: o `ObservadorGoogleAgenda`, que já registra no Google com o resultado real (AGD-02), passa a também gravar a `Reserva` do bot quando o resultado é sucesso ou falha. Data ocupada não grava reserva.
- `ServicoMensagem` ganha um envio que informa se deu certo, para o lembrete não marcar "enviado" quando o WhatsApp está fora.

### Sai

Nada.

## Fora de escopo

- Reserva automática funcionando de verdade (evento no Google, tratamento de erro, conflito de data): já entregue pela change `corrigir-reserva-automatica` (AGD-02 e WPP-07 nos specs). Esta change só acrescenta a gravação da `Reserva` em cima do resultado que o observador já produz. Quem escolhe a reserva manual não depende disso.
- Reserva por hora: a change `reserva-por-hora` ainda não existe como código. Hoje a empresa com locação por hora atende como reserva manual, e o lembrete só alcança a reserva por hora que vier da agenda do dono (evento com hora) ou depois que `reserva-por-hora` entregar. O lembrete usa só a data.
- Apagar ou mover o evento no Google Agenda quando o cliente pede para remarcar. O dono decide e faz.
- Conversa com IA sobre o lembrete (explicar, negociar nova data). A interface de entendimento da resposta deixa o gancho.
- Lembrete por e-mail ou SMS.
- Campanhas ou mensagens de marketing para os contatos. Esta change só avisa sobre reserva que já existe.
- Máscara de número e texto nos logs que o sistema já imprime: já entregue por `proteger-dados-nos-logs` (WPP-06, `MascaraLog`). Os logs novos desta change usam a mesma máscara e não registram esses dados.
- Aviso de chuva com pergunta "quer remarcar?" para o cliente que reservou. Fica para a change `avisar-risco-chuva-reserva`, que reaproveita o envio e a interpretação de resposta desta, junto com a previsão já entregue (`exibir-previsao-tempo-reservas`, EMP-05 e EMP-06).

## Domínios

- spec: `openspec/specs/agenda/spec.md` (AGD-03 sincronizar a agenda em reservas, AGD-04 enviar lembrete; AGD-01 e AGD-02 já existem)
- spec: `openspec/specs/whatsapp/spec.md` (WPP-08 registrar contato, WPP-09 tratar resposta do lembrete; WPP-07 já é a confirmação da reserva automática)
- spec: `openspec/specs/empresa/spec.md` (EMP-07 configurar lembrete, EMP-08 listar reservas e ligar contato, EMP-09 listar contatos e ver o número completo; EMP-01 a EMP-06 já existem)

Os três pré-requisitos (`proteger-dados-nos-logs`, `corrigir-reserva-automatica` e `exibir-previsao-tempo-reservas`) já estão no ar e arquivados; os IDs acima são os próximos livres em cada `spec.md`. Falta só `remover-x-cliente-token`, que está sendo trabalhada no Antigravity e mexe em `ControllerEmpresa`, `ControllerWhatsapp` e `JwtAuthenticationFilter`: esta change só começa a programação depois que ela entrar, para as duas não editarem os mesmos arquivos, e as rotas novas daqui já exigem o token da própria empresa. A tela de reservas pode reaproveitar o selo de risco da previsão.

## Em aberto para decisão

- Lembrete de reserva por hora: ficar fora até `reserva-por-hora` entregar, ou aceitar já o evento com hora vindo da agenda do dono.
- Quando a sessão por número virar sessão por empresa e quando o mapa de bloqueios do observador ganha limpeza: antes desta change ou depois, já que esta passa a gravar `Reserva` no mesmo caminho.
