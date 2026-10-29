# language: pt
# Arquitetura: EventoReservaConfirmada → ObservadorGoogleAgenda (@Async)
# Carga correspondente: k6/webhook-carga.js (HTTP não espera o Google)
Funcionalidade: Observer assíncrono da Google Agenda
  Como sistema de reservas
  Quero que a confirmação dispare o ObservadorGoogleAgenda sem bloquear o webhook
  Para a integração com a agenda ser resiliente sob tráfego

  Cenário: evento de reserva é consumido de forma assíncrona
    Dado uma empresa persistida com sessão "sessao_agenda" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-teste@group.calendar.google.com"
    E a data está livre na agenda
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o ObservadorGoogleAgenda deve agendar no Google sem bloquear o publicador
    E o cliente recebe a mensagem que contém "Reserva confirmada"

  Cenário: sucesso cria o evento, confirma e devolve a conversa ao início
    Dado uma empresa persistida com sessão "sessao_agenda_ok" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-ok@group.calendar.google.com"
    E a data está livre na agenda
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Reserva confirmada"
    E o cliente recebe a mensagem que contém "25-12-2026"
    E o Google recebe 1 tentativa de inserção
    E a conversa do cliente deve estar em "INICIO"

  Cenário: 409 do Google conta como sucesso
    Dado uma empresa persistida com sessão "sessao_agenda_409" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-409@group.calendar.google.com"
    E a data está livre na agenda
    E o Google responde 409 ao inserir o evento
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Reserva confirmada"
    E o Google recebe 1 tentativa de inserção
    E a conversa do cliente deve estar em "INICIO"

  Cenário: falha 5xx tenta 3 vezes e avisa que o responsável vai confirmar
    Dado uma empresa persistida com sessão "sessao_agenda_5xx" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-5xx@group.calendar.google.com"
    E a data está livre na agenda
    E o Google falha sempre com o status 503 ao inserir o evento
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Não consegui registrar sua reserva agora"
    E o Google recebe 3 tentativas de inserção
    E o cliente não recebe a mensagem que contém "Reserva confirmada"
    E a conversa do cliente deve estar em "INICIO"

  Cenário: 403 de permissão falha na hora, sem repetir
    Dado uma empresa persistida com sessão "sessao_agenda_403" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-403@group.calendar.google.com"
    E a data está livre na agenda
    E o Google falha sempre com o status 403 ao inserir o evento
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Não consegui registrar sua reserva agora"
    E o Google recebe 1 tentativa de inserção
    E a conversa do cliente deve estar em "INICIO"

  Cenário: 403 de limite de uso repete e depois confirma
    Dado uma empresa persistida com sessão "sessao_agenda_limite" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-limite@group.calendar.google.com"
    E a data está livre na agenda
    E o Google recusa a primeira inserção com 403 por "rateLimitExceeded"
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Reserva confirmada"
    E o Google recebe 2 tentativas de inserção
    E a conversa do cliente deve estar em "INICIO"

  Cenário: credencial do Google ausente falha sem repetir
    Dado uma empresa persistida com sessão "sessao_agenda_cred" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-cred@group.calendar.google.com"
    E a data está livre na agenda
    E a credencial do Google está ausente
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Não consegui registrar sua reserva agora"
    E o Google recebe 1 tentativa de inserção
    E a conversa do cliente deve estar em "INICIO"

  Cenário: a resposta da primeira tentativa se perde e a segunda acha o evento pelo id
    Dado uma empresa persistida com sessão "sessao_agenda_perdida" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-perdida@group.calendar.google.com"
    E a primeira tentativa chega ao Google e a resposta se perde
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Reserva confirmada"
    E o cliente não recebe a mensagem que contém "acabou de ser reservada"
    E o Google recebe 1 tentativa de inserção
    E a conversa do cliente deve estar em "INICIO"

  Cenário: falha no envio da mensagem ainda leva a conversa ao início
    Dado uma empresa persistida com sessão "sessao_agenda_envio" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-envio@group.calendar.google.com"
    E a data está livre na agenda
    E o envio da mensagem ao cliente falha
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então a conversa do cliente deve estar em "INICIO"
    E o Google recebe 1 tentativa de inserção

  Cenário: agenda não configurada avisa sem tentar o Google
    Dado uma empresa persistida com sessão "sessao_agenda_nenhuma" e ramo "LOCACAO"
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "Não consegui registrar sua reserva agora"
    E o Google recebe 0 tentativas de inserção
    E a conversa do cliente deve estar em "INICIO"

  Cenário: data ocupada na reconsulta volta a pedir a data sem inserir
    Dado uma empresa persistida com sessão "sessao_agenda_ocupada" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-ocupada@group.calendar.google.com"
    E a data está ocupada na agenda
    E o cliente está com a reserva em registro
    Quando o evento EventoReservaConfirmada é publicado para a data "25-12-2026"
    Então o cliente recebe a mensagem que contém "acabou de ser reservada"
    E o Google recebe 0 tentativas de inserção
    E o cliente não recebe a mensagem que contém "Reserva confirmada"
    E a conversa do cliente deve estar em "RESERVA_ESPERANDO_DATA"

  Cenário: duas confirmações da mesma data resultam em uma só reserva
    Dado uma empresa persistida com sessão "sessao_agenda_corrida" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-corrida@group.calendar.google.com"
    E o Google guarda o evento assim que ele é inserido
    Quando duas confirmações da mesma data "25-12-2026" são publicadas
    Então uma só confirmação e um só aviso de data ocupada são enviados
    E o Google recebe 1 tentativa de inserção

  Cenário: reserva por hora com várias datas cria um evento com hora para cada data
    Dado uma empresa persistida com sessão "sessao_agenda_hora" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-hora@group.calendar.google.com"
    E a agenda aceita o horário em todas as datas
    Quando o evento por hora é publicado para as datas "10-11-2026,17-11-2026" às "14:00"
    Então o cliente recebe a mensagem que contém "Reserva confirmada"
    E o cliente recebe a mensagem que contém "10-11-2026, 17-11-2026"
    E o cliente recebe a mensagem que contém "14:00"
    E o Google recebe 2 inserções por hora
    E a conversa do cliente deve estar em "INICIO"

  Cenário: uma data ocupada na reserva por hora derruba o pedido inteiro sem criar nada
    Dado uma empresa persistida com sessão "sessao_agenda_hora_ocupada" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-hora-ocupada@group.calendar.google.com"
    E a agenda aceita o horário em todas as datas
    E o horário está ocupado em "17-11-2026"
    Quando o evento por hora é publicado para as datas "10-11-2026,17-11-2026" às "14:00"
    Então o cliente recebe a mensagem que contém "Nada foi reservado"
    E o cliente recebe a mensagem que contém "17-11-2026"
    E o Google recebe 0 inserções por hora
    E o cliente não recebe a mensagem que contém "Reserva confirmada"
    E a conversa do cliente deve estar em "RESERVA_ESPERANDO_DATA"

  Cenário: falha 5xx na reserva por hora tenta 3 vezes, desfaz o pedido e avisa o cliente
    Dado uma empresa persistida com sessão "sessao_agenda_hora_5xx" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-hora-5xx@group.calendar.google.com"
    E a agenda aceita o horário em todas as datas
    E o Google falha sempre com o status 503 ao inserir o evento por hora
    Quando o evento por hora é publicado para as datas "10-11-2026,17-11-2026" às "14:00"
    Então o cliente recebe a mensagem que contém "Não consegui registrar sua reserva agora"
    E o Google recebe 3 inserções por hora
    E o Google remove 2 eventos do pedido por hora
    E o cliente não recebe a mensagem que contém "Reserva confirmada"
    E a conversa do cliente deve estar em "INICIO"
