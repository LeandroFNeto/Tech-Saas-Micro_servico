# Avisos

Comportamento observável dos comunicados do painel. O cliente vê só aviso ativo. O admin gere a lista completa. O payload é `AvisoDTO` (`tipo`, `titulo`, `conteudo`, `link`, `ativo`).

## Requisitos

### AVI-01 Listar avisos ativos

O sistema deve devolver os avisos com `ativo=true`, do mais recente para o mais antigo. A rota é pública.

Contrato: `especificacao/back/paths/avisos.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerAvisoTest.java`

Cenário: painel do cliente lista ativos

- Dado avisos ativos e inativos
- Quando o cliente envia GET `/avisos` sem token
- Então a API responde 200
- E a lista traz só os ativos

### AVI-02 Admin lista, cria, atualiza e exclui

O sistema deve exigir `x-admin-token` em `/admin/avisos`. A listagem inclui ativos e inativos. Criar devolve o aviso gravado. Atualizar substitui título, tipo, conteúdo, link e a flag `ativo`. Excluir remove o registro. Id inexistente em atualizar ou excluir responde 404.

Contrato: `especificacao/back/paths/admin-avisos.yaml`, `especificacao/back/paths/admin-avisos-id.yaml`
Schema: `especificacao/back/schemas/aviso/AvisoDTO.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerAvisoTest.java` (401 e 400 de POST/PUT, 401 no GET admin, 201 no POST, 200 no PUT, 204 no DELETE e 404 no DELETE de id inexistente).

Cenário: admin sem token

- Quando GET `/admin/avisos` chega sem token
- Então a API responde 401

Cenário: admin lista tudo

- Dado o token administrativo válido
- Quando o admin envia GET `/admin/avisos`
- Então a API responde 200
- E a lista inclui ativos e inativos

Cenário: admin cria aviso

- Dado o token administrativo válido
- Quando o admin envia POST `/admin/avisos` com tipo, título e conteúdo
- Então a API responde 201
- E o aviso fica gravado ativo, salvo indicação contrária

Cenário: admin atualiza aviso existente

- Dado um aviso já gravado e o token administrativo válido
- Quando o admin envia PUT `/admin/avisos/{id}`
- Então a API responde 200
- E tipo e título passam a ser os enviados

Cenário: admin exclui aviso

- Dado um aviso já gravado e o token administrativo válido
- Quando o admin envia DELETE `/admin/avisos/{id}`
- Então a API responde 204

Cenário: excluir id inexistente

- Quando o admin envia DELETE `/admin/avisos/{id}` de um id que não existe
- Então a API responde 404
