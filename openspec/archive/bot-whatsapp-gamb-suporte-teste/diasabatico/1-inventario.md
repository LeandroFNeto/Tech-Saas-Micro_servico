# 1. Inventário: empresa-form (bot-whatsapp-gamb-suporte-teste)

Mudança sem commit em frontend: `empresa-form.component.{ts,html,spec.ts}`, `empresa.service.ts`, `empresa.models.ts`. Nenhum arquivo de produto editado nesta fase.

## 1. Rotas até o empresa-form

| Rota | Componente | Guard |
|---|---|---|
| `/admin/empresas/nova` (admin.routes.ts) | EmpresaFormComponent, lazy | authGuard + roleGuard('ADMIN') no pai `/admin`; ShellComponent |
| `/admin/empresas/:sessao` (admin.routes.ts) | EmpresaFormComponent, lazy | igual |

`*.routes.ts` do cliente não tem rota para ele.

## 2. Links que chegam nele

| Origem | Destino |
|---|---|
| shell.component.html:88 e :176 (menu "Empresas") | routerLink `/admin/empresas/nova` |
| dashboard.component.html:14 ("Cadastrar cliente") | routerLink `/admin/empresas/nova` |
| Edição `/admin/empresas/:sessao` | nenhum link achado no código (lacuna: só por URL direta) |

## 3. Onde o componente é usado

Só nas duas rotas de admin.routes.ts. Spec: `empresa-form.component.spec.ts`, com 3 blocos de teste (6 `it` novos na change).

## 4. O que a modificação pede de tela

| Item | Tipo |
|---|---|
| Textarea "E-mails dos responsáveis da reunião", só na edição e com ramo ATENDENTE_GAMB, com erro e texto de ajuda | nova |
| Opções ATENDENTE_GAMB e TESTE_MODULOS no select de ramo | ajuste |
| Campo `emailsResponsaveisReuniao` (form, validador, service, model) | só dado, sem tela |

Nenhuma rota nova.

## 5. Classes, tokens e textos

| Item | Situação |
|---|---|
| `input-field`, `shadow-none`, `font-mono` | já usados; `input-field` em styles.css:23 |
| Bloco "ID da agenda Google" (label `text-brand-800`, input, ajuda) | padrão que o bloco novo copia |
| `min-h-[96px]` no textarea | classe arbitrária, fora do telas.design.json |
| `text-red-700` na mensagem de erro | cor fora dos tokens `brand`/`accent` |
| `text-brand-600` na ajuda | token brand, ok |
| telas.design.json | sem seção de admin nem de empresa-form |
| *.content.json | não existe para admin |
| Textos novos (label, placeholder, erro, ajuda) | hardcoded no HTML, como os demais campos do form |

## Lacunas (só anotadas, não corrigidas)

- Admin não tem content.json nem seção em telas.design.json.
- Textos novos e os do bloco de agenda ficam hardcoded no HTML.
- `text-red-700` e `min-h-[96px]` fora dos tokens.
- Nenhum link de edição `/admin/empresas/:sessao` no código.

Graphify: `graphify update .` rodou ao fim desta fase; ele regenera `graphify-out/`, não é arquivo de produto.

Estado: aprovado
