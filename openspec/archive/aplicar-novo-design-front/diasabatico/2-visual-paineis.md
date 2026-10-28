# Fase 2B: visual, telas de painel (26/10/2026)

## O que foi feito, por arquivo
- `features/admin/dashboard/dashboard.component.html`: título h1 + "Cadastrar cliente" (48 px). Celular: um cartão por empresa (raio 20, borda brand-100), rótulos pequenos por campo, status em `.chip` (CONNECTED = `bg-accent-50 text-accent-800`, resto `bg-brand-100 text-brand-700`), Editar em `btn-secondary` largura total (44 px). `min-[1100px]:` liga cabeçalho e grade `1.6fr 1.6fr 1fr .7fr 1.1fr 96px`. Erro (vermelho, "Tentar de novo"), carregando e vazio mantidos; `routerLink`s iguais. A `<table>` saiu.
- `features/admin/empresa-form/empresa-form.component.html`: grade `auto-fit minmax(420px)`; coluna 1 = Dados da empresa + Fotos, coluna 2 = Gestão de módulos (`.switch`) + Conexão WhatsApp. `label for`/`id` em todo campo. Modo criar (ramo, preço base, e-mail, senha) no mesmo cartão "Dados da empresa", mesmo padrão, sem layout novo. Miniatura da foto principal ao lado de "Enviar foto principal" (moldura tracejada quando vazia, botão clicável com anel quando há imagem); galeria com miniatura 44 px, "Enviar fotos", "Adicionar URL", "Remover"; prévia "Imagem selecionada" mantida. Textos trocados como pedido; "Salvar alterações" / "Criar cliente". Locação por hora (só edição) virou `.switch`.
- `shared/components/conexao-whatsapp/`: cartão do desenho, `.chip` Online / Aguardando leitura / Desconectado, QR Code, botão Conectar 48 px. `.ts`: só `@Input() compacto` (esconde a frase de instrução). Polling e lógica intactos.
- `features/admin/gestao-avisos/`: lista em cartões (chip de tipo + chip de status, Editar e Excluir lado a lado, `flex-none` no `lg:`), formulário em cartão com `.switch` em Ativo, Cancelar só na edição. `.ts`: só o mapa `corTipo` (cores pedidas).
- `features/client/perfil/` + `cliente-perfil.content.json` + `telas.clientePerfil`: h1; MANUTENÇÃO no topo; conexão compacta; abas = seletor segmentado com rolagem (`role="tablist"`, `role="tab"`, `aria-selected`, painel `role="tabpanel"`); campos com `label for`; Robô com `.switch` e cartões de menu (switch com `aria-label` "Ativar <opção>"); senha com a dica "Use ao menos 6 caracteres." (`senha.novaSenhaDica`, ligada por `aria-describedby`); coluna lateral "Avisos e tutoriais". `.ts` não mudou.
- JSON de texto: dicas da foto e da galeria e a descrição do menu iguais ao desenho; novas chaves `abas.rotulo`, `fotoPrincipal.verMaior`, `galeria.rotuloEntrada`, `galeria.verMaior`, `menu.ativar` (saiu `menu.ativo`, sem uso).
- JSON de estilo: `telas.clientePerfil` reescrita (mesmas chaves que o `.ts` usa: `abas.*`, `selecao.*`; novas `campo.*`, `botaoPrimario`, `foto.linha/vazia`, `menu.grupo`, `pagina.colunaLateral`). `tema.componentes` intacto (o perfil passou a usar `clientePerfil.campo`).
- `shared/components/banner-avisos/`: sem `compacto` = só MANUTENÇÃO (faixa `bg-red-700`, rótulo red-200); `compacto` = só os que NÃO são manutenção, cartão raio 20, borda brand-200 (Preço: accent-200 + accent-50), rótulo colorido por tipo, link vira `btn-secondary` "Ver tutorial →" / `cta()` + "→". Entradas `avisos` e `compacto` iguais; só `cardTipo`/`textoTipo` mudaram de cor.

## Decisões de design
- Cartões com Tailwind direto no HTML nas telas de admin (como o shell); o perfil segue no JSON.
- No form de admin, o cartão de conexão entrou DENTRO do `<form>` (coluna 2, como o desenho); os botões dele são `type="button"`, não enviam o formulário.
- O link de texto da URL da foto (ao lado da miniatura) saiu: a URL já está no campo.
- Botões de ação do perfil ocupam a largura do cartão, como no desenho.

## Para a fase 3 e a auditoria
- Conferir no navegador em 375/768/1280 px (não visto: telas exigem login e API). Pontos: tabela de empresas a 1100 px, abas roláveis a 375 px, linha da galeria (miniatura + campo + Remover) a 375 px.
- Abas sem navegação por setas (todas focáveis por Tab); avaliar se a auditoria pede roving tabindex.
- Texto fixo continua no HTML nas telas de admin, banner e conexão (como o `design.md` prevê).

## Verificação
- `npm run build` em `frontend/`: sem erro e sem aviso de budget.
- CSS gerado (`dist/frontend/browser/styles-*.css`): `.switch` (9 regras), `.chip`, `text-accent-800`, `bg-red-700`, `min-[1100px]:*`, `flex-[999_1_520px]` e os `grid-cols-[repeat(auto-fit,…)]` presentes.

## Riscos
- `role="alert"` na faixa de manutenção é lido ao abrir a página (como o desenho).
- Botão "Remover" e miniatura deixam pouco espaço ao campo da galeria em 375 px (o campo tem `min-w-0`, encolhe sem rolar a página).

Estado: aprovado
