# Qualidade: corrigir-reserva-automatica (volta 1)

Agente: claude-sonnet-5-5. Veredito: **aprovado** (0 alto, 0 médio, 4 baixos). Regressão informada: mvn 319, npm 48/48. Sem comentário em código novo (conferido com `git diff`).

## Os 7 problemas da volta anterior
| # | Problema anterior | Situação |
|---|---|---|
| 1 | Sem teste de `agendarNoGoogle`/`eventoDoPedidoExiste` | Resolvido. `ServicoReservaAgendarTest` cobre título, descrição, dia inteiro (end = data + 1, inclusive virada de mês), id `[0-9a-v]{5,1024}` derivado do pedido, agenda nula/vazia/em branco sem chamar o Google, existente, `cancelled`, 404, erro 500 repassado. |
| 2 | PUT admin validava sempre | Resolvido. `ControllerEmpresa.java:335-341` valida só em desligada para ligada, usando a agenda final (campo novo ou a atual). Testes: true em empresa ligada sem agenda, apagar agenda mandando true, desligada para ligada sem agenda final, desligar sem agenda. Alinha com design 9 e 10. |
| 3 | POST sem o campo | Resolvido. `deveNascerDesligadaQuandoPostNaoEnviaOCampo` e cenário "sem agenda e sem ligar" em `empresas.feature` conferem `false`. |
| 4 | `@ApiResponses`/`@Schema`/YAML | Quase: `@ApiResponses` (POST, PUT cliente, PUT admin), `EmpresaUpdateDTO`/`EmpresaUpdateAdminDTO` e os YAMLs alinhados. Sobrou o `@Schema` de `EmpresaCreateDTO.java` (baixo 1). |
| 5 | `_somenteLeitura` | Resolvido. Sem ocorrência em `frontend/src`; `perfil.component.ts:136` desestrutura e não envia o campo. |
| 6 | `lacunas-front.md` | Quase: seções 2 e 3 atualizadas (48 testes, regra da passagem desligada para ligada); restam itens velhos (baixo 2). |
| 7 | Mapa de bloqueios sem limpeza | Risco aceito, apenas registrado (`ObservadorGoogleAgenda.java:40`): cresce por empresa e data, tolerável na escala atual.

## Problemas restantes
| Sev | Arquivo:linha | Problema |
|---|---|---|
| baixo | `src/main/java/com/example/demo/dto/empresa/EmpresaCreateDTO.java:29` | `@Schema` ainda diz "o bot já nasce com a reserva automática habilitada"; o YAML (`EmpresaCreateDTO.yaml:27`) já diz que liga exige agenda e que ausente nasce `false`. Java e YAML divergem. |
| baixo | `lacunas-front.md` (item 4.4 e última linha) | 4.4 pergunta se os testes de Prova entram nesta change, mas `tasks.md` já os marca feitos; "Estado: aprovado" no fim convive com os itens 4.1 a 4.3 sem decisão da pessoa. |
| baixo | `openspec/changes/corrigir-reserva-automatica/graphify-out/` | Pasta gerada pelo Graphify dentro da change (e `graphify-out/` na raiz, não rastreada). Não é artefato da change; não deve ir no commit. |
| baixo | `openspec/specs/empresa/spec.md:44` | Ainda diz que o cliente envia a reserva automática no PUT `/cliente`. Está coberto pela tarefa de Fechamento (regra de EMP-02); conferir na revisão. |

## Registrado (aceito)
- Mapa de bloqueios do observador sem limpeza (acima).
- Bloqueio em memória e reinício da API durante o registro: já documentados em "Riscos" do `design.md`.

## Conclusão
Núcleo coberto por prova: evento de dia inteiro, id determinístico, 409 como sucesso, regra do POST/PUT admin e PUT do cliente. Pode seguir para Revisão; os baixos 1 a 3 podem ser corrigidos na própria revisão (sem retorno à programação).
