# Só o visual: corrigir-reserva-automatica

## O que foi feito

- Cliente (`/cliente/chatbot`, aba robo): a linha "Reserva Automática pelo Bot" virou só leitura. Fundo lilás claro (`brand-50`), sem cursor de clique, switch desabilitado (meio apagado) e, abaixo da dica, um cadeado com "A reserva automática é liberada pelo administrador." em roxo escuro (`brand-700`, contraste bom). O switch mostra o valor real, mas não muda.
- Admin (`empresa-form`): novo switch "Reserva automática pelo bot" logo abaixo do "ID da agenda Google", no cadastro e na edição (fora do `@if (modoEdicao())`), com a mesma dica do cliente. Com o 400 da agenda, a linha fica com borda e fundo vermelhos claros e a mensagem do back aparece logo abaixo; o aviso geral do topo (`erro()`, `role="alert"`) continua sendo o anúncio para leitor de tela.
- Canvas: `ClienteChatbot.dc.html` (opção só leitura + aviso) e `AdminClienteForm.dc.html` (switch novo, aviso no topo e erro na linha, com `erroAgenda` de exemplo ligado).
- Responsivo: as linhas são flex com quebra de texto e switch fixo; nada alarga a página em 375, 768 ou 1280.
- Acesso: no cliente o switch usa `aria-labelledby` (rótulo) e `aria-describedby` (dica + aviso); no admin o `label` envolve o campo, com `aria-invalid` e `aria-describedby` apontando para o erro.

## Arquivos tocados

- `especificacao/front/referencia-canvas/ClienteChatbot.dc.html`
- `especificacao/front/referencia-canvas/AdminClienteForm.dc.html`
- `frontend/src/app/features/client/perfil/perfil.component.html`
- `frontend/src/app/features/admin/empresa-form/empresa-form.component.html`
- `especificacao/front/telas.design.json` (`clientePerfil.opcao.linhaLeitura`, `aviso`, `avisoIcone`)
- `especificacao/front/cliente-perfil.content.json` (`clientePerfil.robo.reservaAutomatica.aviso`)
- `openspec/changes/corrigir-reserva-automatica/tasks.md` (só "Front, canvas" marcada)

## O que a fase 3 precisa saber

1. Perfil: o input não usa mais `formControlName`; lê `form.controls.permiteReservaAutomatica.value` via `[checked]` e é `disabled`. O controle pode ficar no form para exibir o valor, mas `salvar()` deve tirar `permiteReservaAutomatica` do corpo do PUT.
2. Admin: o HTML usa `formControlName="permiteReservaAutomatica"`, que não existe no form. Adicionar `permiteReservaAutomatica: [false]`, preencher em `preencher()` e enviar no POST e no PUT. Sem isso a tela quebra em tempo de execução.
3. Admin: o HTML usa `erroReservaAutomatica()`, que não existe (o build falha até a fase 3). Criar `signal('')`; no 400 do POST/PUT, setar `erro()` como hoje e também `erroReservaAutomatica()` quando a mensagem for a da agenda; limpar os dois no início de `salvar()`.

## Lacunas

- Tela admin segue com texto fixo no HTML (padrão dela), sem `*.content.json` nem entrada em `telas.design.json`: rótulo, dica e classes do switch novo também ficaram no HTML.
- Nenhum texto de produto novo além do aviso pedido; a dica do admin reaproveita a do cliente.
- Ordem: o canvas foi gravado na mesma passada do Angular, não antes; o resultado bate entre os dois.

Estado: aprovado
