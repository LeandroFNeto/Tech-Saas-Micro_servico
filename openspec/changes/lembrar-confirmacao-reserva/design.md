# Design

## Decisão

1. **Contato por empresa.** Nova entidade `Contato` (`contatos`): `empresa`, `whatsappId` (o JID original recebido, usado para enviar), `nome`, `numeroIndice` (HMAC do `numeroChave` de `WhatsappUtil.normalizarParaChaveEstado`, usado para casar respostas e vínculos), `finalTelefone` (4 últimos dígitos, para a máscara), `primeiraConversaEm`, `ultimaConversaEm`, `naoContatar` (false) e `origem` (`CONVERSA` ou `AGENDA`). Único por empresa e `numeroIndice`. `whatsappId` e `nome` são gravados criptografados (decisão 12). O `ControllerWhatsapp` grava ou atualiza o contato depois de achar a empresa e antes de chamar o módulo, para todos os ramos. O nome só é preenchido quando a reserva pelo bot o informa ou quando o dono o digita no vínculo. Os dados são sempre lidos por empresa; nenhuma consulta cruza empresas.
2. **Reserva local, lida da agenda.** Nova entidade `Reserva` (`reservas`): `empresa`, `contato` (opcional), `googleEventId` (opcional, único por empresa), `origem` (`AGENDA` ou `BOT`), `data`, `horario` (hora do evento ou "Diária Completa"), `titulo` (criptografado), `situacao`, `lembreteEnviarEm`, `lembreteEnviadoEm`, `tentativasEnvio` e `atualizadaEm`.
   - O Google Agenda continua sendo a fonte de verdade da ocupação. A tabela é uma cópia para o painel e para o lembrete.
   - A reserva do bot (empresa com reserva automática) é gravada pelo `ObservadorGoogleAgenda`, depois que o Google responde, com o `googleEventId` do evento criado. Sucesso: `AGUARDANDO_LEMBRETE`. Falha depois das tentativas: `FALHA_AGENDA`, que o painel mostra como "precisa de atenção". Empresa com reserva manual não passa por esse caminho.
3. **Sincronização com a agenda.** `ServicoSincronizacaoAgenda` lê os eventos dos próximos 30 dias e faz upsert por `googleEventId`. A reserva do bot já tem esse id e não duplica.
   - Método novo em `ServicoReserva`, `listarEventosProximos(empresa, dias)`, devolve um record por evento (id, título, descrição, data, hora). `listarReservas` não serve: ele já reduz tudo a "Alugado/Disponível" por dia.
   - Roda de hora em hora para empresas com `googleCalendarId` e modo diferente de `DESLIGADO`, e quando o dono abre a tela (no máximo 1 vez a cada 5 minutos por empresa).
   - Evento que sumiu da agenda ou foi cancelado, com a reserva ainda futura: situação `CANCELADA_NA_AGENDA`. Evento com data ou hora alterada: a reserva é atualizada e `lembreteEnviarEm` é recalculado.
   - Empresa sem `googleCalendarId`: só existem as reservas do bot.
   - Falha do Google Agenda: mantém o que já está na tabela e a tela mostra "Agenda indisponível no momento".
4. **Vínculo com o contato.** Ao importar um evento, o sistema extrai um telefone brasileiro do título ou da descrição e casa com `Contato.numeroIndice`. Achou: liga. Não existe contato: cria com `origem=AGENDA` e `whatsappId` no formato `55DDDNÚMERO@c.us`. Não achou telefone: situação `SEM_CONTATO`. O dono liga pela tela (`PUT .../reservas/{id}/contato` com `contatoId` ou `telefone`), e a situação passa a `AGUARDANDO_LEMBRETE`. Contato `@lid` não tem telefone real: só entra pela conversa, e o dono liga escolhendo-o na lista.
5. **Configuração na empresa.** Campos novos em `Empresa`: `lembreteModo` (`DESLIGADO`, `SUGERIDO` ou `AUTOMATICO`; nulo vale `DESLIGADO`), `lembreteDiasAntes` (1 a 3; padrão 1), `lembreteHorarioEnvio` (`HH:MM`, validado; padrão `10:00`) e `lembreteTexto` (TEXT, até 1000). No contrato, um DTO nomeado `LembreteConfirmacaoDTO` dentro de `EmpresaUpdateDTO` e `EmpresaResponseDTO`. Os limites valem no back. Sem a chave de criptografia (decisão 12), o modo não pode sair de `DESLIGADO` e o PUT responde 409.
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
10. **Painel.** As rotas aceitam só o cliente da própria empresa. O token precisa pertencer à empresa da `{sessao}`: cliente de outra empresa recebe 403, sem token 401, sessão inexistente 404. O `x-admin-token` e `ROLE_ADMIN` recebem 403: a equipe da plataforma não lê contatos nem reservas dos clientes. Aceitar qualquer token não vazio, como faz `isClienteSemToken` hoje, exporia nome e telefone de outra empresa.
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
    - **Logs novos sem dado pessoal.** O código novo não escreve número, nome nem texto de cliente nos logs; usa o id interno e `MascaraLog` (change `proteger-dados-nos-logs`).
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
  - `servico/ServicoReserva.java` (`listarEventosProximos`)
  - `servico/ServicoSincronizacaoAgenda.java`, `servico/ServicoLembreteReserva.java`, `servico/ServicoRespostaLembrete.java`, `servico/InterpretadorRespostaLembrete.java`, `servico/InterpretadorPorPalavras.java`, `servico/ServicoContato.java` (novos)
  - `servico/ServicoMensagem.java` (`tentarEnviarMensagemWPP`)
  - `servico/observador/ObservadorGoogleAgenda.java` (grava a `Reserva` do bot depois do resultado do Google)
  - `controller/ControllerWhatsapp.java` (registrar contato e chamar `ServicoRespostaLembrete`) e `controller/ControllerEmpresa.java` (rotas do painel e mapeamento dos DTOs)
  - `DemoApplication.java` (`@EnableScheduling`), `application.properties` (`contatos.cripto.chave`, `contatos.indice.chave`, `retencao.meses`), `docker-compose.yml` e `.env.example` (`CONTATOS_CHAVE_CRIPTO` e `CONTATOS_CHAVE_INDICE`)
- Front:
  - Antes do Angular: artboard `ClientePerfil` no canvas de design (seção "Lembrete de confirmação") e boards novos `ClienteReservas` (tabela do dia) e `ClienteContatos`
  - `especificacao/front/cliente-perfil.content.json`, `cliente-reservas.content.json` e `cliente-contatos.content.json` (novos) e `telas.design.json` (cores de confirmada, aguardando, sem resposta com risco alto, remarcar e precisa de atenção)
  - `frontend/src/app/features/client/perfil/` (bloco do lembrete), `features/client/reservas/`, `features/client/contatos/` (com a ação "ver número", um contato por vez), rotas em `client.routes.ts` e itens no shell
- Prova:
  - `src/test/java/com/example/demo/servico/ServicoSincronizacaoAgendaTest.java` (upsert por evento, telefone achado e não achado, reserva do bot não duplica, evento removido, evento remarcado, falha do Google mantém os dados)
  - `src/test/java/com/example/demo/servico/ServicoLembreteReservaTest.java` (modo automático envia uma vez; modo sugerido só marca; reserva criada depois do momento de envio não sai; janela até 20:00; falha reenvia até 3 vezes; `naoContatar`; 12 h sem resposta vira `SEM_RESPOSTA`; data passada encerra; limite diário)
  - `src/test/java/com/example/demo/servico/InterpretadorPorPalavrasTest.java` e `ServicoRespostaLembreteTest.java` (frases naturais de confirmação, remarcação e parada; frase com duas intenções vira `OUTRO`; fora do estado `INICIO` a opção 1 do menu segue valendo; lembrete enviado há mais de 72 horas não responde)
  - `src/test/java/com/example/demo/util/CriptografiaCampoTest.java` e um teste de persistência que lê a coluna crua e confirma que número e nome não estão em claro
  - `src/test/resources/features/empresas.feature` (PUT com lembrete e limites; GET reservas 200, 401, 403 e 404; admin recebe 403; POST lembrete 200, 409 e 502; vínculo de contato; GET contatos mascarado; POST número 200 e 429; `/contatos` e `/reservas` da API REST automática respondem 404)
  - Testes Angular: tabela do dia, destaque de risco alto, ação "ligar contato" e ação "ver número"

## Alinhamento

- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`
- Texto do lembrete com o mesmo limite de 1000 caracteres dos outros textos do cliente
- Telefone sempre mascarado nas respostas da API, salvo `POST .../numero`

## Riscos

- **Mensagem ativa pode bloquear o número.** Por isso o padrão é `DESLIGADO`, o modo `SUGERIDO` exige o clique do dono, o envio automático tem intervalo e teto diário, e o pedido de parar é respeitado. Mesmo assim o risco existe no modo `AUTOMATICO`.
- **Criptografia e restrição de acesso reduzem o risco, mas não substituem a LGPD.** O sistema guarda o mínimo, cifra, mascara, limita o acesso e apaga em 12 meses. Valide com quem entende de proteção de dados antes de oferecer aos clientes, principalmente se um dia os contatos forem usados em campanhas, o que esta change não faz.
- **Perda ou vazamento das chaves.** Quem tiver as duas chaves e o banco lê tudo; quem perder a chave perde os contatos. A chave precisa de cópia segura fora do banco e do repositório.
- **Logs que o sistema já imprime.** O webhook e o observador do WhatsApp escrevem o JID e o texto das mensagens no console. A change `proteger-dados-nos-logs` corrige isso e precisa entrar antes desta.
- **Interpretação por palavras é limitada.** Frases ambíguas viram `OUTRO` e não mudam a reserva. O dono ainda vê "sem resposta" e pode ligar. A IA entra depois pela interface, sem mudar o resto.
- **O telefone no título do evento depende do dono.** Quem não anota o telefone na agenda precisará ligar o contato à mão. A tela deve deixar isso rápido, e a primeira versão não tenta adivinhar por nome.
- **Depende da reserva automática correta.** A reserva do bot só chega à tabela e à agenda depois de `corrigir-reserva-automatica`. Quem usa reserva manual não é afetado.
- **Reserva por hora e diária.** O lembrete usa só a data da reserva. Reserva por hora marcada para o mesmo dia da criação não recebe lembrete automático.
- **Atraso de até 15 minutos** no envio, pela frequência da rotina. A sincronização de hora em hora pode levar até 1 hora para ver um evento novo, salvo quando o dono abre a tela.
- **Contadores em memória** (limite diário, intervalo entre envios, trava de 5 minutos da sincronização, limite do número completo). Premissa de operação: uma instância da API, com o produto configurado estabelecimento a estabelecimento. Um reinício zera os contadores. Se um dia houver mais de uma instância, os contadores e a rotina precisam ir para o banco.
- **Quatro changes no mesmo código.** Esta, a de previsão, a de reserva automática e a de logs mexem em arquivos em comum. Aplicar uma de cada vez: logs, reserva automática, previsão, lembrete.
