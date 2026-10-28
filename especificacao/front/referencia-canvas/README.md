# Referência visual aprovada

Cópia do design aceito no canvas "Painel gamb — telas". Serve só de referência para quem programa o front. Não é código de produção e não entra no build.

| Arquivo | Vira no Angular |
|---|---|
| `Apresentacao.dc.html` | `features/apresentacao/` (rota `/`) |
| `Main.dc.html` | `features/auth/login/` (rota `/login`) |
| `AdminEmpresas.dc.html` | `features/admin/dashboard/` (rota `/admin`) |
| `AdminClienteForm.dc.html` | `features/admin/empresa-form/` (rota `/admin/empresas/:sessao`) |
| `AdminAvisos.dc.html` | `features/admin/gestao-avisos/` (rota `/admin/avisos`) |
| `ClientePainel.dc.html` | `features/client/painel/` (rota `/cliente`, painel inicial) |
| `ClienteChatbot.dc.html` | `features/client/perfil/` (rota `/cliente/chatbot`, configuração do chatbot) |
| `logo-gamb.png` | `frontend/public/logo-gamb.png` (PNG transparente, 640 px) |
| `conversa-whatsapp.jpg` | `frontend/public/` (imagem da apresentação; contato e nome já estão cobertos) |

## Como ler os `.dc.html`

- Cada arquivo é uma página fluida: a mesma marcação serve para celular (390 px) e desktop (1280 px). O que muda entre os dois está nos `@media` do `<helmet>` e nos `clamp()` / `flex-wrap` / `grid auto-fit` inline.
- `{{campo}}`, `<sc-for>`, `<sc-if>` e `onClick` são do runtime do canvas. No Angular viram `@for`, `@if` e `(click)`.
- Os dados de exemplo ficam no `<script>` do fim do arquivo. Texto entre colchetes, como `[NOME DA EMPRESA]`, é placeholder.
- `src="/_blob/..."` aponta para o upload do canvas. No Angular use os arquivos desta pasta: `logo-gamb.png` e `conversa-whatsapp.jpg`.
- Cores em hexadecimal já existem em `telas.design.json` (`brand-*` e `accent-*`). A exceção é `#9a4508`, que entra como `accent-800` (ver `design.md` da change). As cores do selo de risco do painel (verde, âmbar e vermelho) estão em `tema.cores.risco`.
- Os links `href="Main.dc.html"` e similares são a navegação entre telas: leia como a rota da tabela acima.
