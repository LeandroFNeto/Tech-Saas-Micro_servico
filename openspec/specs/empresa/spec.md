# Empresa

Comportamento observável do cadastro e do perfil da empresa. Cada intenção HTTP usa um DTO próprio. A resposta de criação e de atualização é `EmpresaResponseDTO`.

## Requisitos

### EMP-01 Criar empresa

O sistema deve cadastrar uma empresa a partir de `EmpresaCreateDTO` e devolver `EmpresaResponseDTO`. Exige `x-admin-token`. A sessão WhatsApp é única.

Contrato: `especificacao/back/paths/empresas.yaml`
Prova: `src/test/resources/features/empresas.feature`

Cenário: admin cria a empresa

- Dado o token administrativo válido
- Quando o admin envia POST `/empresas` com nome, sessão, ramo, e-mail e senha
- Então a API responde 201
- E o corpo segue `EmpresaResponseDTO`
- E a sessão devolvida é a que foi enviada

Cenário: token administrativo inválido

- Quando o POST `/empresas` chega com token inválido
- Então a API responde 401

Cenário: sessão WhatsApp já ocupada

- Dado uma empresa já persistida com aquela sessão
- Quando o admin tenta criar outra empresa com a mesma sessão
- Então a API responde 400

### EMP-02 Cliente atualiza o próprio perfil

O sistema deve aplicar `EmpresaUpdateDTO` na sessão da empresa: textos, links e aparência. Essa rota não altera cobrança nem infraestrutura. Na tela, o cliente envia a foto principal e as fotos da galeria pelo widget do Cloudinary e o sistema guarda só as URLs.

A reserva automática (`permiteReservaAutomatica`) é opcional e só o admin a altera, até os planos liberarem a opção. Ela nasce `false` em empresa nova quando o POST `/empresas` não a envia. No PUT do cliente, valor igual ao atual é aceito e ignorado; valor diferente responde 400 com "A reserva automática é liberada pelo administrador." e nada do PUT é gravado. A tela do perfil mostra a opção só para leitura, com o aviso de que o administrador libera, e não envia o campo. Ligar exige `googleCalendarId`: o POST do admin com `true` sem agenda e o PUT do admin que passa a opção de desligada para ligada sem agenda final (a enviada no mesmo PUT ou a já gravada) respondem 400 com "Cadastre a agenda do Google antes de ligar a reserva automática.". O PUT do admin em empresa já ligada não repete a checagem: apagar a agenda é aceito e a empresa passa a atender como reserva manual (WPP-07). A agenda continua opcional para empresa sem reserva automática.

Contrato: `especificacao/back/paths/empresas-cliente.yaml`, `especificacao/back/paths/empresas.yaml` e `especificacao/back/paths/empresas-admin.yaml` (regra da reserva automática)
Conteúdo da tela: `especificacao/front/cliente-perfil.content.json`
Prova: `src/test/resources/features/empresas.feature`, `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`, `frontend/src/app/features/client/perfil/perfil.component.spec.ts` (fotos e reserva automática só para leitura) e `frontend/src/app/features/admin/empresa-form/empresa-form.component.spec.ts` (aviso da agenda)

Cenário: perfil do cliente é atualizado

- Dado uma empresa persistida na sessão
- Quando o cliente envia PUT `/empresas/{sessao}/cliente` com nome, saudação e link do mapa
- Então a API responde 200
- E o corpo segue `EmpresaResponseDTO`
- E nome e link do mapa devolvidos são os enviados

Cenário: fotos do perfil são enviadas pelo Cloudinary e podem ser abertas

- Dado o perfil do cliente com foto principal e fotos na galeria
- Quando a tela do perfil é aberta
- Então a foto principal e cada foto da galeria têm um link que abre a imagem em nova aba, sem acesso ao `window.opener`
- E os botões "Enviar foto principal" e "Enviar fotos" abrem o widget do Cloudinary
- E, se o script do Cloudinary não carregou, a tela avisa "O widget do Cloudinary não carregou. Recarregue a página."

Cenário: cliente tenta mudar a reserva automática

- Dado uma empresa com a reserva automática desligada
- Quando o cliente envia PUT `/empresas/{sessao}/cliente` com nome novo e `permiteReservaAutomatica` `true`
- Então a API responde 400 com "A reserva automática é liberada pelo administrador."
- E uma leitura seguinte mostra a reserva automática desligada e o nome antigo

Cenário: cliente reenvia o valor atual

- Quando o cliente envia o PUT com `permiteReservaAutomatica` igual ao atual
- Então a API responde 200 e grava o resto do perfil

Cenário: empresa nova nasce com a reserva automática desligada

- Quando o admin envia POST `/empresas` sem `permiteReservaAutomatica` e sem agenda
- Então a API responde 201
- E a reserva automática vem `false`

Cenário: admin liga sem agenda

- Quando o admin envia POST `/empresas` com `permiteReservaAutomatica` `true` e sem agenda, ou PUT `/empresas/{sessao}/admin` que liga a opção numa empresa desligada e sem agenda final
- Então a API responde 400 com "Cadastre a agenda do Google antes de ligar a reserva automática."

Cenário: admin liga com agenda

- Quando o admin envia o POST ou o PUT do admin ligando a opção com `googleCalendarId` preenchido
- Então a API responde 201 no POST ou 200 no PUT
- E a reserva automática vem `true`

Cenário: admin apaga a agenda de empresa já ligada

- Dado uma empresa com a reserva automática ligada e agenda
- Quando o admin envia PUT `/empresas/{sessao}/admin` com a agenda vazia
- Então a API responde 200
- E a reserva automática continua `true`

### EMP-03 Admin atualiza a infraestrutura

O sistema deve aplicar `EmpresaUpdateAdminDTO` (sessão, tipo de locação, agenda Google e módulos). Exige `x-admin-token`.

Contrato: `especificacao/back/paths/empresas-admin.yaml`
Prova: `src/test/resources/features/empresas.feature`

Cenário: admin atualiza com token

- Dado uma empresa persistida e o token administrativo válido
- Quando o admin envia PUT `/empresas/{sessao}/admin`
- Então a API responde 200
- E o corpo segue `EmpresaResponseDTO`

Cenário: admin sem token

- Quando o PUT `/empresas/{sessao}/admin` chega sem token
- Então a API responde 401

### EMP-04 Admin redefine a senha do cliente

O sistema deve gravar um hash novo da senha do cliente daquela sessão, sem conferir a senha atual. Exige `x-admin-token`.

Contrato: `especificacao/back/paths/empresas-reset-senha.yaml` (`ResetSenhaAdminDTO`)
Prova: `src/test/java/com/example/demo/servico/ServicoAuthResetSenhaTest.java` (serviço) e `src/test/resources/features/empresas.feature` (HTTP 204, 400, 401 e 404).

Cenário: reset grava hash novo

- Dado um usuário ligado à sessão da empresa
- Quando o admin redefine a senha dessa sessão
- Então a senha persistida é o hash da senha nova
- E a senha anterior não é consultada

### EMP-05 Cliente configura a localização e a previsão do tempo

O sistema deve permitir que o cliente informe a cidade do espaço e ligue ou desligue a previsão do tempo pelo PUT do próprio perfil. A previsão nasce desligada. Ao receber uma cidade nova, o sistema a resolve no provedor de previsão (centro da cidade, sem bairro) e devolve o nome, o estado e o país encontrados para o cliente conferir. Cada empresa troca a cidade no máximo 5 vezes por dia. Cidade não encontrada, limite de trocas ou provedor indisponível respondem erro e nada do PUT é gravado. A chave do provedor fica só no back.

Contrato: `especificacao/back/paths/empresas-cliente.yaml` (`EmpresaUpdateDTO`, `EmpresaResponseDTO`)
Prova: `src/test/resources/features/empresas.feature` e `src/test/java/com/example/demo/servico/previsao/ServicoPrevisaoTempoTest.java`

Cenário: empresa sem configuração mostra a previsão desligada

- Dado uma empresa que nunca configurou a previsão
- Quando o perfil dela é lido
- Então a previsão aparece desligada

Cenário: cidade válida é resolvida para conferir

- Dado o cliente da própria empresa
- Quando ele envia PUT `/empresas/{sessao}/cliente` com uma cidade existente e a previsão ligada
- Então a API responde 200
- E a resposta traz a cidade resolvida com nome, estado e país
- E a cidade fica gravada na empresa

Cenário: cidade inexistente

- Quando o cliente envia uma cidade que o provedor não encontra
- Então a API responde 400 com "Cidade não encontrada. Confira o nome da cidade."
- E nada do PUT é gravado

Cenário: provedor indisponível

- Dado o provedor de previsão fora do ar, sem resposta em 3 s ou sem chave configurada
- Quando o cliente envia uma cidade nova
- Então a API responde 503 com "Não foi possível verificar a cidade agora. Tente novamente em alguns minutos."
- E nada do PUT é gravado
- E essa tentativa não conta no limite diário de trocas

Cenário: sexta troca de cidade no mesmo dia

- Dado uma empresa que já trocou a cidade 5 vezes hoje
- Quando o cliente envia outra cidade
- Então a API responde 429 com "Muitas alterações de cidade hoje. Tente amanhã."
- E nada do PUT é gravado

### EMP-06 Painel inicial do cliente com WhatsApp, previsão e atalho para o chatbot

O sistema deve abrir em `/cliente` um painel inicial com a conexão do WhatsApp (regra de WPP-04, sem mudança), a previsão dos próximos dias (até 8) e um atalho para a configuração do chatbot em `/cliente/chatbot`. Cada dia mostra o tempo, as temperaturas, a chance e o volume de chuva, a marcação "Reservado" quando a data está ocupada no Google Agenda da empresa e o risco de desmarcação BAIXO, MÉDIO ou ALTO. O risco é ALTO com chance de chuva de 70% ou mais, temporal ou 10 mm ou mais; MÉDIO com chance de 40% a 69% ou de 2 mm a 9,9 mm; BAIXO no resto, comparando a chance arredondada para inteiro e a chuva com 1 casa decimal. A previsão de cada cidade é renovada no máximo a cada 8 horas e a ocupação é recalculada a cada consulta, no fuso `America/Sao_Paulo`. Quando há dias na tela, aparece a atribuição "Dados meteorológicos: OpenWeather" e o horário da atualização.

A consulta da previsão (GET `/empresas/{sessao}/previsao`) e o PUT do cliente só aceitam o admin ou o cliente da própria empresa. Quem não é admin recebe a mesma resposta 403 para sessão inexistente e para empresa alheia; o 404 de sessão inexistente sai só para o admin. O ID da agenda Google da empresa só é devolvido ao admin.

Contrato: `especificacao/back/paths/empresas-previsao.yaml` (`PrevisaoTempoResponseDTO`, `PrevisaoDiaDTO`), `especificacao/back/paths/empresas-cliente.yaml` e `especificacao/back/paths/empresas-search.yaml`
Conteúdo da tela: `especificacao/front/cliente-painel.content.json`
Prova: `src/test/java/com/example/demo/servico/previsao/CalculadoraRiscoDesmarcacaoTest.java`, `src/test/java/com/example/demo/servico/previsao/ServicoPrevisaoTempoTest.java`, `src/test/resources/features/empresas.feature`, `frontend/src/app/features/client/painel/painel.component.spec.ts` e `frontend/src/app/core/services/previsao.service.spec.ts`

Cenário: painel inicial do cliente

- Dado o cliente autenticado
- Quando ele abre `/cliente`
- Então vê a conexão do WhatsApp, o bloco da previsão e o atalho "Configuração do chatbot"
- E o atalho leva a `/cliente/chatbot`

Cenário: dia reservado com chuva forte

- Dado a previsão ligada, a cidade resolvida e uma data ocupada no Google Agenda
- E o provedor prevê para essa data chance de chuva de 70% ou mais
- Quando o cliente consulta a previsão
- Então a API responde 200
- E esse dia vem marcado como reservado e com risco ALTO
- E a tela mostra a atribuição "Dados meteorológicos: OpenWeather"

Cenário: previsão desligada, sem cidade ou provedor fora do ar

- Quando a previsão é consultada nessas condições
- Então a API responde 200 indicando previsão indisponível e sem dias
- E a tela mostra o aviso correspondente sem quebrar o painel

Cenário: agenda Google com erro

- Dado a empresa com agenda configurada e o Google Agenda em erro
- Quando a previsão é consultada
- Então nenhum dia sai como reservado
- E a tela mostra "Reservas indisponíveis no momento"

Cenário: sem token

- Quando o GET `/empresas/{sessao}/previsao` chega sem token
- Então a API responde 401

Cenário: cliente de outra empresa ou sessão inexistente

- Dado um cliente autenticado que não é admin
- Quando ele consulta a previsão ou envia o PUT de uma empresa que não é a dele, ou de uma sessão inexistente
- Então a API responde 403 nos dois casos

Cenário: admin consulta sessão inexistente

- Quando o admin consulta a previsão de uma sessão que não existe
- Então a API responde 404

Cenário: agenda Google oculta ao cliente

- Quando o cliente lê a própria empresa pela busca por sessão
- Então o ID da agenda Google vem vazio
- E o admin, na mesma leitura, recebe o ID

### EMP-07 Empresa define duração, intervalo, estado e horários de funcionamento

O sistema deve permitir que o admin ou o cliente da própria empresa leia (GET) e atualize (PUT) o funcionamento da reserva por hora em `/empresas/{sessao}/funcionamento`: duração do atendimento (múltiplo de 30, de 30 a 720 minutos; padrão 60), intervalo entre atendimentos (0 a 120; padrão 0), `uf` (uma das 27 siglas; vazio remove) e os horários por perfil. Os perfis são `SEGUNDA` a `DOMINGO` e `FERIADO`; cada perfil tem aberto ou fechado, abre e fecha (`HH:mm`) e uma duração própria opcional, que vazia herda a da empresa. Campo nulo no PUT não altera; a lista de horários enviada substitui a anterior, e a gravação é uma transação só.

O PUT responde 400 quando a duração ou o intervalo saem da faixa, quando a `uf` não existe, quando abre não é antes de fecha, quando um perfil se repete ou quando a janela não comporta ao menos um atendimento da duração aplicada. Sem configuração valem todos os dias das 08:00 às 22:00 e `FERIADO` igual a `DOMINGO`. Empresa que já tinha locação por hora recebe os padrões sem migração e segue em reserva manual até o admin ligar a reserva automática. Mudar a duração não mexe em reservas já criadas.

Qual janela vale numa data: o dia especial da empresa (EMP-08); depois feriado nacional ou do estado da `uf` (perfil `FERIADO`, ou `DOMINGO` se `FERIADO` não existe); depois o dia da semana. Os feriados nacionais são calculados pelo sistema (fixos mais Carnaval, Sexta-feira Santa, Páscoa e Corpus Christi, e Consciência Negra a partir de 2024); os estaduais vêm de uma tabela fixa por UF que precisa de conferência com fonte oficial a cada virada de ano; empresa sem `uf` só segue os nacionais. Feriado municipal é dia especial lançado pela empresa.

A tela é a aba "Funcionamento" de `/cliente/chatbot`, com modo simples (um horário e a duração) e "Personalizar" (perfis, intervalo, UF e dias especiais).

Contrato: `especificacao/back/paths/empresas-funcionamento.yaml` (`FuncionamentoDTO`, `HorarioFuncionamentoDTO`, `FuncionamentoUpdateDTO`)
Conteúdo da tela: `especificacao/front/cliente-perfil.content.json`
Prova: `src/test/resources/features/empresas.feature`, `src/test/java/com/example/demo/controller/ControllerFuncionamentoTest.java`, `src/test/java/com/example/demo/servico/ServicoFuncionamentoTest.java`, `src/test/java/com/example/demo/servico/CalendarioFeriadosTest.java`, `frontend/src/app/features/client/funcionamento/funcionamento.component.spec.ts` e `frontend/src/app/core/services/funcionamento.service.spec.ts`

Cenário: empresa sem configuração

- Dado uma empresa sem funcionamento salvo
- Quando o admin ou o cliente da empresa faz GET em `/empresas/{sessao}/funcionamento`
- Então a API responde 200 com duração 60, intervalo 0 e listas vazias

Cenário: gravar o funcionamento

- Quando o PUT envia duração 90, intervalo 15, `uf` "sp" e horários de `SABADO` e `DOMINGO`
- Então a API responde 200
- E o GET seguinte devolve esses valores com a `uf` "SP"

Cenário: valores inválidos

- Quando o PUT envia duração 45, intervalo 121, `uf` "XX", abre depois de fecha, janela menor que a duração ou perfil repetido
- Então a API responde 400 em cada caso

Cenário: sem token ou cliente de outra empresa

- Quando o GET chega sem token
- Então a API responde 401
- E, com o JWT de um cliente de outra empresa, responde 403

### EMP-08 Empresa cadastra dias especiais

O sistema deve permitir que o admin ou o cliente da própria empresa cadastre (POST `/empresas/{sessao}/dias-especiais`) e remova (DELETE `/empresas/{sessao}/dias-especiais/{id}`) dias especiais: feriado municipal, folga ou horário próprio, com motivo opcional de até 120 caracteres que o cliente final vê. A data vai de hoje a 180 dias à frente, no fuso `America/Sao_Paulo`, e só um dia especial por data; data fora da faixa, repetida (inclusive em cadastros simultâneos) ou horário inválido responde 400. O dia especial vale mais que feriado e que o dia da semana.

Fechar uma data não cancela reservas. A resposta do POST traz em `reservasNaData` os eventos que já existem na agenda naquela data (hora e título, que podem incluir eventos manuais do dono), e a tela mostra o aviso "Há reservas nesta data". Falha ao ler a agenda não impede o cadastro: a lista vem vazia. O DELETE só remove dia especial da empresa da sessão; id de outra empresa responde 404.

Contrato: `especificacao/back/paths/empresas-dias-especiais.yaml` e `especificacao/back/paths/empresas-dias-especiais-id.yaml` (`DataEspecialCreateDTO`, `DataEspecialDTO`)
Conteúdo da tela: `especificacao/front/cliente-perfil.content.json`
Prova: `src/test/resources/features/empresas.feature`, `src/test/java/com/example/demo/controller/ControllerFuncionamentoTest.java`, `frontend/src/app/features/client/funcionamento/funcionamento.component.spec.ts`

Cenário: fechar uma data com reservas

- Dado uma empresa com agenda e um evento na data
- Quando o POST cadastra a data como fechada com o motivo "Feriado municipal"
- Então a API responde 201 com `reservasNaData` contendo o evento
- E o evento continua na agenda
- E o GET do funcionamento lista o dia especial

Cenário: horário próprio

- Quando o POST cadastra uma data daqui a 20 dias das "10:00" às "16:00"
- Então a API responde 201 com `abre` "10:00", `fecha` "16:00" e `reservasNaData` vazio

Cenário: data inválida

- Quando o POST cadastra uma data daqui a 200 dias, ou uma data que já tem dia especial
- Então a API responde 400

Cenário: remover

- Quando o DELETE remove o dia especial cadastrado
- Então a API responde 204 e o GET não lista mais o dia

Cenário: dia especial de outra empresa

- Dado um dia especial da empresa A
- Quando o DELETE usa a sessão da empresa B com o id dele
- Então a API responde 404 e o dia da empresa A continua listado

## Superfície HTTP ainda sem requisito

GET `/empresas` (coleção HAL, token admin) e o item Spring Data REST em `especificacao/back/paths/empresas-id.yaml` existem no contrato. Eles ficam de fora destes requisitos até uma change dizer o que o painel deve observar ali. O comportamento de produto desta spec é o das rotas com DTO nomeado.
