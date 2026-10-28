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
- **Tela "Reservas" do cliente**, com a tabela do dia (hoje, amanhã e próximos 30 dias) e a situação de cada reserva:
  - confirmada pelo cliente;
  - lembrete enviado, aguardando;
  - **sem resposta: risco alto de perda**, em destaque;
  - pediu para remarcar ou cancelar;
  - falha no envio ou na agenda, que precisa de atenção;
  - sem contato (com a ação "ligar contato").
- Tela "Contatos" com a lista de interessados da empresa.
- **Proteção dos dados dos contatos:**
  - número e nome gravados criptografados;
  - telefone sempre mascarado na tela e na API;
  - o número completo só aparece por ação explícita, um contato por vez, com limite por hora e registro de quem pediu;
  - a equipe da plataforma não tem acesso às rotas de contatos e reservas dos clientes;
  - sem a chave de criptografia configurada, os contatos não são gravados.
- Retenção: contatos sem conversa e reservas encerradas há mais de 12 meses são apagados por rotina diária.

### Altera

- `EMP-02`: `EmpresaUpdateDTO` e `EmpresaResponseDTO` ganham o bloco de lembrete.
- Webhook do WhatsApp: antes de chamar o módulo da empresa, registra o contato e trata a resposta do lembrete.
- Fluxo de locação: o `ObservadorGoogleAgenda` (change `corrigir-reserva-automatica`) grava a `Reserva` do bot quando o Google responde, com sucesso ou falha.
- `ServicoMensagem` ganha um envio que informa se deu certo, para o lembrete não marcar "enviado" quando o WhatsApp está fora.

### Sai

Nada.

## Fora de escopo

- Reserva automática funcionando de verdade (evento no Google, tratamento de erro, conflito de data): change `corrigir-reserva-automatica`, pré-requisito desta. Quem escolhe a reserva manual não depende dela.
- Apagar ou mover o evento no Google Agenda quando o cliente pede para remarcar. O dono decide e faz.
- Conversa com IA sobre o lembrete (explicar, negociar nova data). A interface de entendimento da resposta deixa o gancho.
- Lembrete por e-mail ou SMS.
- Campanhas ou mensagens de marketing para os contatos. Esta change só avisa sobre reserva que já existe.
- Máscara de número e texto nos logs que o sistema já imprime: change `proteger-dados-nos-logs`, que vem antes. Os logs novos desta change usam a mesma máscara e não registram esses dados.
- Aviso de chuva com pergunta "quer remarcar?" para o cliente que reservou. Fica para a change `avisar-risco-chuva-reserva`, que reaproveita o envio e a interpretação de resposta desta, junto com `exibir-previsao-tempo-reservas`.

## Domínios

- spec: `openspec/specs/agenda/spec.md` (AGD-03 sincronizar a agenda em reservas, AGD-04 enviar lembrete)
- spec: `openspec/specs/whatsapp/spec.md` (WPP-07 registrar contato, WPP-08 tratar resposta do lembrete)
- spec: `openspec/specs/empresa/spec.md` (EMP-07 configurar lembrete, EMP-08 listar reservas e ligar contato, EMP-09 listar contatos e ver o número completo)

Vem depois de `proteger-dados-nos-logs`, `corrigir-reserva-automatica` e `exibir-previsao-tempo-reservas`: as quatro mudam `ControllerEmpresa`, os DTOs de empresa, o perfil do cliente, `swagger.yaml` e partes do fluxo de reserva. A tela de reservas pode reaproveitar o selo de risco da previsão.
