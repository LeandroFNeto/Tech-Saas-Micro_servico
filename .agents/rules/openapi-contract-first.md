# OpenAPI contract-first

A Fonte Única de Verdade da API HTTP é a pasta `especificacao/back/`, montada por `especificacao/back/swagger.yaml`.

## Onde editar

| Tipo | Pasta |
|---|---|
| Rotas | `especificacao/back/paths/` |
| DTOs de empresa | `especificacao/back/schemas/empresa/` |
| Webhook | `especificacao/back/schemas/webhook/` |
| HAL / Data REST | `especificacao/back/schemas/data-rest/` |
| Índice | `especificacao/back/swagger.yaml` (só `$ref`; não coloque detalhe aqui) |
| Conteúdo das telas | `especificacao/front/*.content.json` |
| Design das telas (cor, tamanho, campos) | `especificacao/front/telas.design.json` |

## Fluxo (não inverter)

1. Escreva a mudança no arquivo YAML certo dentro de `especificacao/back/`.
2. Se for rota ou schema novo, registre o `$ref` em `especificacao/back/swagger.yaml`.
3. Só então altere o Java para cumprir o contrato.
4. Não regenere o contrato a partir do SpringDoc.

## Regras

- POST empresa → `EmpresaCreateDTO`; PUT cliente → `EmpresaUpdateDTO`; PUT admin → `EmpresaUpdateAdminDTO`; resposta → `EmpresaResponseDTO`; webhook → `WhatsappWebhookDTO`.
- Não exponha entidade JPA nem `Map<String, Object>` como payload.
- Se Java e YAML divergirem, **corrija o Java**.
