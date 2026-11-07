# Design

## Contexto

- `empresa-form` já tem os interruptores de reserva automática e previsão dentro de "Dados da empresa" e a lista de módulos em "Gestão de módulos". O PUT do admin já aceita `permiteReservaAutomatica`, `permitePrevisaoTempo` e `modulosAtivos`.
- `usaIA` é calculado no back a partir de `IA_GEMINI` em `modulosAtivos` (`CatalogoModulos`). O front só não lista esse código.
- `painel.component` já recebe `permitePrevisaoTempo`, mas mostra o interruptor desabilitado com o aviso `bloqueadaAdmin`.
- O DELETE e o modal de confirmação ficam hoje em `dashboard.component`.

## Decisões

1. **Sem mudança no back nem no contrato.** A IA vira só mais um item de `CATALOGO_MODULOS`, com código `IA_GEMINI`. O back já liga `usaIA` por ele.
2. **Seção "Recursos da empresa".** Os interruptores de reserva automática e previsão saem de "Dados da empresa" e entram na seção de módulos, que passa a se chamar "Recursos da empresa". Os controles do formulário (`permiteReservaAutomatica`, `permitePrevisaoTempo`, `modulos`) não mudam de nome; só o lugar no HTML. A mensagem de erro da agenda (`erroReservaAutomatica`) acompanha o interruptor.
3. **Menu do bot × IA.** `IA_GEMINI` entra em `modulosAtivos` junto dos outros e o back o trata como já trata hoje. Não entra em `MODULOS_MENU_BOT`, que é a lista de botões do menu que o cliente edita.
4. **Previsão no painel do cliente.** O bloco inteiro do painel fica dentro de `@if (permitePrevisaoTempo())`. Sem permissão, `buscarPrevisao()` não roda. O texto `bloqueadaAdmin` sai de `cliente-painel.content.json` e do componente. O padrão atual `permitePrevisaoTempo !== false` (campo ausente conta como liberado) é mantido para não esconder a previsão por resposta antiga da API.
5. **Exclusão na edição.** A zona de exclusão é uma seção própria, depois de "Redefinir senha", fora do `<form>` principal, com borda e título em vermelho. O modal de confirmação e o `excluir()` se mudam do dashboard para `empresa-form`, com a mesma checagem `auth.isSuperAdmin()`. Em modo cadastro a zona não aparece. Ao concluir, navega para `/admin`; em erro, mostra a mensagem no topo da página.
6. **Dashboard.** Remove botão, modal e os signals de exclusão (`confirmacaoAberta`, `empresaExcluindo`, `excluindo`) e os testes correspondentes, que passam para o spec do `empresa-form`.

## Riscos

- Empresa antiga com `IA_GEMINI` fora do catálogo do front: ao salvar, o módulo é preservado só se o interruptor da IA estiver ligado. Como a tela agora lista o código, o estado carregado reflete o real.
- Mover o botão tira a exclusão rápida da lista; é a troca desejada.
- Layout do formulário em duas colunas: a seção de recursos vai para a coluna da direita, onde já estão os módulos; conferir no celular que a ordem fica coerente.

## Questões em aberto

Nenhuma que bloqueie. Se o admin quiser esconder o atalho do chatbot em empresas sem IA, é outra change.
