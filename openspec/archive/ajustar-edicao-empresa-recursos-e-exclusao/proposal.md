# ajustar-edicao-empresa-recursos-e-exclusao

## Problema

Na tela de edição da empresa (`/admin/empresas/:sessao`) o admin não controla todos os recursos da empresa em um lugar só:

- A IA (módulo `IA_GEMINI`, que liga `usaIA`) existe no back, mas não aparece na lista de módulos da tela; o admin não consegue ligar nem desligar.
- "Liberar previsão do tempo" e "Reserva automática" ficam soltos dentro de "Dados da empresa", misturados com campos de texto.
- No painel do cliente (`/cliente`), quando o admin não liberou a previsão, o interruptor continua visível, só desabilitado e com um aviso. O cliente vê uma opção que não pode usar.
- O botão "Excluir" fica em cada linha da lista do dashboard, ao lado de "Editar". É fácil clicar nele sem querer.

## O que muda

### Entra
- Seção "Recursos da empresa" na tela de edição, reunindo em interruptores: IA, previsão do tempo, reserva automática e os módulos do bot que já existem.
- Módulo `IA_GEMINI` no catálogo de módulos do front, com rótulo e descrição.
- Zona de exclusão no fim da página de edição, separada do formulário, com confirmação antes de excluir.

### Altera
- Painel do cliente: sem a permissão `permitePrevisaoTempo`, o interruptor de ativar a previsão e o texto de "bloqueada pelo admin" deixam de aparecer. O bloco da previsão inteiro some quando a empresa não tem a opção.
- Dashboard admin: sai o botão "Excluir" e o modal de confirmação da lista. Fica só "Editar".
- Depois de excluir pela página de edição, o admin volta para `/admin`.

### Sai
- Nada muda no back: o DELETE `/empresas/{sessao}` (EMP-13), o PUT do admin e os campos `permitePrevisaoTempo`, `permiteReservaAutomatica` e `modulosAtivos` ficam como estão.

## Capabilities

### Modified Capabilities
- `empresa`: EMP-05/EMP-06 (previsão some do painel do cliente sem permissão), EMP-13 (exclusão feita na página de edição) e requisito novo do bloco de recursos na edição.

## Suposições

- "Opção de IA" é o módulo `IA_GEMINI`; não se cria campo novo no back.
- "Menu principal do cliente" é o painel `/cliente`, onde mora o interruptor da previsão.
- Excluir continua restrito ao super admin, como hoje.
- Empresa criada (modo cadastro) também mostra os interruptores de recursos, como já mostra os módulos.

## Fora de escopo

- Novos planos ou cobrança por recurso.
- Alterar o back, o contrato YAML ou a regra de desativação lógica.
- Mudar o menu lateral do cliente além da previsão.

## Impacto

- Front: `empresa-form`, `dashboard` (admin), `painel` (cliente), `empresa.models.ts` e os `.spec.ts` dessas telas.
- Conteúdo de tela: `especificacao/front/cliente-painel.content.json`.
- Spec: `openspec/specs/empresa/spec.md`.
