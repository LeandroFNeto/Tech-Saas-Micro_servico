# language: pt
# Origem: swagger.yaml → /empresas, /empresas/{sessao}/cliente, /empresas/{sessao}/admin
# Schemas: EmpresaCreateDTO, EmpresaUpdateDTO, EmpresaUpdateAdminDTO, EmpresaResponseDTO
Funcionalidade: Contratos de empresa do SaaS
  Como cliente da API documentada em swagger.yaml
  Quero que POST e PUT usem DTOs isolados por intenção
  Para o front-end não receber a entidade JPA

  Cenário: POST /empresas cria com EmpresaCreateDTO e devolve EmpresaResponseDTO
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome            | Recanto Vista Alegre |
      | sessaoWhatsapp  | sessao_recanto_01    |
      | ramoDeAtuacao   | LOCACAO              |
      | precoBase       | 500.0                |
      | email           | cliente@recanto.com  |
      | senha           | SenhaCliente123      |
    Então o status HTTP deve ser 201
    E o corpo deve seguir o schema EmpresaResponseDTO
    E o campo "sessaoWhatsapp" deve ser "sessao_recanto_01"

  Cenário: POST /empresas rejeita token administrativo inválido
    Dado o token administrativo "token-invalido"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Recanto Vista Alegre |
      | sessaoWhatsapp | sessao_recanto_02    |
      | ramoDeAtuacao  | LOCACAO              |
      | email          | outro@recanto.com    |
      | senha          | SenhaCliente123      |
    Então o status HTTP deve ser 401

  Cenário: POST /empresas rejeita sessão WhatsApp já ocupada
    Dado uma empresa persistida com sessão "sessao_ocupada" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Outro Recanto  |
      | sessaoWhatsapp | sessao_ocupada |
      | ramoDeAtuacao  | LOCACAO        |
      | email          | ocupado@recanto.com |
      | senha          | SenhaCliente123     |
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/cliente atualiza só o perfil com EmpresaUpdateDTO
    Dado uma empresa persistida com sessão "sessao_cliente" e ramo "LOCACAO"
    Quando eu envio PUT /empresas/sessao_cliente/cliente com o EmpresaUpdateDTO:
      | nome                       | Recanto Vista Alegre Atualizado |
      | mensagemSaudacao           | Olá! Como posso ajudar?         |
      | linkGoogleMaps             | https://maps.app.goo.gl/exemplo |
      | permiteReservaAutomatica   | false                           |
    Então o status HTTP deve ser 200
    E o corpo deve seguir o schema EmpresaResponseDTO
    E o campo "nome" deve ser "Recanto Vista Alegre Atualizado"
    E o campo "linkGoogleMaps" deve ser "https://maps.app.goo.gl/exemplo"

  Cenário: PUT /empresas/{sessao}/cliente com permiteReservaAutomatica diferente do atual retorna 400 e não grava
    Dado uma empresa persistida com sessão "sessao_cliente_auto" e ramo "LOCACAO"
    Quando eu envio PUT /empresas/sessao_cliente_auto/cliente com o EmpresaUpdateDTO:
      | nome                       | Nome que não deve gravar |
      | permiteReservaAutomatica   | true                     |
    Então o status HTTP deve ser 400
    Dado o token administrativo "sua-chave-admin"
    Quando eu busco a empresa sessao_cliente_auto pelo painel com o token administrativo
    Então o status HTTP deve ser 200
    E o corpo deve conter "\"permiteReservaAutomatica\":false"
    E o corpo não deve conter "Nome que não deve gravar"

  Cenário: PUT /empresas/{sessao}/cliente com permiteReservaAutomatica igual ao atual é aceito
    Dado uma empresa persistida com sessão "sessao_cliente_igual" e ramo "LOCACAO"
    Quando eu envio PUT /empresas/sessao_cliente_igual/cliente com o EmpresaUpdateDTO:
      | nome                       | Nome gravado |
      | permiteReservaAutomatica   | false        |
    Então o status HTTP deve ser 200
    E o campo "nome" deve ser "Nome gravado"
    E o corpo deve conter "\"permiteReservaAutomatica\":false"

  Cenário: POST /empresas ligando a reserva automática sem agenda retorna 400
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome                     | Espaço Sem Agenda    |
      | sessaoWhatsapp           | sessao_auto_sem_ag   |
      | ramoDeAtuacao            | LOCACAO              |
      | email                    | semagenda@recanto.com |
      | senha                    | SenhaCliente123      |
      | permiteReservaAutomatica | true                 |
    Então o status HTTP deve ser 400

  Cenário: POST /empresas sem agenda e sem ligar a reserva automática funciona
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Manual         |
      | sessaoWhatsapp | sessao_auto_manual    |
      | ramoDeAtuacao  | LOCACAO               |
      | email          | manual@recanto.com    |
      | senha          | SenhaCliente123       |
    Então o status HTTP deve ser 201
    E o corpo deve conter "\"permiteReservaAutomatica\":false"

  Cenário: POST /empresas ligando a reserva automática com agenda funciona
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome                     | Espaço Com Agenda                     |
      | sessaoWhatsapp           | sessao_auto_com_ag                    |
      | ramoDeAtuacao            | LOCACAO                               |
      | email                    | comagenda@recanto.com                 |
      | senha                    | SenhaCliente123                       |
      | googleCalendarId         | agenda-auto@group.calendar.google.com |
      | permiteReservaAutomatica | true                                  |
    Então o status HTTP deve ser 201
    E o corpo deve conter "\"permiteReservaAutomatica\":true"

  Cenário: PUT /empresas/{sessao}/admin ligando a reserva automática sem agenda retorna 400
    Dado uma empresa persistida com sessão "sessao_admin_sem_ag" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_admin_sem_ag/admin com o EmpresaUpdateAdminDTO:
      | permiteReservaAutomatica | true |
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/admin ligando a reserva automática com agenda funciona
    Dado uma empresa persistida com sessão "sessao_admin_com_ag" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_admin_com_ag/admin com o EmpresaUpdateAdminDTO:
      | permiteReservaAutomatica | true                                  |
      | googleCalendarId         | agenda-admin@group.calendar.google.com |
    Então o status HTTP deve ser 200
    E o corpo deve conter "\"permiteReservaAutomatica\":true"

  Cenário: PUT /empresas/{sessao}/admin em empresa já ligada apaga a agenda sem erro
    Dado uma empresa persistida com sessão "sessao_admin_ja_ligada" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_admin_ja_ligada/admin com o EmpresaUpdateAdminDTO:
      | permiteReservaAutomatica | true                                  |
      | googleCalendarId         | agenda-ligada@group.calendar.google.com |
    Então o status HTTP deve ser 200
    Quando eu envio PUT /empresas/sessao_admin_ja_ligada/admin com o EmpresaUpdateAdminDTO:
      | permiteReservaAutomatica | true |
      | googleCalendarId         | [empty] |
    Então o status HTTP deve ser 200
    E o corpo deve conter "\"permiteReservaAutomatica\":true"

  Cenário: PUT /empresas/{sessao}/admin exige token e usa EmpresaUpdateAdminDTO
    Dado uma empresa persistida com sessão "sessao_admin" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_admin/admin com o EmpresaUpdateAdminDTO:
      | locacaoPorHora | false |
    Então o status HTTP deve ser 200
    E o corpo deve seguir o schema EmpresaResponseDTO

  Cenário: PUT /empresas/{sessao}/admin sem token retorna 401
    Dado uma empresa persistida com sessão "sessao_admin_negado" e ramo "LOCACAO"
    Quando eu envio PUT /empresas/sessao_admin_negado/admin sem token com o EmpresaUpdateAdminDTO:
      | locacaoPorHora | true |
    Então o status HTTP deve ser 401

  Cenário: PUT /empresas/{sessao}/reset-senha grava a senha nova
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Reset      |
      | sessaoWhatsapp | sessao_reset_ok   |
      | ramoDeAtuacao  | LOCACAO           |
      | email          | reset@recanto.com |
      | senha          | SenhaCliente123   |
    Quando eu envio PUT /empresas/sessao_reset_ok/reset-senha com o ResetSenhaAdminDTO:
      | novaSenha | NovaSenha456 |
    Então o status HTTP deve ser 204

  Cenário: PUT /empresas/{sessao}/reset-senha rejeita senha curta
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_reset_curta/reset-senha com o ResetSenhaAdminDTO:
      | novaSenha | 12345 |
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/reset-senha rejeita token administrativo inválido
    Dado o token administrativo "token-invalido"
    Quando eu envio PUT /empresas/sessao_reset_negado/reset-senha com o ResetSenhaAdminDTO:
      | novaSenha | NovaSenha456 |
    Então o status HTTP deve ser 401

  Cenário: PUT /empresas/{sessao}/reset-senha de sessão inexistente retorna 404
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_reset_ausente/reset-senha com o ResetSenhaAdminDTO:
      | novaSenha | NovaSenha456 |
    Então o status HTTP deve ser 404

  Cenário: PUT /empresas/{sessao}/cliente grava cidade válida e liga a previsão
    Dado uma empresa persistida com sessão "sessao_prev_ok" e ramo "LOCACAO"
    E o provedor de previsão reconhece qualquer cidade
    Quando a empresa "sessao_prev_ok" troca a cidade da previsão para "Atibaia"
    Então o status HTTP deve ser 200
    E o campo "cidadePrevisaoResolvida" deve ser "Atibaia, São Paulo, BR"
    E o corpo deve conter "\"previsaoTempoAtiva\":true"
    E a empresa "sessao_prev_ok" deve ter a cidade de previsão "Atibaia" gravada

  Cenário: PUT /empresas/{sessao}/cliente rejeita cidade inexistente com 400
    Dado uma empresa persistida com sessão "sessao_prev_invalida" e ramo "LOCACAO"
    E o provedor de previsão não reconhece a cidade
    Quando a empresa "sessao_prev_invalida" troca a cidade da previsão para "Xyzcidade"
    Então o status HTTP deve ser 400
    E a empresa "sessao_prev_invalida" não deve ter cidade de previsão gravada

  Cenário: PUT /empresas/{sessao}/cliente com provedor fora do ar devolve 503 sem gravar nada
    Dado uma empresa persistida com sessão "sessao_prev_fora" e ramo "LOCACAO"
    E o provedor de previsão está fora do ar
    Quando a empresa "sessao_prev_fora" troca a cidade da previsão para "Atibaia"
    Então o status HTTP deve ser 503
    E a empresa "sessao_prev_fora" não deve ter cidade de previsão gravada

  Cenário: PUT /empresas/{sessao}/cliente na sexta troca de cidade no mesmo dia devolve 429
    Dado uma empresa persistida com sessão "sessao_prev_limite" e ramo "LOCACAO"
    E o provedor de previsão reconhece qualquer cidade
    Quando a empresa "sessao_prev_limite" troca a cidade da previsão 5 vezes
    Então o status HTTP deve ser 200
    Quando a empresa "sessao_prev_limite" troca a cidade da previsão para "Bragança Paulista"
    Então o status HTTP deve ser 429

  Cenário: GET /empresas/{sessao}/previsao devolve 200 ao admin com os dias e o risco
    Dado uma empresa persistida com sessão "sessao_prev_get" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    E o provedor de previsão reconhece qualquer cidade
    E o provedor de previsão devolve hoje com chuva forte
    Quando a empresa "sessao_prev_get" troca a cidade da previsão para "Atibaia"
    E eu envio GET /empresas/sessao_prev_get/previsao com o token administrativo
    Então o status HTTP deve ser 200
    E o corpo deve conter "\"disponivel\":true"
    E o corpo deve conter "\"risco\":\"ALTO\""
    E o corpo deve conter "\"reservado\":false"

  Cenário: GET /empresas/{sessao}/previsao devolve 200 ao cliente da própria empresa
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Previsão Dono |
      | sessaoWhatsapp | sessao_prev_dono     |
      | ramoDeAtuacao  | LOCACAO              |
      | email          | dono@recanto.com     |
      | senha          | SenhaCliente123      |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_prev_dono@gamb.com.br |
      | senha | SenhaCliente123              |
    E eu envio GET /empresas/sessao_prev_dono/previsao com o JWT do login
    Então o status HTTP deve ser 200
    E o corpo deve conter "\"disponivel\":false"

  Cenário: GET /empresas/{sessao}/previsao sem token retorna 401
    Dado uma empresa persistida com sessão "sessao_prev_401" e ramo "LOCACAO"
    Quando eu envio GET /empresas/sessao_prev_401/previsao sem token
    Então o status HTTP deve ser 401

  Cenário: GET /empresas/{sessao}/previsao de cliente de outra empresa retorna 403
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Alvo      |
      | sessaoWhatsapp | sessao_prev_alvo |
      | ramoDeAtuacao  | LOCACAO          |
      | email          | alvo@recanto.com |
      | senha          | SenhaCliente123  |
    E eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Intruso      |
      | sessaoWhatsapp | sessao_prev_intruso |
      | ramoDeAtuacao  | LOCACAO             |
      | email          | intruso@recanto.com |
      | senha          | SenhaCliente123     |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_prev_intruso@gamb.com.br |
      | senha | SenhaCliente123                 |
    E eu envio GET /empresas/sessao_prev_alvo/previsao com o JWT do login
    Então o status HTTP deve ser 403

  Cenário: GET /empresas/{sessao}/previsao com o token legado de cliente retorna 403
    Dado uma empresa persistida com sessão "sessao_prev_legado" e ramo "LOCACAO"
    Quando eu envio GET /empresas/sessao_prev_legado/previsao com o token legado de cliente
    Então o status HTTP deve ser 403

  Cenário: GET /empresas/{sessao}/previsao de sessão inexistente retorna 404
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio GET /empresas/sessao_prev_ausente/previsao com o token administrativo
    Então o status HTTP deve ser 404

  Cenário: PUT /empresas/{sessao}/cliente com o token legado retorna 403 sem consultar o provedor
    Dado uma empresa persistida com sessão "sessao_put_legado" e ramo "LOCACAO"
    E o provedor de previsão reconhece qualquer cidade
    E o provedor de previsão ainda não foi consultado
    Quando eu envio PUT /empresas/sessao_put_legado/cliente com a cidade "Atibaia" e o token legado de cliente
    Então o status HTTP deve ser 403
    E o provedor de previsão não deve ter sido consultado
    E a empresa "sessao_put_legado" não deve ter cidade de previsão gravada

  Cenário: PUT e GET do painel de cliente de outra empresa retornam 403 sem gastar contador nem cota
    Dado o token administrativo "sua-chave-admin"
    E o provedor de previsão reconhece qualquer cidade
    E o provedor de previsão ainda não foi consultado
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Alvo Put     |
      | sessaoWhatsapp | sessao_put_alvo     |
      | ramoDeAtuacao  | LOCACAO             |
      | email          | alvoput@recanto.com |
      | senha          | SenhaCliente123     |
    E eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Intruso Put     |
      | sessaoWhatsapp | sessao_put_intruso     |
      | ramoDeAtuacao  | LOCACAO                |
      | email          | intrusoput@recanto.com |
      | senha          | SenhaCliente123        |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_put_intruso@gamb.com.br |
      | senha | SenhaCliente123                |
    E eu envio PUT /empresas/sessao_put_alvo/cliente com a cidade "Atibaia" e o JWT do login
    Então o status HTTP deve ser 403
    Quando eu busco a empresa sessao_put_alvo pelo painel com o JWT do login
    Então o status HTTP deve ser 403
    E o provedor de previsão não deve ter sido consultado
    Quando a empresa "sessao_put_alvo" troca a cidade da previsão 5 vezes
    Então o status HTTP deve ser 200

  Cenário: painel devolve googleCalendarId ao admin e não devolve ao cliente da própria empresa
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome             | Espaço Agenda                         |
      | sessaoWhatsapp   | sessao_agenda_dono                    |
      | ramoDeAtuacao    | LOCACAO                               |
      | email            | agendadono@recanto.com                |
      | senha            | SenhaCliente123                       |
      | googleCalendarId | agenda-dono@group.calendar.google.com |
    E eu busco a empresa sessao_agenda_dono pelo painel com o token administrativo
    Então o status HTTP deve ser 200
    E o corpo deve conter "agenda-dono@group.calendar.google.com"
    Quando eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_agenda_dono@gamb.com.br |
      | senha | SenhaCliente123                |
    E eu busco a empresa sessao_agenda_dono pelo painel com o JWT do login
    Então o status HTTP deve ser 200
    E o corpo não deve conter "agenda-dono@group.calendar.google.com"

  Cenário: sessão inexistente e sessão de outra empresa dão a mesma resposta ao cliente
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Alvo Enum     |
      | sessaoWhatsapp | sessao_enum_alvo     |
      | ramoDeAtuacao  | LOCACAO              |
      | email          | alvoenum@recanto.com |
      | senha          | SenhaCliente123      |
    E eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Intruso Enum     |
      | sessaoWhatsapp | sessao_enum_intruso     |
      | ramoDeAtuacao  | LOCACAO                 |
      | email          | intrusoenum@recanto.com |
      | senha          | SenhaCliente123         |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_enum_intruso@gamb.com.br |
      | senha | SenhaCliente123                 |
    E eu envio GET /empresas/sessao_enum_alvo/previsao com o JWT do login
    E a resposta guardada como "outra"
    E eu envio GET /empresas/sessao_enum_inexistente/previsao com o JWT do login
    Então o status HTTP deve ser 403
    E a resposta atual deve ser igual à guardada como "outra"

  Cenário: PUT /empresas/{sessao}/cliente com 503 do provedor não conta na troca diária
    Dado uma empresa persistida com sessão "sessao_conta_503" e ramo "LOCACAO"
    E o provedor de previsão está fora do ar
    Quando a empresa "sessao_conta_503" troca a cidade da previsão 5 vezes
    Então o status HTTP deve ser 503
    Dado o provedor de previsão reconhece qualquer cidade
    Quando a empresa "sessao_conta_503" troca a cidade da previsão para "Atibaia"
    Então o status HTTP deve ser 200

  Cenário: PUT /empresas/{sessao}/cliente com 400 de cidade não encontrada conta na troca diária
    Dado uma empresa persistida com sessão "sessao_conta_400" e ramo "LOCACAO"
    E o provedor de previsão não reconhece a cidade
    Quando a empresa "sessao_conta_400" troca a cidade da previsão 5 vezes
    Então o status HTTP deve ser 400
    Dado o provedor de previsão reconhece qualquer cidade
    Quando a empresa "sessao_conta_400" troca a cidade da previsão para "Atibaia"
    Então o status HTTP deve ser 429

  Cenário: GET /empresas/{sessao}/funcionamento de empresa sem configuração devolve os padrões
    Dado uma empresa persistida com sessão "sessao_func_padrao" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio GET /empresas/sessao_func_padrao/funcionamento com o token administrativo
    Então o status HTTP deve ser 200
    E o corpo deve conter "\"duracaoAtendimentoMinutos\":60"
    E o corpo deve conter "\"intervaloEntreAtendimentosMinutos\":0"
    E o corpo deve conter "\"horarios\":[]"
    E o corpo deve conter "\"diasEspeciais\":[]"

  Cenário: PUT /empresas/{sessao}/funcionamento grava duração, intervalo, UF e horários por perfil
    Dado uma empresa persistida com sessão "sessao_func_grava" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_func_grava/funcionamento com o corpo:
      """
      {"duracaoAtendimentoMinutos":90,"intervaloEntreAtendimentosMinutos":15,"uf":"sp",
       "horarios":[{"perfil":"SABADO","fechado":false,"abre":"10:00","fecha":"20:00"},
                   {"perfil":"DOMINGO","fechado":true}]}
      """
    Então o status HTTP deve ser 200
    Quando eu envio GET /empresas/sessao_func_grava/funcionamento com o token administrativo
    Então o status HTTP deve ser 200
    E o corpo deve conter "\"duracaoAtendimentoMinutos\":90"
    E o corpo deve conter "\"intervaloEntreAtendimentosMinutos\":15"
    E o corpo deve conter "\"uf\":\"SP\""
    E o corpo deve conter "\"perfil\":\"SABADO\""
    E o corpo deve conter "\"perfil\":\"DOMINGO\""

  Cenário: PUT /empresas/{sessao}/funcionamento rejeita duração que não é múltiplo de 30
    Dado uma empresa persistida com sessão "sessao_func_dur" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_func_dur/funcionamento com o corpo:
      """
      {"duracaoAtendimentoMinutos":45}
      """
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/funcionamento rejeita intervalo acima de 120 minutos
    Dado uma empresa persistida com sessão "sessao_func_int" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_func_int/funcionamento com o corpo:
      """
      {"intervaloEntreAtendimentosMinutos":121}
      """
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/funcionamento rejeita UF inexistente
    Dado uma empresa persistida com sessão "sessao_func_uf" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_func_uf/funcionamento com o corpo:
      """
      {"uf":"XX"}
      """
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/funcionamento rejeita abertura depois do fechamento
    Dado uma empresa persistida com sessão "sessao_func_ordem" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_func_ordem/funcionamento com o corpo:
      """
      {"horarios":[{"perfil":"SEGUNDA","fechado":false,"abre":"18:00","fecha":"09:00"}]}
      """
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/funcionamento rejeita janela menor que a duração do atendimento
    Dado uma empresa persistida com sessão "sessao_func_curta" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_func_curta/funcionamento com o corpo:
      """
      {"duracaoAtendimentoMinutos":60,
       "horarios":[{"perfil":"SEGUNDA","fechado":false,"abre":"08:00","fecha":"08:30"}]}
      """
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/funcionamento rejeita perfil repetido
    Dado uma empresa persistida com sessão "sessao_func_dup" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_func_dup/funcionamento com o corpo:
      """
      {"horarios":[{"perfil":"SEXTA","fechado":true},{"perfil":"SEXTA","fechado":true}]}
      """
    Então o status HTTP deve ser 400

  Cenário: POST /empresas/{sessao}/dias-especiais fechando uma data com reservas avisa e não cancela
    Dado uma empresa persistida com sessão "sessao_func_aviso" e ramo "LOCACAO"
    E a empresa persistida tem a agenda Google "agenda-func-aviso@group.calendar.google.com"
    E a agenda tem o evento "14:00 às 15:00 - Reserva - Maria" em qualquer data
    E o token administrativo "sua-chave-admin"
    Quando eu cadastro em sessao_func_aviso o dia especial fechado daqui a 30 dias com o motivo "Feriado municipal"
    Então o status HTTP deve ser 201
    E o corpo deve conter "\"fechado\":true"
    E o corpo deve conter "\"motivo\":\"Feriado municipal\""
    E o corpo deve conter "Reserva - Maria"
    Quando eu envio GET /empresas/sessao_func_aviso/funcionamento com o token administrativo
    Então o corpo deve conter "Feriado municipal"

  Cenário: POST /empresas/{sessao}/dias-especiais com horário próprio é aceito
    Dado uma empresa persistida com sessão "sessao_func_horario" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu cadastro em sessao_func_horario o dia especial daqui a 20 dias das "10:00" às "16:00"
    Então o status HTTP deve ser 201
    E o corpo deve conter "\"abre\":\"10:00\""
    E o corpo deve conter "\"fecha\":\"16:00\""
    E o corpo deve conter "\"reservasNaData\":[]"

  Cenário: POST /empresas/{sessao}/dias-especiais rejeita data além de 180 dias
    Dado uma empresa persistida com sessão "sessao_func_longe" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu cadastro em sessao_func_longe o dia especial fechado daqui a 200 dias com o motivo "Longe demais"
    Então o status HTTP deve ser 400

  Cenário: POST /empresas/{sessao}/dias-especiais rejeita data que já tem dia especial
    Dado uma empresa persistida com sessão "sessao_func_repete" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu cadastro em sessao_func_repete o dia especial fechado daqui a 10 dias com o motivo "Primeiro"
    Então o status HTTP deve ser 201
    Quando eu cadastro em sessao_func_repete o dia especial fechado daqui a 10 dias com o motivo "Segundo"
    Então o status HTTP deve ser 400

  Cenário: DELETE /empresas/{sessao}/dias-especiais/{id} remove o dia especial
    Dado uma empresa persistida com sessão "sessao_func_remove" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu cadastro em sessao_func_remove o dia especial fechado daqui a 15 dias com o motivo "Folga"
    E eu removo em sessao_func_remove o dia especial guardado
    Então o status HTTP deve ser 204
    Quando eu envio GET /empresas/sessao_func_remove/funcionamento com o token administrativo
    Então o corpo não deve conter "Folga"

  Cenário: DELETE de dia especial de outra empresa retorna 404 e não apaga
    Dado uma empresa persistida com sessão "sessao_func_dona" e ramo "LOCACAO"
    E uma empresa persistida com sessão "sessao_func_outra" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu cadastro em sessao_func_dona o dia especial fechado daqui a 12 dias com o motivo "Só da dona"
    E eu removo em sessao_func_outra o dia especial guardado
    Então o status HTTP deve ser 404
    Quando eu envio GET /empresas/sessao_func_dona/funcionamento com o token administrativo
    Então o corpo deve conter "Só da dona"

  Cenário: GET /empresas/{sessao}/funcionamento sem token retorna 401
    Dado uma empresa persistida com sessão "sessao_func_401" e ramo "LOCACAO"
    Quando eu envio GET /empresas/sessao_func_401/funcionamento sem token
    Então o status HTTP deve ser 401

  Cenário: GET /empresas/{sessao}/funcionamento de cliente de outra empresa retorna 403
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Alvo Func     |
      | sessaoWhatsapp | sessao_func_alvo     |
      | ramoDeAtuacao  | LOCACAO              |
      | email          | alvofunc@recanto.com |
      | senha          | SenhaCliente123      |
    E eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Intruso Func     |
      | sessaoWhatsapp | sessao_func_intruso     |
      | ramoDeAtuacao  | LOCACAO                 |
      | email          | intrusofunc@recanto.com |
      | senha          | SenhaCliente123         |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_func_intruso@gamb.com.br |
      | senha | SenhaCliente123                 |
    E eu envio GET /empresas/sessao_func_alvo/funcionamento com o JWT do login
    Então o status HTTP deve ser 403
