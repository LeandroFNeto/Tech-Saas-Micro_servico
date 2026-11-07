# Tarefas

## Canvas e conteúdo
- [x] Passar a mudança de tela pelo canvas de design (edição da empresa com "Recursos da empresa" e zona de exclusão; painel do cliente sem a previsão) e atualizar `especificacao/front/telas.design.json`
- [x] Remover `bloqueadaAdmin` de `especificacao/front/cliente-painel.content.json`

## Admin: edição da empresa
- [x] Adicionar `IA_GEMINI` ("Atendente virtual com IA") a `CATALOGO_MODULOS` em `frontend/src/app/shared/models/empresa.models.ts`
- [x] Reunir IA, previsão do tempo, reserva automática e módulos na seção "Recursos da empresa" em `empresa-form.component.html`, mantendo o erro da agenda junto da reserva automática
- [x] Mover o botão e o modal de exclusão para o fim de `empresa-form` (depois de "Redefinir senha", fora do form principal), só para super admin e só em modo edição; ao excluir, ir para `/admin`
- [x] Remover botão "Excluir", modal e signals de exclusão de `dashboard.component.html` e `dashboard.component.ts`

## Cliente: painel
- [x] Esconder o bloco da previsão em `painel.component.html` quando `permitePrevisaoTempo()` for falso; não chamar `buscarPrevisao()` nesse caso; apagar o texto "bloqueada pelo admin"

## Provas
- [x] `empresa-form.component.spec.ts`: IA liga e desliga `IA_GEMINI` no PUT; estado carregado; zona de exclusão (super admin, confirma, cancela, outro perfil, volta para `/admin`)
- [x] `dashboard.component.spec.ts`: lista só com "Editar"; remover os testes de exclusão
- [x] `painel.component.spec.ts`: sem permissão não há bloco nem chamada de previsão; com permissão segue igual
- [x] Conferir a tela no navegador (desktop e celular) — testes do front: 200 de 200 passam

## Fechamento
- [x] Sincronizar `openspec/specs/empresa/spec.md` com o delta, editando à mão: o cabeçalho `### EMP-06 ...` da spec principal não tem o prefixo `Requirement:`, então o sync automático não o encontra (EMP-06 modificado, EMP-15 e EMP-16 novos; EMP-13 passa a citar a página de edição)
- [x] Mover esta pasta para `openspec/archive/ajustar-edicao-empresa-recursos-e-exclusao/`
