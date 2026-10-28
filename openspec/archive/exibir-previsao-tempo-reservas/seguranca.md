# Segurança: exibir-previsao-tempo-reservas

Escopo: arquivos da seção Arquivos do `design.md`, mais os YAML e testes da volta 3. Volta 3. Revisão sem patch, conferida no código.

## Achados da volta 2

1. Corrigido. `ControllerEmpresa.java` L202-212: o PUT `/{sessao}/cliente` só segue para admin ou `isClienteDaEmpresa` (L371-380), antes de `geocodificarCidade`. O token legado `x-cliente-token` vira principal `String` (`JwtAuthenticationFilter.java` L92-96) e cai em 403. JWT de outra empresa compara `id` e cai em 403. Sem token, a cadeia exige autenticação (`SecurityConfig.java` L58).
2. Corrigido. `ControllerEmpresa.java` L113-128 e L457: `findBySessaoWhatsapp` só devolve a própria empresa ao cliente, e `googleCalendarId` sai só com `admin=true`. `listar` (L88) exige admin. `cadastrar`, PUT admin e `reset-senha` exigem admin e devolvem `EmpresaResponseDTO`, sem `googleCalendarId`.
3. Corrigido. GET previsão (L286-301), PUT do cliente e `findBySessaoWhatsapp` dão 403 igual para sessão inexistente e para empresa alheia a quem não é admin. O 404 só sai para o admin. Sem token: 401 antes da busca.
4. Corrigido. `geocodificarCidade` (L382-396) estorna a troca só em `ProvedorIndisponivelException` (503). O 400 de cidade não encontrada continua contando, porque gasta geocodificação.

## Caminhos de bypass testados no código

- Maiúscula, minúscula ou espaço na sessão: a posse é conferida por `id` da empresa encontrada, nunca pelo texto da sessão.
- JWT de admin: `Usuario.getAuthorities` dá `ROLE_ADMIN`; acesso total é o esperado.
- Estorno (`ServicoPrevisaoTempo.java` L63-74): é por `empresaId` da empresa já autorizada, só desconta no mesmo dia e nunca abaixo de 0. A sexta tentativa dá 429 sem estorno. Só falha do provedor devolve a troca, então não passa de 5 geocodificações com sucesso por dia nem gasta a cota de outra empresa.
- GraphQL (`/api/ia-query`, `ConsultaEmpresaGrafo.java`): cliente lê só a própria empresa, e `EmpresaConsulta` não tem `googleCalendarId`.
- Spring Data REST: `EmpresaRepository`, `UsuarioRepository` e `AvisoRepository` têm `exported = false`.
- Fora do controller, `googleCalendarId` só é lido em serviço (`ServicoReserva`, `LocacaoStrategy`, `ServicoPrevisaoTempo`) e não vai para DTO.
- `OPENWEATHER_API_KEY` continua só em variável de ambiente e não aparece em DTO, log nem erro.
- YAML (`empresas-search.yaml`, `empresas-cliente.yaml`, `entidades.yaml`) descreve o 403 e `googleCalendarId` nulo para cliente, de acordo com o código.

## Observação fora do escopo

- O token legado `x-cliente-token` aceita qualquer texto como `ROLE_CLIENTE` (`JwtAuthenticationFilter.java` L90-96). Nas rotas desta tarefa ele recebe 403, mas outras rotas que confiam só em `ROLE_CLIENTE` valem uma revisão própria.

Sem vazamento nem acesso indevido nos arquivos da tarefa.

Estado: aprovado
