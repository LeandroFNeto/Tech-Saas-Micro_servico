# Auditoria: corrigir-reserva-automatica

Agente novo, só leitura de produto. Pesquisa pelo Graphify; leitura direta dos arquivos indicados e do diff.

## Comandos
- `npm run build` em `frontend/`: passou (bundle gerado, sem erro).
- `npm test -- --watch=false --browsers=ChromeHeadless`: 38 de 38 SUCCESS. Existem `perfil.component.spec.ts` e `empresa-form.component.spec.ts`; nenhum falhou e nenhum dependia do campo que o perfil deixou de enviar.

## Achados por item (perfil `/cliente/chatbot` aba robo, `EmpresaFormComponent`, 2 canvas)
- Disponível: ok. Rotas e guards inalterados (authGuard + roleGuard); build e testes sem erro. Console no navegador não foi aberto (sem app rodando).
- Alcançável: ok. Voltar do perfil (`/cliente`) e do form (`/admin`) intactos; painel e shell levam ao perfil.
- Estados: ok. Perfil: switch só leitura com aviso. Admin: erro de linha (borda e fundo vermelhos) e aviso do topo `role="alert"`; `salvando` sempre volta a false no erro. Canvas admin mostra o erro com `erroAgenda` ligado.
- Texto: perfil ok (aviso em `cliente-perfil.content.json`). Admin: rótulo, dica e a frase do 400 fixos no HTML/TS; tela admin não tem content.json (padrão já registrado na fase 2, lacuna). A frase fixa no TS repete a do back.
- Estilo: perfil ok (`linhaLeitura`, `aviso`, `avisoIcone` em `telas.design.json`). Admin usa classes Tailwind soltas (`border-red-300`, `bg-red-50`, `text-red-700`) fora dos tokens; padrão da tela, sem JSON.
- Responsivo 375 px: não medido em navegador. Pela leitura, linhas flex com `gap-4`, texto quebra e switch fixo; sem causa de rolagem horizontal.
- Acesso: ok. Perfil: switch `disabled` com `aria-labelledby` e `aria-describedby`, ícone `aria-hidden`. Admin: `label` envolve o campo, `aria-invalid` e `aria-describedby` ligados ao erro.
- Dado: ok. `permiteReservaAutomatica` existe em `EmpresaCreateDTO`, `EmpresaUpdateAdminDTO`, `EmpresaUpdateDTO` e `EmpresaResponseDTO`; o front envia no POST e PUT admin e não envia no PUT do cliente.
- Texto do 400: `ControllerEmpresa` devolve "Cadastre a agenda do Google antes de ligar a reserva automática." (contém "agenda"), POST e PUT do admin; `server.error.include-message=always` faz `error.message` chegar a `mensagemHttp`. `tratarErroSalvar` (status 400 + /agenda/i) reconhece. Risco menor: outro 400 do form que cite "agenda" também acende o erro da linha.

## Problemas e fase de origem
1. Nenhum spec cobre o comportamento novo (perfil sem o campo no PUT; `tratarErroSalvar`; switch admin). Fase 3; vira lacuna para a tarefa "Prova".
2. Variável `_somenteLeitura` não usada em `perfil.component.ts` (build não reclama). Fase 3, cosmético.
3. Admin com cor e texto fora de tokens/content.json. Fase 2, lacuna já conhecida.
4. Responsivo 375 px e console sem prova em navegador. Fase 4 (esta), pendente de checagem manual.

Estado: aprovado
