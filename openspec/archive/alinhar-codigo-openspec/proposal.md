# alinhar-codigo-openspec

## Problema

O comportamento vigente já está em `openspec/specs/`. Parte dele tem prova, parte só tem o contrato. O código precisa ficar do lado do spec e do YAML, requisito por requisito, sem abrir feature nova.

## O que muda
### Entra
- Prova que o spec já cobra e ainda não existe:
  - `AUTH-02`: cenário de alterar a própria senha
  - `EMP-04`: HTTP 204, 400, 401 e 404 da redefinição de senha pelo admin
  - `AVI-02`: 400 de dados inválidos e 401 de POST/PUT
  - `IA-02`: 400 de JSON malformado e 401 sem credencial em `POST /api/ia-query`

### Altera
- Java, somente onde um requisito já escrito divergir do YAML em `especificacao/back/`. O contrato permanece a fonte da verdade.

### Sai
- Nada de rota, DTO ou tela.

## Fora de escopo

- Funcionalidade que não está em `openspec/specs/`
- Reescrever spec para combinar com o código
- Copiar DTO, campo de tela ou estilo para o spec
- Indexar documentos no Graphify (isso pede chave de API)

## Domínios
- spec: `openspec/specs/auth/spec.md`
- spec: `openspec/specs/empresa/spec.md`
- spec: `openspec/specs/whatsapp/spec.md`
- spec: `openspec/specs/agenda/spec.md`
- spec: `openspec/specs/avisos/spec.md`
- spec: `openspec/specs/ia/spec.md`
