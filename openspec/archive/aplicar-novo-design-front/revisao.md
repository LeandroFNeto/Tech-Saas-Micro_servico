# Revisão - aplicar-novo-design-front (26/10/2026)

## Confirmado no código
- `app.routes.ts`: `''` (pathMatch full) carrega `ApresentacaoComponent` em lazy; `**` redireciona para `''`. `login`, `admin` e `cliente` e seus guards não mudaram.
- `apresentacao.component.ts`: com `auth.isAuthenticated()`, faz `navigateByUrl(auth.rotaInicial())`. O login mantém o mesmo redirecionamento.
- Logo: `logo-gamb.png` no shell, no login, na apresentação e no favicon de `index.html`. `logo-gamb.jpg` não existe mais em `frontend/public` nem em `frontend/src`.
- Textos novos: 401 "E-mail ou senha inválidos..." (`login.content.json`), "Enviar foto principal", "Enviar fotos", "Salvar alterações", "Use ao menos 6 caracteres.". Os textos antigos sumiram.
- `[CONTATO DA GAMB]` não aparece em `frontend/src` nem nos JSON de `especificacao/front`. Ele só fica no `.dc.html` de referência e no spec que proíbe o placeholder.
- Nenhum arquivo de `especificacao/back` mudou. O diff em `src/` é de outra tarefa.

## Comandos (rodei em `frontend/`)
- `npm test -- --watch=false --browsers=ChromeHeadless`: TOTAL 4 SUCCESS.
- `npm run build`: sucesso, bundle em `frontend/dist/frontend`.

## Gravado
- `openspec/specs/auth/spec.md`: AUTH-03 "Página pública de apresentação", depois do AUTH-02, com três cenários: sem sessão, com sessão e rota desconhecida.
- `openspec/specs/README.md`: a linha "Login e senha" agora cita `auth.feature` e `apresentacao.component.spec.ts`. As outras linhas não foram tocadas.

## Pendências para a pessoa
- O cenário "rota desconhecida" é garantido pela rota `**` em `app.routes.ts`, mas o spec do componente não o testa. Vale um teste de rota depois.
- O contato da gamb (`[CONTATO DA GAMB]`) segue fora do build até ser informado.
- Conferir `conversa-whatsapp.jpg` antes do deploy, porque a imagem é pública.
- A rota `design-v0` continua sem guard (fora desta change).
- Não foram vistos no navegador: 768 px, tabela com dados reais e QR Code.
- `tasks.md` está sem caixas marcadas. O deploy arquiva a pasta.

Estado: aprovado
