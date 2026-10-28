# Segurança: corrigir-reserva-automatica

Veredito: **aprovado**. Nenhum achado alto ou médio. Escopo: arquivos da seção Arquivos do design.md.

## Conferido sem achado

- `ControllerEmpresa.java:203-219`: PUT do cliente checa dono (403) antes da regra de `permiteReservaAutomatica` (400). Não dá para usar o 400 para descobrir o estado de outra empresa. O campo não é mais gravado pelo cliente.
- `ControllerEmpresa.java:144` e `:326`: POST e PUT do admin exigem `ROLE_ADMIN` ou `x-admin-token` antes de qualquer leitura.
- `ControllerEmpresa.java:419-450`: `EmpresaResponseDTO` não tem `googleCalendarId`. `paraPainel` (`:478`) só devolve a agenda ao admin.
- `EmpresaUpdateDTO` não tem `googleCalendarId`. YAML do cliente (`empresas-cliente.yaml`, `EmpresaUpdateDTO.yaml`) não expõe a agenda.
- Logs em `ObservadorGoogleAgenda` (`:57`, `:73`, `:94`, `:165`), `ServicoReserva` (`:38`, `:53`, `:149`, `:195`) e `LocacaoStrategy` (`:222`, `:397`, `:420`, `:472`): só id da empresa, `idPedido`, tentativa e `getSimpleName()` da exceção. Sem WhatsApp, nome, agenda ou token.
- `AgendaNaoConfiguradaException`: mensagem só com o id da empresa.
- Mensagens ao cliente final (`ObservadorGoogleAgenda.java:33-36`, `:169-178`): texto fixo, sem detalhe interno.
- `ServicoReserva.java:74-96`: id do evento é SHA-256 do UUID em base32hex. Colisão entre pedidos ou empresas é desprezível.
- `ObservadorGoogleAgenda.java:114-138`: bloqueio por empresa e data cobre reconsulta e inserção. A espera fora do bloqueio está nos Riscos do design.

## Achados baixos (não bloqueiam)

1. **Sessão do bot por número, não por empresa.** `GerenciadorSessao.java:139-141`. Cenário: o mesmo número conversa com as empresas A e B. Um "sim" em A deixa o número em `RESERVA_REGISTRANDO`, e o bot de B responde "Ainda estou registrando..." até A terminar. O `concluirRegistro` de A também muda o estado da conversa com B. Não vaza dado a terceiros: os dados são do próprio número, e o evento copia os valores no "sim". Já existia antes, mas a change aumenta o efeito. Severidade: baixa.
2. **Reservas sem limite por número.** `LocacaoStrategy.java:310-328`. Cenário: um número qualquer repete o fluxo e reserva todas as datas livres, inclusive muito distantes (`31-12-9999` passa em `parseData`), e a agenda do dono fica bloqueada. Cada pedido também cria uma entrada permanente no mapa `bloqueios` (`ObservadorGoogleAgenda.java:40`). Severidade: baixa (escala pequena, a opção só o admin liga). Sugestão para outra change: limite de datas futuras e de pedidos por número.
3. **"Sim" duplicado e simultâneo.** `LocacaoStrategy.java:61` e `:322`. Ler e trocar o estado não é atômico. Dois webhooks ao mesmo tempo publicam dois eventos. O bloqueio impede reserva dupla, mas o cliente pode receber "data acabou de ser reservada" logo depois de "confirmada". Severidade: baixa (só incomoda).
4. **Nome do cliente sem limite de tamanho** no resumo do evento (`ServicoReserva.java:47`). Texto puro na agenda do próprio dono. Um nome enorme só faz o pedido falhar. Severidade: informativa.

Fora do escopo e já existente: `x-admin-token` comparado com `equals`, que não tem tempo constante (`ControllerEmpresa.java:67`).
