# Design

## Decisão

### 1. Tamanho da entrada do login
- `LoginRequestDTO`: `@Size(max = 254)` no e-mail e `@Size(max = 128)` na senha, com `maxLength` igual no YAML.
- `auth-login.yaml` ganha a resposta 400, que hoje não está documentada.
- Senha nova ganha `@Size(max = 128)` e `maxLength: 128` em `AlterarSenhaDTO.novaSenha`, `ResetSenhaAdminDTO.novaSenha` e `EmpresaCreateDTO.senha`. Sem isso, uma senha aceita na troca poderia ser recusada no login.
- `AlterarSenhaDTO.senhaAtual` também fica limitada a 128.

### 2. Teto rígido no limite de login
- O `ConcurrentHashMap` dá lugar a um `LinkedHashMap` em ordem de inserção, com acesso `synchronized`. Cada operação é curta e o login já paga um BCrypt, então a disputa pelo bloqueio é irrelevante nesta escala.
- `registrarFalha` remove e reinsere a chave, deixando no início do mapa a entrada com a falha mais antiga.
- Ao passar de `MAXIMO_ENTRADAS = 10_000`, sai a entrada mais antiga. Entradas vencidas saem do início do mapa no mesmo passo, sem varrer tudo.
- Cada fila guarda no máximo `MAXIMO_FALHAS` registros. Com o item 1 limitando a chave, o consumo fica em torno de 10 MB no pior caso.

### 3. Teto por IP (consequência do item 4)
- Rodar BCrypt para e-mail inexistente permite gastar CPU sem limite, trocando o e-mail a cada tentativa, porque a contagem atual é por e-mail e IP. Por isso entra um segundo contador, por IP, de 30 falhas em 15 minutos, conferido antes do BCrypt, com o mesmo 429 e `Retry-After`.
- O contador por IP usa o mesmo mapa com teto, com chave prefixada, e só vale com `PROXIES_CONFIAVEIS` preenchida. Com a lista vazia, todos dividiriam o IP do proxy e 30 falhas de qualquer pessoa travariam o login de todos.
- Login certo zera a contagem por e-mail e IP, mas não a contagem por IP.

### 4. Tempo igual para e-mail inexistente
- `ServicoAuth` calcula no construtor um hash de referência com `passwordEncoder.encode` sobre um valor aleatório, usado só nesta instância. O hash tem o mesmo custo configurado no encoder, não fica no código e não precisa de configuração.
- E-mail não encontrado roda `passwordEncoder.matches(senha, hashDeReferencia)` e responde o mesmo 401 "Senha incorreta".

### 5. IP literal sem DNS
- Classe nova `util/LiteralDeIp`, sem dependência nova, com `bytes(String)` que devolve 4 ou 16 bytes, ou `null`.
- IPv4: exatamente 4 octetos decimais de 0 a 255, sem zero à esquerda. `1.2.3` passa a ser recusado.
- IPv6: 8 grupos hexadecimais com no máximo um `::` e IPv4 embutido no final. Sem identificador de zona (`%`).
- `::ffff:a.b.c.d` devolve os 4 bytes do IPv4, como `InetAddress.getByName` faz hoje, para continuar casando com as faixas IPv4.
- `IpDoCliente` passa a usar a classe nova e registra aviso no boot quando a lista de proxies está vazia, sem expor valor.

### 6. API atrás do proxy
A publicação em `127.0.0.1` só funciona se o proxy alcançar esse endereço. Um proxy em container não alcança o `127.0.0.1` do host, então mudar sem conferir pode tirar a API do ar. O `docker-proxy-1` visto no servidor é de outra stack e não confirma como o tráfego chega à API.

A primeira tarefa é conferir, no servidor e sem alterar nada, qual processo atende as portas 443 e 80 e para onde ele encaminha a API.

| O que a conferência mostrar | O que fazer | `PROXIES_CONFIAVEIS` |
|---|---|---|
| Proxy no próprio host | `127.0.0.1:8080:8080` no `docker-compose.yml` | IP do gateway da `bot-network`, que é o endereço de origem visto pela API |
| Proxy em container | `127.0.0.1:8080:8080` no `docker-compose.yml`; no `docker-compose.override.yml` do servidor, a API entra numa rede externa compartilhada com o proxy, que passa a encaminhar para `bot-java:8080` | IP fixo do proxy nessa rede (`ipv4_address`) |
| Nenhum proxy na frente da API | Parar e voltar à pessoa, porque a API perderia o acesso externo | Vazia |

Em todos os casos o IP é confirmado por `docker network inspect` e por um teste de login errado, conferindo que o limite conta o IP real do cliente.

O `.env.example` passa a mostrar um IP único. No `atualizar-env-producao.sh`, a faixa da rede continua sendo exibida como informação, mas Enter mantém o valor atual em vez de gravar a faixa inteira.

### 7. Bloqueio forçado de login por terceiro
Avaliação das alternativas:

| Alternativa | Resultado |
|---|---|
| Manter a chave por e-mail e IP | Escolhida. O terceiro precisa estar no mesmo IP público e saber o e-mail; o efeito dura até 15 minutos e só vale para aquele IP |
| Deixar a senha certa passar durante o bloqueio | Recusada, porque liberaria tentativas sem limite |
| Bloquear só por e-mail | Recusada, porque qualquer pessoa na internet travaria uma conta |
| Desbloqueio manual pelo painel | Recusada nesta escala: rota nova para um caso raro. Reiniciar a API zera as contagens |

A mitigação real é o IP correto: o item 6 e o aviso de lista vazia do item 5 evitam o pior caso, em que todos dividem o IP do proxy.

### 8. Gateway
- `descreverErroSeguro`: `name` só com `[A-Za-z0-9_.-]`, até 64 caracteres, senão `erro`. `code` com a mesma regra, até 32 caracteres, senão é omitido. `response.status` só se for inteiro de 100 a 599.
- `deviceController.ts`: `logger.error` recebe `descreverErroSeguro(e)`. As respostas tiram `error` e `log` e mantêm `status` e `message`. O backend Java não lê esses dois campos das respostas do gateway.

### 9. Número da Gamb no script
`atualizar-env-producao.sh` confere `^55[0-9]{10,11}$` e pergunta de novo enquanto o texto for inválido. Enter continua deixando em branco. Um valor já gravado no `.env` e inválido entra na lista de pendências, sem exibir o conteúdo.

### 10. Rotação das chaves de contatos
Os dados afetados são `contatos.whatsapp_id`, `contatos.nome`, `contatos.numero_indice` e `reservas.titulo`. A coluna `numero_indice` é recalculada a partir do `whatsapp_id` decifrado, com `WhatsappUtil.normalizarParaChaveEstado`.

- Variáveis de uso único: `CONTATOS_CHAVE_CRIPTO_ANTIGA` e `CONTATOS_CHAVE_INDICE_ANTIGA`, sem valor padrão. Sem elas, nada acontece.
- A aplicação não sobe se só uma estiver definida, se a antiga for inválida ou se a antiga for igual à nova.
- O componente novo `RotacaoChavesContatos` implementa `SmartInitializingSingleton`, que roda depois do Flyway e do JPA e antes de o servidor web e os agendamentos começarem. Nenhuma requisição encontra dado em estado misto.
- A leitura e a gravação usam `JdbcTemplate`, em lotes por `id`, porque o conversor JPA só conhece a chave nova.
- Cada linha é uma transação, com as colunas da linha juntas. Para cada valor:
  1. Valor com `v1:` que abre com a chave nova já foi rotacionado e fica como está.
  2. Valor com `v1:` que abre com a chave antiga é cifrado com a nova, e o índice é recalculado.
  3. Valor sem `v1:`, gravado antes da cifra, é cifrado com a nova, e o índice é recalculado.
  4. Valor que não abre com nenhuma das chaves interrompe o boot. O log traz só a tabela e o `id`.
- O log final traz só as quantidades por tabela. A falha para a subida e a próxima tentativa continua de onde parou.
- `@Value` de `CriptografiaCampo` perde as chaves padrão do código (regra 2.2).

Ordem da operação, feita pela pessoa no servidor:
1. Rodar `pg_dump` e guardar o arquivo fora do servidor, cifrado (regra 10.6).
2. Rodar `scripts/rotacionar-chaves-contatos.sh`, que move as chaves atuais para as variáveis `_ANTIGA` e gera as novas.
3. Rodar `docker compose up -d --force-recreate` e conferir as quantidades no log.
4. Apagar as variáveis `_ANTIGA` do `.env`, as cópias `.env.bak-*` e o dump de antes da rotação, assim que o painel listar os contatos.

## Arquivos
- Contrato: `especificacao/back/schemas/auth/LoginRequestDTO.yaml`, `AlterarSenhaDTO.yaml`, `especificacao/back/schemas/empresa/ResetSenhaAdminDTO.yaml`, `EmpresaCreateDTO.yaml`, `especificacao/back/paths/auth-login.yaml`. Nenhum `$ref` novo.
- Java:
  - `dto/auth/LoginRequestDTO.java`, `dto/auth/AlterarSenhaDTO.java`, `dto/empresa/ResetSenhaAdminDTO.java`, `dto/empresa/EmpresaCreateDTO.java`;
  - `servico/ServicoAuth.java`, `servico/LimiteTentativasLogin.java`, `servico/IpDoCliente.java`, `controller/ControllerAuth.java`;
  - novos `util/LiteralDeIp.java` e `servico/RotacaoChavesContatos.java`;
  - `util/CriptografiaCampo.java`, `application.properties`.
- Front: nenhum.
- Infra e gateway: `docker-compose.yml`, `docker-compose.override.example.yml`, `.env.example`, `scripts/atualizar-env-producao.sh`, novo `scripts/rotacionar-chaves-contatos.sh`, `wppconnect-server/src/util/functions.ts`, `wppconnect-server/src/controller/deviceController.ts`.
- Prova:
  - `ControllerAuthTest` (400 por tamanho, 429 por IP), `ServicoAuthTest` (BCrypt com e-mail inexistente), `auth.feature`;
  - `LimiteTentativasLoginTest` (teto, descarte da mais antiga, limite por IP), `IpDoClienteTest` e `LiteralDeIpTest` (sem DNS, formas recusadas, IPv4 em IPv6);
  - `RotacaoChavesContatosTest` com banco real de teste (linhas rotacionadas, retomada, chave errada, valor sem `v1:`, recusa de configuração, log sem dado);
  - `wppconnect-server/src/tests` (`descreverErroSeguro` e `deviceController` sem o objeto do erro);
  - `docker compose config` e execução do script contra um `.env` temporário.

## Alinhamento
- Stack de `00-architecture.md`, sem dependência nova.
- DTO nomeado; sem entidade JPA e sem `Map` no payload.
- YAML em `especificacao/back/` antes do Java.
- Sem migração Flyway: as colunas não mudam.
- Cumpre as seções 2, 3, 4, 5, 6 e 10 de `.cursor/rules/seguranca.mdc`.
- Prova da seção 12: 400 e 429 cobertos. A rota de login é pública e os testes de 401 e 403 não se aplicam; não há rota nova.

## Riscos
- [Publicar em `127.0.0.1` tirar a API do ar] → a conferência da decisão 6 vem antes da mudança. Reverter é devolver `8080:8080` e recriar o container.
- [Rotação falhar no meio] → o boot para e a próxima subida continua de onde parou. O dump anterior cobre a perda das chaves.
- [Perder a chave antiga antes do fim] → as variáveis `_ANTIGA` só saem depois de conferir as quantidades e o painel. Sem elas os contatos ficam ilegíveis.
- [Backups antigos] → continuam abertos à chave vazada. Exceção à regra 2.5, com prazo: apagar os backups anteriores à rotação ao fim do período de retenção de backups, em até 30 dias.
- [Chave vazada ativa até o deploy] → a regra 2.5 pede troca imediata. Exceção com prazo: esta change é a próxima a ser implementada, e o script de rotação pode ser publicado antes das demais tarefas.
- [Contatos com a mesma chave depois de recalcular] → a constante `uk_contato_empresa_numero_indice` interrompe o boot com tabela e `id`. Só ocorreria se a normalização tivesse mudado depois da gravação.
- [Webhook durante a rotação] → a API fica fora do ar durante o boot, como em qualquer deploy. Com o volume atual, são segundos.
- [Flood desalojar um bloqueio legítimo] → com o teto por IP, tirar 10.000 entradas exige mais de 300 IPs. Aceito nesta escala.
- [Teto por IP em rede compartilhada] → 30 falhas num mesmo IP travam o login desse IP por até 15 minutos. Aceito: o número é alto para uso normal e o bloqueio é só naquele IP.
- [Login sem teto por IP se `PROXIES_CONFIAVEIS` ficar vazia] → aviso no boot.
- [Limites em memória] → reinício zera as contagens; com mais de uma instância, cada uma conta à parte.
- [Bloqueio forçado por terceiro no mesmo IP] → aceito pela decisão 7; dura até 15 minutos e reiniciar a API zera.
- [Resposta do gateway sem o erro] → o diagnóstico passa a depender do log com tipo e código. Quem chama o gateway não lê os campos removidos.
- [Outros controllers do fork com erro bruto] → exceção à regra 6.1, com prazo: a entrega de `migrar-wppconnect-para-baileys`, que remove o fork.
- [Mudança descartada pela migração para Baileys] → os itens de `wppconnect-server` valem só até essa entrega. O esforço é pequeno e o risco existe até lá.
