# 4. Auditoria: empresa-form (bot-whatsapp-gamb-suporte-teste)

Agente novo, sem o contexto de quem escreveu o visual. Nenhum arquivo de produto editado.

## Verificação (frontend/)

- `npm run build`: sem erro (0 ocorrências de "error"). Só aviso de `RouterLink` não usado em `tutoriais.component.ts`, fora desta change.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 159 de 159 SUCCESS, nenhuma falha.

## Tabela da skill

| Item | Resultado |
|---|---|
| Disponível | ok. `/admin/empresas/nova` e `/admin/empresas/:sessao` em admin.routes.ts, lazy, authGuard + roleGuard('ADMIN') no pai. Console no navegador não verificado |
| Alcançável | ok. Ida: menu "Empresas" e "Cadastrar cliente" (nova); botão "Editar" por linha em dashboard.component.html:78-81 (edição, com aria-label). Volta: `← Voltar` para `/admin` |
| Estados | ok. Normal; e-mail inválido ("Há um e-mail inválido na lista."); mais de 10 ("Use no máximo 10 e-mails."); vazio (aviso laranja); salvando (readOnly + botão "Salvando..."). Campo só aparece na edição com ATENDENTE_GAMB |
| Texto | lacuna conhecida: textos fixos no HTML, admin sem content.json. Não bloqueia |
| Estilo | ok. `text-red-700` aceito (token de erro do perfil). `min-h-24` é utilitário padrão. Demais classes são brand/accent. Classes ainda literais no HTML, sem importar `telas.adminEmpresaForm` (decisão pendente da pessoa) |
| Responsivo | não verificado no navegador (tela exige login ADMIN e API). Leitura do HTML: `min-w-0`, `w-full`, `break-all`, `resize-y`, grid `minmax(min(100%,420px),1fr)`, sem largura fixa; sem risco de rolagem lateral em 375 px |
| Acesso | ok. `label for="empresa-responsaveis"` ligado ao textarea; `aria-invalid`; `aria-describedby` com ajuda, vazio e erro; erro com `role="alert"`; foco visível via `focus:ring-2` do `input-field` (e ring vermelho no erro) |
| Dado | ok. `emailsResponsaveisReuniao` existe em EmpresaUpdateAdminDTO.yaml (array, maxItems 10, format email) e em EntityModelEmpresa (entidades.yaml:77). Select traz ATENDENTE_GAMB e TESTE_MODULOS |

## Lacunas (não bloqueiam)

- Admin sem `*.content.json` e sem seção equivalente no uso do design JSON.
- `form.invalid` pode bloquear o salvar com o campo escondido (outro ramo) se o backend mandar valor inválido; anotado na fase 2.
- Opções ATENDENTE_GAMB e TESTE_MODULOS só aparecem na criação (`@if (!modoEdicao())`); correto, ramo não se edita.
- Inventário dizia não haver link de edição; a fase 3 corrigiu, confirmado aqui.

Estado: aprovado
