# IA

Comportamento observável da consulta em grafo usada pela IA. O endpoint é GraphQL em `/api/ia-query`. Ele não substitui e não altera as rotas REST de `/empresas`. A resposta traz só os campos citados na query.

## Requisitos

### IA-01 Devolver só os campos pedidos

O sistema deve receber um documento GraphQL e responder com o envelope `data` limitado à seleção. Uma query de nome e tabela de preços não devolve o restante do cadastro.

Contrato: `especificacao/back/paths/ia-query.yaml` (`IaQueryRequest`, `IaQueryResponse`)
Prova: `src/test/java/com/example/demo/graphql/ConsultaEmpresaGrafoControllerTest.java`, `src/test/java/com/example/demo/graphql/EconomiaTokenAuditoriaTest.java`

Cenário: seleção mínima

- Quando a query pede `{ empresas { nome tabelaDePrecos } }`
- Então a resposta 200 traz esses dois campos
- E o objeto da empresa não inclui os demais campos do cadastro

### IA-02 Admin vê todas; cliente vê só a sua

O sistema deve exigir JWT Bearer ou `x-admin-token`. Admin lista todas as empresas. Cliente autenticado lê somente a empresa ligada ao próprio usuário: o id de outra empresa não volta dados, e a listagem não percorre a base inteira. Token de cliente sem empresa devolve lista vazia e não consulta o banco. Id que não é numérico, ou sessão em branco, não consulta o banco.

Contrato: `especificacao/back/paths/ia-query.yaml`
Prova: `src/test/java/com/example/demo/graphql/ConsultaEmpresaGrafoTest.java` e `src/test/resources/features/ia-query.feature` (HTTP 400 e 401).

Cenário: admin lista todas

- Dado um admin autenticado e duas empresas
- Quando a consulta lista empresas
- Então as duas entram no resultado

Cenário: cliente lê só a própria

- Dado um cliente ligado à empresa 7
- Quando ele busca o id 7
- Então recebe a própria empresa
- Quando ele busca o id 8
- Então o resultado é vazio
- E a listagem não chama a leitura de todas as empresas

Cenário: cliente sem empresa

- Dado um token de cliente sem empresa associada
- Quando ele lista empresas
- Então a lista é vazia
- E o banco não é consultado

Cenário: id ou sessão inválidos

- Dado um admin autenticado
- Quando o id não é numérico ou a sessão está em branco
- Então o resultado é vazio
- E o banco não é consultado

### IA-03 Não expor segredo nem calendário

O sistema deve limitar a projeção a dados de atendimento: id, nome, tabela de preços, ramo, regras de locação, saudação, sessão WhatsApp, links de mapa, foto e galeria, flag de IA, locação por hora e reserva automática. Usuário, senha e `googleCalendarId` não saem nesta camada.

Contrato: `especificacao/back/paths/ia-query.yaml`
Prova: `src/test/java/com/example/demo/graphql/EconomiaTokenAuditoriaTest.java` (ausência de `googleCalendarId` na projeção). A ausência de usuário e senha está no contrato e no tipo `EmpresaConsulta`.

Cenário: projeção da IA omite o calendário

- Quando a consulta devolve a empresa para a IA
- Então o JSON não contém `googleCalendarId`
