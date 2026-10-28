# Design

## Decisão

1. **Provedor: OpenWeatherMap, plano gratuito.** Uso comercial permitido com atribuição ("Dados meteorológicos: OpenWeather"). Confirmar os termos e o limite no site do provedor antes de aprovar. O mesmo provedor faz a geocodificação e a previsão:
   - geocodificação: `GET https://api.openweathermap.org/geo/1.0/direct?q=<cidade>,BR&limit=5&appid=<chave da API>`; vale o primeiro resultado com `country=BR`;
   - previsão: `GET https://api.openweathermap.org/data/3.0/onecall?lat=..&lon=..&exclude=current,minutely,hourly,alerts&units=metric&lang=pt_br&appid=<chave da API>` (8 dias, com `pop` = chance de chuva, `rain` em mm, `weather.id` e `timezone_offset`).
   - O One Call 3.0 dá 1.000 chamadas por dia sem custo. Na conta, o limite diário deve ficar em 1.000 para nunca gerar cobrança.
   - A chave da API fica na variável de ambiente `OPENWEATHER_API_KEY`, lida em `application.properties` (`previsao.openweather.chave`, `previsao.openweather.url-base`) e repassada ao container em `docker-compose.yml` e `.env.example`. Nunca vai para o front.
   - "Chave da API" é a credencial do provedor. "Interruptor" é a opção do cliente "Mostrar previsão do tempo" (`previsaoTempoAtiva`).
2. **Troca de provedor sem mexer na tela.** Interface `ProvedorPrevisao` com `geocodificar(cidade)` e `previsaoDiaria(lat, lon)`, devolvendo records internos. A implementação é `ProvedorOpenWeather`. Se um dia o plano gratuito mudar, entra outro provedor (MET Norway, Open-Meteo pago ou próprio) sem tocar no contrato nem no Angular.
3. **Atualização a cada 8 horas, sob demanda, por coordenada.** `ServicoPrevisaoTempo` guarda em cache em memória só a resposta do provedor, com validade de 8 h, na chave latitude e longitude arredondadas a 2 casas. Empresas da mesma cidade compartilham a mesma entrada. O cache nunca guarda a ocupação nem o DTO final. Só chama o provedor quando alguém abre a tela e a entrada venceu. Teto: 3 chamadas de previsão por cidade por dia. Não precisa de `@Scheduled`. Um reinício limpa o cache e custa no máximo 1 chamada a mais por cidade.
   - Trocar a cidade não invalida o cache: a coordenada nova só abre uma entrada nova.
   - Cada empresa pode trocar a cidade no máximo 5 vezes por dia (cada troca gasta 1 geocodificação). A sexta tentativa responde 429 com "Muitas alterações de cidade hoje. Tente amanhã.". O contador fica em memória, por empresa.
4. **Ocupação do dia, calculada a cada requisição.** O serviço cruza a previsão em cache com `ServicoReserva.listarReservas(empresa)`, que devolve os próximos 30 dias como "Alugado"/"Disponível" com data `dd/MM/yyyy`.
   - Empresa sem `googleCalendarId`: `reservado` é `false` em todos os dias e nenhum aviso aparece.
   - `listarReservas` passa a devolver os 30 dias "Disponível" quando a agenda não tem eventos (hoje devolve `null`). Ela só devolve `null` em erro do Google Agenda. Nenhum outro código chama esse método hoje.
   - `null` com agenda configurada = erro do Google Agenda: `reservasDisponiveis=false` no DTO, nenhum cartão sai como "Reservado" e o front mostra "Reservas indisponíveis no momento".
5. **Fuso `America/Sao_Paulo`.** "Hoje" e a data dos cartões usam esse fuso, como em WPP-05. A data de cada dia da previsão vem de `dt + timezone_offset` do provedor. As datas `dd/MM/yyyy` de `listarReservas` são convertidas para `LocalDate` e comparadas por data, nunca por texto.
6. **Falha não quebra a tela.** Erro, tempo esgotado (3 s) ou chave da API ausente: a API responde 200 com `disponivel=false` e lista vazia; o front mostra "Previsão indisponível no momento". A última entrada de cache, mesmo vencida, é usada se existir; nesse caso `atualizadoEm` mostra o horário real daquela entrada.
7. **Localização na empresa.** Campos novos em `Empresa`: `cidadePrevisao`, `cidadePrevisaoResolvida`, `latitude`, `longitude` e `previsaoTempoAtiva`. `previsaoTempoAtiva` nulo (linhas antigas, `ddl-auto=update`) vale `false`: o código lê `Boolean.TRUE.equals(...)`.
   - A localização é o centro da cidade. Bairro não é geocodificado.
   - `cidadePrevisaoResolvida` guarda o nome, o estado e o país devolvidos (por exemplo "Atibaia, São Paulo, BR"), para o cliente conferir cidades homônimas.
   - No PUT do cliente, se `cidadePrevisao` mudar, o back geocodifica e grava.
   - Cidade não encontrada: 400 com "Cidade não encontrada. Confira o nome da cidade.".
   - Provedor fora do ar, tempo esgotado ou chave da API ausente na geocodificação: 503 com "Não foi possível verificar a cidade agora. Tente novamente em alguns minutos.". Nada do PUT é gravado, e o front mantém o que o cliente digitou.
8. **Regra do risco.** Classe pura `CalculadoraRiscoDesmarcacao` com os limites da proposta, sobre valores arredondados: `chanceChuva = round(pop × 100)` como inteiro e `chuvaMm` com 1 casa decimal. Temporal = `weather.id` entre 200 e 299. Dia sem `rain` conta como 0 mm. Neve e outros códigos só entram pela chance e pelos milímetros.
9. **Rota nova.** GET `/empresas/{sessao}/previsao` com `x-admin-token` (ou `ROLE_ADMIN`) ou com o cliente da própria empresa.
   - O `x-cliente-token` precisa pertencer à empresa da `{sessao}`. Cliente de outra empresa recebe 403. Aceitar qualquer token não vazio, como faz `isClienteSemToken` hoje, deixaria um cliente gastar a cota de outra empresa.
   - Sem token: 401. Sessão inexistente: 403 para quem não é admin, igual ao de empresa alheia, e 404 só para o admin. A mesma regra vale no PUT do cliente e na busca por sessão.
   - Devolve `PrevisaoTempoResponseDTO`: `disponivel`, `reservasDisponiveis`, `atualizadoEm`, `cidade` e `dias` (lista de `PrevisaoDiaDTO`). Com o interruptor desligado ou sem cidade, a resposta é 200 com `disponivel=false`.

10. **Painel inicial e ordem de entrega.** A tela do cliente vira dois destinos: `/cliente` (painel inicial: conexão do WhatsApp, previsão e botão "Configuração do chatbot") e `/cliente/chatbot` (o `PerfilComponent` atual sem o bloco de conexão e sem a previsão).
    - Cidade e interruptor da previsão ficam no próprio cartão da previsão do painel, em modo de edição. Sem cidade, o cartão mostra "Informe a cidade para ver a previsão" e o campo.
    - A conexão do WhatsApp reaproveita `ConexaoWhatsappComponent` sem mudar a regra.
    - O front vem primeiro, na ordem da skill `diasabatico`: canvas, depois HTML e navegação com dados de exemplo, sem API. O back (YAML, Java, infra) vem depois, e a última tarefa liga o front à API.

## Arquivos

- Contrato:
  - `especificacao/back/schemas/empresa/EmpresaUpdateDTO.yaml` (`cidadePrevisao`, `previsaoTempoAtiva`)
  - `especificacao/back/schemas/empresa/EmpresaResponseDTO.yaml` (os dois campos e `cidadePrevisaoResolvida`)
  - `especificacao/back/schemas/previsao/PrevisaoTempoResponseDTO.yaml` e `PrevisaoDiaDTO.yaml` (novos; dia: data, diaSemana, reservado, resumo, codigoTempo, tempMax, tempMin, chanceChuva, chuvaMm, risco `BAIXO|MEDIO|ALTO`, estimativa)
  - `especificacao/back/paths/empresas-previsao.yaml` (novo; 200, 401, 403 e 404) e `$ref` em `swagger.yaml`
  - `especificacao/back/paths/empresas-cliente.yaml` (400 de cidade não encontrada, 429 de limite de trocas e 503 de provedor indisponível)
- Java:
  - `model/Empresa.java` (localização e interruptor)
  - `servico/previsao/ProvedorPrevisao.java`, `ProvedorOpenWeather.java`, `ServicoPrevisaoTempo.java`, `CalculadoraRiscoDesmarcacao.java`
  - `servico/ServicoReserva.java` (`listarReservas` devolve os 30 dias "Disponível" quando a agenda não tem eventos)
  - `controller/ControllerEmpresa.java` (GET previsão com checagem de empresa; geocodificar no PUT do cliente)
  - `src/main/resources/application.properties` (chave da API e URL por variável de ambiente)
- Infra:
  - `docker-compose.yml` (`OPENWEATHER_API_KEY` no `environment` da API) e `.env.example`
- Front:
  - Antes do Angular: canvas de design, com o board `ClientePerfil` dividido em `ClientePainel` (painel inicial com os três blocos) e `ClienteChatbot` (a configuração atual)
  - `especificacao/front/cliente-painel.content.json` (novo), `cliente-perfil.content.json` (vira a configuração do chatbot) e `telas.design.json` (cores do selo baixo/médio/alto nos tokens)
  - `frontend/src/app/features/client/painel/` (novo: conexão do WhatsApp, cartões da previsão, botão), `features/client/perfil/` (vira a configuração do chatbot, rota `/cliente/chatbot`), `client.routes.ts`, `shell.component` (menu do cliente) e um serviço Angular para GET previsão
  - Atribuição "Dados meteorológicos: OpenWeather" no rodapé do bloco
- Prova:
  - `src/test/java/com/example/demo/servico/previsao/CalculadoraRiscoDesmarcacaoTest.java` (limites de cada faixa, 69 e 70%, 9,9 e 10 mm, 1,9 e 2 mm, temporal)
  - `src/test/java/com/example/demo/servico/previsao/ServicoPrevisaoTempoTest.java` com `ProvedorPrevisao` simulado (cache de 8 h por coordenada compartilhado entre empresas, cache sem ocupação, falha devolve indisponível, cruzamento com dias reservados, fuso `America/Sao_Paulo` perto da meia-noite, `listarReservas` nulo com agenda configurada, empresa sem agenda)
  - `src/test/resources/features/empresas.feature` (PUT com cidade válida, inválida, provedor fora do ar e sexta troca no dia; GET previsão 200, 401, 403 e 404)
  - Teste do componente do painel no Angular: a atribuição aparece quando há cartões, o botão leva a `/cliente/chatbot` e a conexão do WhatsApp aparece
  - `perfil.component.spec.ts` ajustado para a tela sem conexão e sem previsão

## Alinhamento

- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`
- Chave da API só no back, por variável de ambiente

## Riscos

- **Plano gratuito pode mudar.** OpenWeatherMap já fechou a API 2.5 antes. A interface `ProvedorPrevisao` limita o estrago a uma classe.
- **One Call 3.0 pede cartão no cadastro**, mesmo no uso gratuito. Sem limitar a 1.000 chamadas por dia na conta, há risco de cobrança.
- **Licença ODbL:** precisa da atribuição visível na tela.
- **`lembrar-confirmacao-reserva` também mexe no board `ClientePerfil`** (bloco do lembrete, reservas e contatos). Com o painel inicial, esses itens precisam decidir se ficam no painel ou na configuração; revisar essa change antes de ela ir para a programação.
- Previsão além de 5 dias é pouco confiável. Os cartões do 6º ao 8º dia mostram "estimativa".
- Uma coordenada por cidade pode não refletir o bairro do espaço em cidade grande.
- Os limites do risco são um palpite inicial; sem histórico de cancelamentos, não há como calibrar.
- O contador de trocas de cidade e o cache ficam em memória: um reinício zera os dois. Premissa de operação: uma instância da API. Se um dia houver mais de uma, os dois precisam ir para o banco.
