# Design

## Decisão

O design aceito está em `especificacao/front/referencia-canvas/` (ver o `README.md` de lá). O Angular reproduz esse visual com Tailwind, sem biblioteca nova. Celular primeiro: as classes sem prefixo valem para 390 px; `lg:` (1024 px) liga a barra lateral e `min-[1100px]:` liga a tabela de empresas.

**Rotas** (`app.routes.ts`): `''` carrega `ApresentacaoComponent` (lazy, standalone). `**` redireciona para `''`. A apresentação, como o login, manda quem já está autenticado para `auth.rotaInicial()`. `login`, `admin` e `cliente` e seus guards não mudam. `logout()` continua indo para `/login`.

**Texto e estilo** seguem o padrão do perfil: classes em `especificacao/front/telas.design.json` e textos em `*.content.json`. As telas novas (apresentação e login) ganham `apresentacao.content.json` e `login.content.json`. As demais mantêm o texto no HTML, exceto onde a lista abaixo muda o texto.

**Classes comuns** entram em `frontend/src/styles.css` (`@layer components`), ao lado de `btn-primary`, `btn-secondary`, `input-field` e `card`: `.switch` (interruptor de 48×28), `.chip` e `.wordmark` (degradê cobre `#bc8561` → `#f48c52` → `#e2b57f`, com `bg-clip-text`). `.input-field` passa a `min-h-12` e `text-base`.

**Cores**: todas as do canvas já existem em `brand-*` e `accent-*`. Entra só `accent-800: #9a4508` em `telas.design.json` (texto dos chips e da aba ativa, com contraste legível). Vermelho dos erros e da manutenção usa o `red-*` padrão do Tailwind.

**Logo**: `frontend/public/logo-gamb.png` substitui o `.jpg`. Sem borda arredondada e sem anel, porque a logo já é um círculo. Tamanhos: 40 px nas barras, 44 px no topo da apresentação, de 64 a 96 px no login.

**Mapa de telas**

| Canvas | Componente |
|---|---|
| `Apresentacao` | novo `features/apresentacao/apresentacao.component` |
| `Main` | `features/auth/login/login.component` |
| menu (todas as telas de painel) | `shared/components/shell/shell.component` |
| `AdminEmpresas` | `features/admin/dashboard/dashboard.component` |
| `AdminClienteForm` | `features/admin/empresa-form/empresa-form.component` e o cartão de `shared/components/conexao-whatsapp` |
| `AdminAvisos` | `features/admin/gestao-avisos/gestao-avisos.component` |
| `ClientePerfil` | `features/client/perfil/perfil.component`, `shared/components/banner-avisos` e `conexao-whatsapp` |

**Textos que mudam**
- Erro de login (401): "E-mail ou senha inválidos. Confira os dados e tente de novo." no lugar de "Senha incorreta". Não revela qual dado falhou. O contrato (401) não muda.
- Foto do cliente: "Fazer Upload da Foto Principal" → "Enviar foto principal"; "Adicionar Fotos à Galeria" → "Enviar fotos".
- Botão de salvar no modo edição do cliente: "Salvar infraestrutura" → "Salvar alterações".
- Dica nova na senha nova do perfil: "Use ao menos 6 caracteres." (a regra de 6 caracteres já existe).
- Apresentação: textos novos, em `apresentacao.content.json`.

**Acessibilidade e toque**: alvo mínimo de 44 px, `label` ligado ao campo, `<button>` e `<a>` de verdade, `aria-label` nos ícones, foco visível, interruptor com `input[type=checkbox]` real por baixo.

## Arquivos
- Contrato: nenhum. `especificacao/back/` e `swagger.yaml` não mudam.
- Java: nenhum.
- Front: `frontend/src/app/app.routes.ts`, `features/apresentacao/` (novo), `features/auth/login/`, `shared/components/shell/`, `features/admin/dashboard/`, `features/admin/empresa-form/`, `features/admin/gestao-avisos/`, `features/client/perfil/`, `shared/components/banner-avisos/`, `shared/components/conexao-whatsapp/`, `frontend/src/styles.css`, `frontend/src/index.html`, `frontend/public/` (logo e imagem da apresentação), `frontend/tailwind.config.js` (só se faltar algum token), `especificacao/front/telas.design.json`, `especificacao/front/cliente-perfil.content.json` e os dois `*.content.json` novos.
- Prova: `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts` (novo), `app.component.spec.ts` (continua passando), `npm run build` e conferência manual em 375, 768 e 1280 px.

## Alinhamento
- Stack de `00-architecture.md`
- Sem Java e sem YAML: nenhum DTO, rota de API ou `$ref` novo
- `.cursor/skills/diasabatico/SKILL.md` conduz a parte visual; modelos pela coluna da ferramenta em uso

## Riscos
- **Texto de produção**: a linha "Ainda não tem acesso? Fale com a gamb: [CONTATO DA GAMB]" não pode ir ao ar com o placeholder. A linha fica fora do build até a pessoa informar o contato.
- **Imagem da apresentação**: `conversa-whatsapp.jpg` é uma conversa real. Contato, nome da empresa e a linha de mensagens temporárias já estão cobertos; conferir antes do deploy.
- **Zoom no iPhone**: campo com menos de 16 px dá zoom. Por isso a fonte global de campo sobe para 16 px.
- **Tailwind**: classes novas vindas de JSON só entram no build porque `tailwind.config.js` já lê `especificacao/front/**/*.json`. Não mover esses arquivos.
- **Tela de criar cliente** sem desenho próprio: a auditoria do `diasabatico` registra a lacuna em vez de inventar layout.
- **Rota `**`**: link antigo ou errado passa a abrir a apresentação, não o login.
