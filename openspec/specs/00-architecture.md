# Arquitetura

Contrato da stack e das fronteiras. Código novo segue este documento e os demais arquivos em `openspec/specs/`.

## Stack

| Camada | Tecnologia |
|---|---|
| Backend | Java 21, Spring Boot 3.4 (parent `3.4.3`) |
| Frontend | Angular 19, Tailwind CSS |
| Contrato HTTP | OpenAPI (Swagger): `especificacao/back/`, montado por `especificacao/back/swagger.yaml` |
| Telas | Conteúdo em `especificacao/front/*.content.json`; design em `especificacao/front/telas.design.json` |

## Orientação

- A API HTTP é contract-first: o YAML em `especificacao/back/` é a fonte da verdade; o Java cumpre o contrato.
- Payloads usam DTOs nomeados. Entidade JPA e `Map<String, Object>` não saem na API.
- O frontend Angular 19 consome esse contrato; o estilo usa Tailwind.
- A consulta da IA é leitura GraphQL em `/api/ia-query`. Ela não substitui as rotas REST de `/empresas`.
- POST `/api/ia-query` é leitura GraphQL para a IA. Não substitui as rotas REST de `/empresas` e não devolve usuário, senha nem `googleCalendarId`.
- O comportamento observável de cada domínio fica nesta pasta. Feature nova passa antes por `openspec/changes/`; depois de aplicada, o contrato consolidado volta para `specs/` e o histórico vai para `openspec/archive/`.
