# 3. Navegação e estado local: empresa-form (bot-whatsapp-gamb-suporte-teste)

## O que mudou

| Arquivo | Mudança |
|---|---|
| `empresa-form.component.ts` | `emailsValidos` devolve `{ excessoEmails: true }` com mais de 10 e-mails (precedência sobre `emailInvalido`); `{ emailInvalido: true }` para e-mail malformado; constante `MAX_EMAILS_RESPONSAVEIS = 10` |
| `empresa-form.component.spec.ts` | 2 casos novos: 11 e-mails bloqueia o envio e mostra "Use no máximo 10 e-mails."; exatamente 10 passa. Testes existentes intactos |

Payload, service e contrato não foram tocados.

## Rotas e guards (confirmado, sem mudança)

- `/admin/empresas/nova` e `/admin/empresas/:sessao` em admin.routes.ts, lazy, `authGuard` + `roleGuard('ADMIN')` no pai `/admin`.
- Nenhum `*.routes.ts` alterado.

## Lacuna do inventário: corrigida pela leitura

O inventário disse que não havia link para `/admin/empresas/:sessao`. Existe: `dashboard.component.html:78`, botão "Editar" em cada linha da lista, com `[routerLink]="['/admin/empresas', empresa.sessaoWhatsapp]"`. Nada a decidir; nenhum link criado.

## telas.design.json no TS

Não importado. O empresa-form usa classes literais no HTML, sem o padrão `design.campo.*` do perfil, e trocar tudo está fora do pedido. Anotado para a pessoa decidir.

## Verificação (frontend/)

- `npm run build`: ok.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 159 de 159 SUCCESS.

Estado: aprovado
