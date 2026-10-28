# Qualidade - aplicar-novo-design-front (26/10/2026)

| Item | Resultado |
|---|---|
| Rota `''` lazy -> ApresentacaoComponent; `**` -> `''` (`app.routes.ts`) | OK |
| Sessão válida em `/` vai a `rotaInicial()` (construtor da apresentação) | OK |
| `accent-800 #9a4508` em `telas.design.json`; usado em chips/abas | OK |
| `.switch`, `.chip` em `styles.css`; `.input-field` com `min-h-12 text-base` | OK |
| `.wordmark` definida e usada (shell); login/apresentação usam a classe via JSON | OK |
| Logo PNG no shell, login, apresentação e `index.html` (`image/png`); sem `logo-gamb.jpg` em `frontend/src`, `public`, `index.html` | OK |
| `[CONTATO DA GAMB]` fora do código e dos JSON (só no `.dc.html` de referência e no spec que o proíbe) | OK |
| 5 textos: 401 novo, "Enviar foto principal", "Enviar fotos", "Salvar alterações", "Use ao menos 6 caracteres." (textos antigos não existem mais) | OK |
| Campos Sessão/Agenda do cadastro agora `text-base` (A2 da auditoria corrigido) | OK |
| Sem DTO/rota de API/YAML novos; `especificacao/back` e `swagger.yaml` intactos | OK |
| `.ts` alterados: só campos visuais (`mostrarSenha`, `corTipo`, `compacto`, `NgTemplateOutlet`, imports de JSON/RouterLink), troca da mensagem do 401 e rotas; nenhuma chamada HTTP, serviço, guard ou validação mudou | OK |
| `apresentacao.component.spec.ts`: Entrar -> `/login` (2 links), sessão -> `navigateByUrl`, âncora `#como`, sem placeholder | OK |
| `npm test -- --watch=false --browsers=ChromeHeadless` (rodei): TOTAL 4 SUCCESS | OK |
| `npm run build` (rodei): sucesso, chunk lazy da apresentação | OK |

## Escopo
`src/main/java/.../LocacaoStrategy.java`, `LocacaoStrategyTest.java`, `openspec/specs/whatsapp/spec.md` e demais itens de back no `git status` são de OUTRA tarefa; não avaliei. Nada em `src/` ou `especificacao/back` vem desta change.

## Achados
- Reprova: nenhum.
- Ajuste 1: `apresentacao.component.spec.ts` não prova o redirecionamento do `**` nem do `login` com sessão; aceitável (tasks.md pede só Entrar e sessão).
- Ajuste 2: `lacunas-front.md` (d) diz que o spec "não existe" e a auditoria (A4) também; o spec já existe. Relatório desatualizado, sem efeito no código. `lacunas-front.md` também diz "Apresentação sem teste automático".
- Ajuste 3: o relatório da fase 2A diz que a apresentação "não manda quem tem sessão"; a fase 3 corrigiu. Coerente.
- Ajuste 4 (já registrado, segue): `design-v0` sem guard; tela de criar cliente sem desenho; 768 px, tabela com dados e QR Code não vistos no navegador; `conversa-whatsapp.jpg` conferir antes do deploy; `docs/logo-gamb-antiga.jpg` e `docs/gerar-readme-hero.py` entram no commit (a `producao` leva só `frontend/` e `vercel.json`).
- Ajuste 5: `tasks.md` ainda sem caixas marcadas (Revisão/Deploy marcam).

## AUTH-03 (texto para a Revisão; proposal/tasks bastam)
### AUTH-03 Página pública de apresentação

O sistema deve exibir em `/` uma página pública que explica o gamb (conceito, três passos e benefícios) e tem o botão Entrar para `/login`. Quem já tem sessão válida vai direto ao painel do seu papel. Rota desconhecida abre `/`. Não consome API nem exibe contato de produção sem confirmação.

Contrato: nenhum
Prova: `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts`

Cenário: visitante sem sessão

- Dado um visitante sem sessão
- Quando abre `/`
- Então vê a apresentação
- E o botão Entrar leva a `/login`

Cenário: usuário com sessão válida

- Dado um usuário autenticado
- Quando abre `/`
- Então é levado ao painel do seu papel

Estado: aprovado
