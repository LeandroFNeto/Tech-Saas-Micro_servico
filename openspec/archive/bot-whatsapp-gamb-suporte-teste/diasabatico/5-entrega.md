# 5. Entrega: empresa-form (bot-whatsapp-gamb-suporte-teste)

## 1. Pronto por tela

- empresa-form (`/admin/empresas/nova` e `/admin/empresas/:sessao`): campo de e-mails dos responsáveis, só na edição com ramo ATENDENTE_GAMB, com ajuda, erros (e-mail inválido e mais de 10), aviso de lista vazia e estado salvando.

## 2. Lacunas

| Tela | O que falta | Quem resolve |
|---|---|---|
| admin (geral) | Sem `*.content.json`; textos novos e do bloco de agenda ficam fixos no HTML | texto |
| empresa-form | Classes literais no HTML; `telas.adminEmpresaForm` existe no JSON mas não é importado | visual |
| empresa-form | Não abri no navegador em 375, 768 e 1280 px; console não conferido (exige login ADMIN e API no ar) | visual |
| empresa-form | `form.invalid` pode travar o salvar se o campo escondido (outro ramo) vier com valor inválido do back | contrato |
| empresa-form | Textos "Até 10 e-mails." e "Use no máximo 10 e-mails." sem content | texto |
| empresa-form | `text-red-700` usado no erro, fora dos tokens brand/accent | visual |

## 3. Depende de API

- Limite de 10 e-mails: o front valida, mas o back também valida (`maxItems: 10` em EmpresaUpdateAdminDTO.yaml). Se o back mudar, a regra do front muda junto.
- Nada novo de contrato: `emailsResponsaveisReuniao` já existe no DTO e na entidade (entidades.yaml:77). Payload, service e contrato não foram tocados.
- Checagem visual ao vivo depende da API no ar e de login ADMIN.

## 4. O que a pessoa precisa decidir

1. Criar `admin.content.json` agora ou manter os textos no HTML?
2. Trocar as classes literais por `telas.adminEmpresaForm` (como o perfil do cliente) ou manter?
3. Corrigir o `form.invalid` para ignorar campo escondido agora ou deixar anotado?
4. Aceitar `text-red-700` como cor de erro?
5. Quem roda a checagem ao vivo em 375, 768 e 1280 px?

## Testes (estado final)

- Front: `npm run build` ok.
- Front: `npm test` 159 de 159 SUCCESS (ChromeHeadless).
- Aviso de build fora desta change: RouterLink não usado em `tutoriais.component.ts`.

Estado: aprovado
