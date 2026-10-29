# Design

## Decisão

1. **Contato por empresa.** Nova entidade `Contato` (`contatos`): `empresa`, `whatsappId` (o JID original recebido, usado para enviar), `nome`, `numeroIndice` (HMAC do `numeroChave` de `WhatsappUtil.normalizarParaChaveEstado`, usado para casar respostas e vínculos), `finalTelefone` (4 últimos dígitos, para a máscara), `primeiraConversaEm`, `ultimaConversaEm`, `naoContatar` (false) e `origem` (`CONVERSA` ou `AGENDA`). Único por empresa e `numeroIndice`. `whatsappId` e `nome` são gravados criptografados (decisão 12). O `ControllerWhatsapp` grava ou atualiza o contato depois de achar a empresa e antes de chamar o módulo, para todos os ramos. O nome só é preenchido quando a reserva pelo bot o informa ou quando o dono o digita no vínculo. Os dados são sempre lidos por empresa; nenhuma consulta cruza empresas.
2. **Reserva local, lida da agenda.** Nova entidade `Reserva` (`reservas`): `empresa`, `contato` (opcional), `googleEventId` (opcional, único por empresa), `origem` (`AGENDA` ou `BOT`), `data`, `horario` (hora do evento ou "Diária Completa"), `titulo` (criptografado), `situacao`, `lembreteEnviarEm`, `lembreteEnviadoEm`, `tentativasEnvio` e `atualizadaEm`.
   - O Google Agenda continua sendo a fonte de verdade da ocupação. A tabela é uma cópia para o painel e para o lembrete.
   - A reserva do bot (empresa com reserva automática e agenda) é gravada pelo `ObservadorGoogleAgenda`, que já existe e já produz o resultado real (AGD-02): `registarNoGoogle` consome `EventoReservaConfirmada` (`idPedido`, `dataReserva`, `horario`, `nomeCliente`, `numeroWhatsapp`, `empresa`), faz até 3 tentativas, conta o 409 como sucesso e chama `GerenciadorSessao.concluirRegistro` no `finally`. Esta change só acrescenta, nesse ponto, a gravação da `Reserva`, sem mudar o resultado, as tentativas nem a mensagem ao cliente.
   - `googleEventId`: `ServicoReserva.agendarNoGoogle(empresa, dataReserva, nomeCliente, whatsappId, idPedido)` já devolve o id do evento criado (evento de dia inteiro, id derivado do `idPedido` por `idEventoDoPedido`, SHA-256 em base32hex). Hoje o observador descarta esse retorno e o resultado interno (`Resultado`: `SUCESSO`, `DATA_OCUPADA`, `FALHA`) não carrega id. No 409 e no caso "evento do pedido já existe" (`eventoDoPedidoExiste`, a partir da segunda tentativa) não há retorno, mas o id é o mesmo `idEventoDoPedido(idPedido)`. Por isso o `googleEventId` da `Reserva` do bot é sempre `idEventoDoPedido(idPedido)`; a tarefa é tornar esse cálculo acessível ao observador (hoje é estático e restrito ao pacote de `ServicoReserva`) e fazer o resultado do observador carregá-lo.
   - Sucesso: `AGUARDANDO_LEMBRETE`. Falha final depois das tentativas (inclusive `AgendaNaoConfiguradaException`, 4xx e credencial ausente): `FALHA_AGENDA`, que o painel mostra como "precisa de atenção"; nesse caso ainda não há evento, então `googleEventId` fica nulo. Data ocupada (`DATA_OCUPADA`): o cliente volta a escolher a data (estado `RESERVA_ESPERANDO_DATA`) e nenhuma `Reserva` é gravada.
   - A gravação é idempotente por (`empresa`, `googleEventId`) e fica em bloco protegido: se gravar a `Reserva` falhar, o erro é logado sem dado pessoal (empresa e `idPedido`), e `concluirRegistro` e a mensagem ao cliente seguem como hoje. O contato vem do número original do evento, normalizado e casado por `numeroIndice`; o nome informado na reserva preenche `Contato.nome`.
   - Empresa com reserva manual (automática desligada, locação por hora ou sem `googleCalendarId`, WPP-07) não passa por esse caminho: a estratégia de locação não publica o evento. O evento criado traz "Reserva - <nome>" no título e "WhatsApp: <número>" na descrição; a sincronização acha o mesmo `googleEventId` e atualiza em vez de duplicar.
3. **Sincronização com a agenda.** `ServicoSincronizacaoAgenda` lê os eventos dos próximos 30 dias e faz upsert por `googleEventId`. A reserva do bot já tem esse id (`idEventoDoPedido`) e não duplica, mesmo que a sincronização rode antes de o observador gravar: o upsert é pelo mesmo (`empresa`, `googleEventId`).
   - Método novo em `ServicoReserva`, `listarEventosProximos(empresa, dias)`, devolve um record por evento (id, título, descrição, data, hora). `listarReservas` não serve: ele já reduz tudo a "Alugado/Disponível" por dia.
   - Roda de hora em hora para empresas com `googleCalendarId` e modo diferente de `DESLIGADO`, e quando o dono abre a tela (no máximo 1 vez a cada 5 minutos por empresa).
   - Evento que sumiu da agenda ou foi cancelado, com a reserva ainda futura: situação `CANCELADA_NA_AGENDA`. Evento com data ou hora alterada: a reserva é atualizada e `lembreteEnviarEm` é recalculado.
   - Empresa sem `googleCalendarId`: só existem as reservas do bot.
   - Falha do Google Agenda: mantém o que já está na tabela e a tela mostra "Agenda indisponível no momento".
4. **Vínculo com o contato.** Ao importar um evento, o sistema extrai um telefone brasileiro do título ou da descrição e casa com `Contato.numeroIndice`. Achou: liga. Não existe contato: cria com `origem=AGENDA` e `whatsappId` no formato `55DDDNÚMERO@c.us`. Não achou telefone: situação `SEM_CONTATO`. O dono liga pela tela (`PUT .../reservas/{id}/contato` com `contatoId` ou `telefone`), e a situação passa a `AGUARDANDO_LEMBRETE`. Contato `@lid` não tem telefone real: só entra pela conversa, e o dono liga escolhendo-o na lista.
5. **Configuração na empresa.** O lembrete é independente da reserva automática: o bloco de lembrete não lê nem escreve `permiteReservaAutomatica`, e o PUT do cliente que o traz segue a regra de EMP-02 (valor igual ao atual é ignorado; valor diferente responde 400 "A reserva automática é liberada pelo administrador." e nada é gravado, nem o lembrete). O texto padrão do lembrete não promete reserva automática. Campos novos em `Empresa`: `lembreteModo` (`DESLIGADO`, `SUGERIDO` ou `AUTOMATICO`; nulo vale `DESLIGADO`), `lembreteDiasAntes` (1 a 3; padrão 1), `lembreteHorarioEnvio` (`HH:MM`, validado; padrão `10:00`) e `lembreteTexto` (TEXT, até 1000). No contrato, um DTO nomeado `LembreteConfirmacaoDTO` dentro de `EmpresaUpdateDTO` e `EmpresaResponseDTO`. Os limites valem no back. Sem a chave de criptografia (decisão 12), o modo não pode sair de `DESLIGADO` e o PUT responde 409.
6. **Momento do lembrete.** Ao criar ou atualizar a reserva, `lembreteEnviarEm` = data da reserva menos `lembreteDiasAntes`, no horário configurado, em `America/Sao_Paulo`. Se esse momento já passou na criação, fica nulo e não sai lembrete automático. Mudar a configuração depois só vale para reservas novas ou alteradas. O envio manual continua possível.
7. **Rotina.** `@EnableScheduling` e `ServicoLembreteReserva` com `@Scheduled(cron = "0 */15 * * * *", zone = "America/Sao_Paulo")`. A cada execução:
   - `AUTOMATICO`: reservas `AGUARDANDO_LEMBRETE` com `lembreteEnviarEm <= agora`, contato sem `naoContatar` e horário até 20:00 do dia do envio são enviadas;
   - `SUGERIDO`: as mesmas reservas viram `PRONTA_PARA_AVISAR`, sem enviar;
   - `FALHA_ENVIO` com menos de 3 tentativas é reenviada nos ciclos seguintes, dentro da mesma janela;
   - `LEMBRETE_ENVIADO` há mais de 12 horas sem resposta vira `SEM_RESPOSTA`, que o painel mostra como risco alto de perda. Se o cliente responder depois, a situação muda normalmente;
   - reserva com data passada vira `ENCERRADA`.
8. **Envio com resultado.** `ServicoMensagem` ganha `tentarEnviarMensagemWPP(empresa, whatsappId, texto)`, que devolve `true` ou `false` e reaproveita `preencherDestino` (inclusive `isLid`). `enviarMensagemWPP` não muda. Fluxo do lembrete: grava `ENVIANDO` e `tentativasEnvio + 1`, envia, e então grava `LEMBRETE_ENVIADO` com `lembreteEnviadoEm` ou `FALHA_ENVIO`. `ENVIANDO` com mais de 30 minutos é tratado como `FALHA_ENVIO`. Isso evita duplicar após reinício. Limites contra bloqueio do número: intervalo mínimo de 5 segundos entre envios da mesma empresa e teto de 50 mensagens ativas por empresa por dia.
9. **Resposta em linguagem natural.** Interface `InterpretadorRespostaLembrete` com `interpretar(texto)`, que devolve `CONFIRMA`, `REMARCA`, `PARAR` ou `OUTRO`. A implementação inicial, `InterpretadorPorPalavras`, normaliza o texto (minúsculas, sem acento e sem pontuação) e procura expressões:
   - `CONFIRMA`: "sim", "confirmo", "confirmado", "pode ser", "combinado", "ok", "tudo certo", "estarei lá" e variações, além de 👍;
   - `REMARCA`: "não vou poder", "cancelar", "desmarcar", "remarcar", "mudar a data", "outra data" e variações;
   - `PARAR`: "parar", "não quero receber" e variações;
   - texto com sinais de mais de uma intenção (por exemplo "sim, mas quero mudar a data") ou sem nenhum sinal: `OUTRO`.
   - Mais tarde, uma implementação com `ServicoIA` entra no lugar sem mudar quem chama.
   - O `ServicoRespostaLembrete`, chamado no `ControllerWhatsapp` antes da estratégia, só age quando o estado da conversa é `INICIO` e existe reserva `LEMBRETE_ENVIADO` ou `SEM_RESPOSTA` daquele contato, com data de hoje em diante, cujo lembrete foi enviado há no máximo 72 horas (a mais próxima, se houver várias). Então:
     - `CONFIRMA` → `CONFIRMADA_PELO_CLIENTE` e agradecimento;
     - `REMARCA` → `CANCELAMENTO_SOLICITADO` e mensagem dizendo que o responsável vai entrar em contato;
     - `PARAR` → `naoContatar=true` no contato e confirmação;
     - `OUTRO`, ou conversa em outro estado: segue o fluxo normal, sem mudar a reserva. A opção 1 do menu continua sendo a opção 1.
   - O texto padrão do lembrete convida a responder naturalmente, sem exigir número.
10. **Rotas da tela de Automação.** As rotas aceitam só o cliente da própria empresa. O token precisa pertencer à empresa da `{sessao}`: cliente de outra empresa recebe 403, sem token 401, sessão inexistente 404. O `x-admin-token` e `ROLE_ADMIN` recebem 403: a equipe da plataforma não lê contatos nem reservas dos clientes. Aceitar qualquer token não vazio, como faz `isClienteSemToken` hoje, exporia nome e telefone de outra empresa; as rotas novas não usam esse método. A change `remover-x-cliente-token` o retira do `ControllerEmpresa`, e a verificação de empresa do token vale aqui com ou sem ela.
    - `GET /empresas/{sessao}/reservas?dias=30` (1 a 60): sincroniza se passaram mais de 5 minutos e devolve `ReservaResumoDTO`.
    - `POST /empresas/{sessao}/reservas/{id}/lembrete`: envia agora. 409 se o contato estiver com `naoContatar`, a reserva estiver sem contato ou já for passada; 502 se o WhatsApp falhar.
    - `PUT /empresas/{sessao}/reservas/{id}/contato`: liga um contato.
    - `GET /empresas/{sessao}/contatos`: lista `ContatoResumoDTO`, sempre com telefone mascarado.
    - `POST /empresas/{sessao}/contatos/{id}/numero`: devolve o número completo de um contato. Máximo de 10 por hora por empresa (429 depois disso) e cada acesso grava um registro de auditoria.
11. **Retenção.** Rotina diária apaga `Reserva` encerrada há mais de 12 meses e `Contato` sem conversa e sem reserva há mais de 12 meses, junto com os registros de auditoria de mais de 12 meses. O prazo é uma propriedade (`retencao.meses`).
12. **Proteção dos dados.**
    - **Criptografia em repouso.** `CriptografiaCampo` é um `AttributeConverter` com AES-256-GCM e IV aleatório por valor, aplicado a `Contato.whatsappId`, `Contato.nome` e `Reserva.titulo`. A chave vem de `CONTATOS_CHAVE_CRIPTO` (32 bytes em base64) e o valor guarda o número da versão da chave, para permitir troca futura.
    - **Busca sem abrir o número.** `numeroIndice` é um HMAC-SHA256 do número normalizado com uma chave separada, `CONTATOS_CHAVE_INDICE`. Serve para achar e para garantir unicidade sem guardar o número em claro.
    - **Falha fechada.** Se uma das duas chaves estiver ausente, o sistema não grava contatos nem reservas com contato, não envia lembretes e registra o aviso uma única vez.
    - **Só um por vez.** A API nunca devolve lista com números completos nem exporta contatos. O número completo vem só pelo `POST .../numero`, um contato por chamada, com limite por hora e auditoria (`AuditoriaAcessoContato`: empresa, contato, data e hora, e o e-mail do usuário do token).
    - **Sem exposição automática.** Os repositórios novos usam `@RepositoryRestResource(exported = false)`, como os atuais, porque o projeto tem Spring Data REST. `Contato` e `Reserva` não entram no schema GraphQL.
    - **Logs novos sem dado pessoal.** O código novo não escreve número, nome nem texto de cliente nos logs; usa o id interno e `MascaraLog` (já entregue por `proteger-dados-nos-logs`, WPP-06). O observador já loga só empresa, `idPedido` e classe da exceção; a gravação nova segue o mesmo padrão.
    - A chave precisa ter cópia fora do banco. Perder a chave significa perder o acesso aos contatos.

## Arquivos

- Contrato:
  - `especificacao/back/schemas/empresa/LembreteConfirmacaoDTO.yaml` (novo)
  - `especificacao/back/schemas/empresa/EmpresaUpdateDTO.yaml` e `EmpresaResponseDTO.yaml` (campo `lembreteConfirmacao`)
  - `especificacao/back/schemas/reserva/ReservaResumoDTO.yaml` (novo: id, data, horario, titulo, nomeCliente, situacao, riscoPerda, precisaAtencao, origem, contatoId, telefoneMascarado, podeAvisar), `VincularContatoDTO.yaml` (novo: `contatoId` ou `telefone`) e `schemas/contato/ContatoResumoDTO.yaml` (novo), `NumeroContatoDTO.yaml` (novo: `numero`)
  - `especificacao/back/paths/empresas-reservas.yaml`, `empresas-reservas-lembrete.yaml`, `empresas-reservas-contato.yaml`, `empresas-contatos.yaml` e `empresas-contatos-numero.yaml` (novos) e `$ref` em `swagger.yaml`
- Java:
  - `model/Contato.java`, `model/Reserva.java`, `model/SituacaoReserva.java`, `model/ModoLembrete.java`, `model/AuditoriaAcessoContato.java`, `repository/ContatoRepository.java`, `repository/ReservaRepository.java`, `repository/AuditoriaAcessoContatoRepository.java` (todos `exported = false`)
  - `model/Empresa.java` (campos do lembrete)
  - `util/CriptografiaCampo.java` (conversor e HMAC)
  - `servico/ServicoReserva.java` (`listarEventosProximos`; `agendarNoGoogle` e `idEventoDoPedido` já existem, e o segundo precisa ficar acessível ao observador)
  - `servico/ServicoSincronizacaoAgenda.java`, `servico/ServicoLembreteReserva.java`, `servico/ServicoRespostaLembrete.java`, `servico/InterpretadorRespostaLembrete.java`, `servico/InterpretadorPorPalavras.java`, `servico/ServicoContato.java` (novos)
  - `servico/ServicoMensagem.java` (`tentarEnviarMensagemWPP`)
  - `servico/observador/ObservadorGoogleAgenda.java` (já registra com resultado real e 3 tentativas; passa a carregar o id do evento no resultado e a gravar a `Reserva` do bot em `registarNoGoogle`, sem mudar a mensagem ao cliente)
  - `evento/EventoReservaConfirmada.java` não muda: já traz o necessário
  - `controller/ControllerWhatsapp.java` (registrar contato e chamar `ServicoRespostaLembrete`) e `controller/ControllerEmpresa.java` (rotas do painel e mapeamento dos DTOs)
  - `DemoApplication.java` (`@EnableScheduling`), `application.properties` (`contatos.cripto.chave`, `contatos.indice.chave`, `retencao.meses`), `docker-compose.yml` e `.env.example` (`CONTATOS_CHAVE_CRIPTO` e `CONTATOS_CHAVE_INDICE`)
- Front:
  - Antes do Angular: no canvas de design, board novo `ClienteAutomacao` com as abas Lembrete, Reservas e Contatos, e o board `ClientePainel` com o botão "Automação". O menu lateral (shell) ganha o item "Automação"
  - `especificacao/front/cliente-automacao.content.json` (novo), `cliente-painel.content.json` e `shell.content.json` (botão e item de menu) e `telas.design.json` (cores de confirmada, aguardando, sem resposta com risco alto, remarcar e precisa de atenção)
  - `frontend/src/app/features/client/automacao/` (abas Lembrete, Reservas e Contatos, esta com a ação "ver número", um contato por vez), `features/client/painel/` (botão), `client.routes.ts` (`/cliente/automacao`) e `shell.component` (item de menu). `features/client/perfil/` não muda
- Prova:
  - `src/test/java/com/example/demo/servico/ServicoSincronizacaoAgendaTest.java` (upsert por evento, telefone achado e não achado, reserva do bot não duplica, evento removido, evento remarcado, falha do Google mantém os dados)
  - `src/test/java/com/example/demo/servico/ServicoLembreteReservaTest.java` (modo automático envia uma vez; modo sugerido só marca; reserva criada depois do momento de envio não sai; janela até 20:00; falha reenvia até 3 vezes; `naoContatar`; 12 h sem resposta vira `SEM_RESPOSTA`; data passada encerra; limite diário)
  - `src/test/java/com/example/demo/servico/InterpretadorPorPalavrasTest.java` e `ServicoRespostaLembreteTest.java` (frases naturais de confirmação, remarcação e parada; frase com duas intenções vira `OUTRO`; fora do estado `INICIO` a opção 1 do menu segue valendo; lembrete enviado há mais de 72 horas não responde)
  - `src/test/java/com/example/demo/util/CriptografiaCampoTest.java` e um teste de persistência que lê a coluna crua e confirma que número e nome não estão em claro
  - `src/test/java/com/example/demo/servico/observador/ObservadorGoogleAgendaTest.java` e `src/test/resources/features/observer-agenda.feature` (estendidos: sucesso grava `Reserva` com `AGUARDANDO_LEMBRETE`; 409 e evento achado na segunda tentativa gravam com o mesmo id; falha final grava `FALHA_AGENDA`; data ocupada não grava; falha ao gravar não impede a mensagem nem `concluirRegistro`)
  - `src/test/resources/features/empresas.feature` (PUT com lembrete e limites; PUT do cliente com lembrete e `permiteReservaAutomatica` diferente responde 400 sem gravar o lembrete; GET reservas 200, 401, 403 e 404; admin recebe 403; POST lembrete 200, 409 e 502; vínculo de contato; GET contatos mascarado; POST número 200 e 429; `/contatos` e `/reservas` da API REST automática respondem 404)
  - Testes Angular: tabela do dia, destaque de risco alto, ação "ligar contato" e ação "ver número"

## Alinhamento

- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`
- Texto do lembrete com o mesmo limite de 1000 caracteres dos outros textos do cliente
- Regra de EMP-02 preservada: `permiteReservaAutomatica` só o admin altera, e o lembrete não depende dela
- Telefone sempre mascarado nas respostas da API, salvo `POST .../numero`

## Riscos

- **Mensagem ativa pode bloquear o número.** Por isso o padrão é `DESLIGADO`, o modo `SUGERIDO` exige o clique do dono, o envio automático tem intervalo e teto diário, e o pedido de parar é respeitado. Mesmo assim o risco existe no modo `AUTOMATICO`.
- **Criptografia e restrição de acesso reduzem o risco, mas não substituem a LGPD.** O sistema guarda o mínimo, cifra, mascara, limita o acesso e apaga em 12 meses. Valide com quem entende de proteção de dados antes de oferecer aos clientes, principalmente se um dia os contatos forem usados em campanhas, o que esta change não faz.
- **Perda ou vazamento das chaves.** Quem tiver as duas chaves e o banco lê tudo; quem perder a chave perde os contatos. A chave precisa de cópia segura fora do banco e do repositório.
- **Logs.** `proteger-dados-nos-logs` já está no ar (WPP-06): o webhook e os observadores não escrevem mais o JID, o nome nem o texto. Qualquer log novo desta change precisa seguir o mesmo padrão; é o ponto a vigiar na revisão.
- **Interpretação por palavras é limitada.** Frases ambíguas viram `OUTRO` e não mudam a reserva. O dono ainda vê "sem resposta" e pode ligar. A IA entra depois pela interface, sem mudar o resto.
- **O telefone no título do evento depende do dono.** Quem não anota o telefone na agenda precisará ligar o contato à mão. A tela deve deixar isso rápido, e a primeira versão não tenta adivinhar por nome.
- **Reserva automática já correta, mas só para diária.** `corrigir-reserva-automatica` está no ar: o evento do bot existe de verdade, com id derivado do pedido. Quem usa reserva manual não é afetado. Nada aqui reabre o que o observador já garante.
- **Reserva por hora fica de fora.** `reserva-por-hora` ainda não foi feita: o bot só registra diária, e `dataEstaLivre` trata qualquer evento como dia ocupado. O lembrete usa só a data; o evento com hora que o dono lança na agenda pode ser lido, mas o lembrete por hora de verdade depende de `reserva-por-hora`. Reserva marcada para o mesmo dia da criação não recebe lembrete automático.
- **Sessão da conversa é por número, não por empresa.** `GerenciadorSessao` guarda estado e rascunho pela chave do número normalizado. O mesmo cliente falando com duas empresas compartilha o estado. Isso cruza com `ServicoRespostaLembrete`, que só age em `INICIO`, e com `concluirRegistro`, que muda o estado pelo número. Os contatos desta change são por empresa; esta change não resolve a sessão, mas não deve presumir isolamento dela.
- **Mapa de bloqueios do observador sem limpeza.** `bloqueios` em `ObservadorGoogleAgenda` ganha uma entrada por empresa e data e nunca é esvaziado. Gravar a `Reserva` no mesmo caminho não piora, mas o ponto continua aberto.
- **Tela do admin sem `content.json`.** A tela do admin segue sem arquivo de conteúdo em `especificacao/front/`. Esta change não mexe nela (o admin não vê contatos nem reservas, decisão 10); se isso mudar, a tela nova nasce com `content.json`.
- **`remover-x-cliente-token`.** Entrando antes ou depois, as rotas novas daqui já exigem o JWT da própria empresa; só a janela de edição em `ControllerEmpresa` e `ControllerWhatsapp` precisa ser separada.
- **Atraso de até 15 minutos** no envio, pela frequência da rotina. A sincronização de hora em hora pode levar até 1 hora para ver um evento novo, salvo quando o dono abre a tela.
- **Contadores em memória** (limite diário, intervalo entre envios, trava de 5 minutos da sincronização, limite do número completo). Premissa de operação: uma instância da API, com o produto configurado estabelecimento a estabelecimento. Um reinício zera os contadores. Se um dia houver mais de uma instância, os contadores e a rotina precisam ir para o banco.
- **Tela de Automação é o lugar de outras automações.** A tela nasce com o lembrete, mas o nome e a rota servem a automações futuras (por exemplo o aviso de chuva em `avisar-risco-chuva-reserva`). Cada automação nova entra como aba ou cartão, sem mudar o menu.
- **Mais de uma change no mesmo código.** Logs, reserva automática e previsão já foram aplicadas; esta e `remover-x-cliente-token` ainda mexem em `ControllerEmpresa` e `ControllerWhatsapp`. Aplicar uma de cada vez.
