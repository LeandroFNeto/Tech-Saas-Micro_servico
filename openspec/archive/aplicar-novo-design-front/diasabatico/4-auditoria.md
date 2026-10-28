# Fase 4: auditoria (terminal 4, 26/10/2026)

Só leitura e medição. Único arquivo gravado: este.

## Build e testes
- `npm run build` em `frontend/`: sem erro e sem aviso. Chunk lazy `apresentacao-component` (6,42 kB).
- `npm test -- --watch=false --browsers=ChromeHeadless`: Chrome Headless 156, 1 de 1 SUCCESS. A suíte tem só o `app.component.spec.ts`.

## Itens
| # | Item | Resultado |
|---|---|---|
| 1 | Disponível/alcançável | OK. `/`, `/login`, `/admin`, `/admin/empresas/nova`, `/admin/empresas/:sessao`, `/admin/avisos`, `/cliente` abrem sem erro de tela. Entrar, "← Conhecer o gamb", abas do admin e Sair existem. Com sessão falsa, `/` foi direto a `/cliente` |
| 2 | Fidelidade ao desenho | Parcial. Aparência conferida no navegador (marca, faixa, cartões, abas, chips, `.switch`). Comparação linha a linha com cada `.dc.html`: não feita |
| 3 | Estados | OK com ressalva (A1) |
| 4 | Texto | OK. `[CONTATO DA GAMB]` ausente do código e dos JSON. 401, "Enviar foto principal", "Enviar fotos", "Salvar alterações" e a dica de 6 caracteres presentes. Admin, banner e conexão seguem com texto no HTML, como o `design.md` prevê |
| 5 | Estilo/tokens | OK. Sem hexadecimal solto nos templates; `accent-800` existe |
| 6 | Responsivo | OK com ressalva (A2, A3) |
| 7 | Acesso | OK. Nenhum campo sem rótulo, imagem sem `alt` ou botão sem nome nas telas medidas |
| 8 | Lógica intacta | OK. O diff dos `.ts` traz só campos visuais (`mostrarSenha`, `corTipo`, `compacto`, `NgTemplateOutlet`, classes) e as rotas previstas. Nada em serviços, guards, interceptors, validações ou HTTP |
| 9 | Dado vs DTO | Não verificado (não cruzei os campos com `especificacao/back/`) |
| 10 | Riscos | Ver abaixo |

## Achados
Bloqueia: nenhum.

Ajuste (pode seguir):
- A1. `dashboard.component.html:9-22`: com erro de API, a faixa de erro e "Nenhum cliente cadastrado ainda." aparecem juntos. O comportamento já era assim antes da change.
- A2. `empresa-form.component.html:43` e `:53` (`empresa-sessao`, `empresa-agenda`): `text-sm` deixa a fonte em 14 px, abaixo dos 16 px prometidos; o iPhone dá zoom. Trocar por `text-base`.
- A3. O `.switch` mede 48x28 (menos de 44 px de altura). O desenho e o `design.md` pedem 48x28, e a linha inteira com rótulo é maior. Aceitável. Rótulos de 11 px (cabeçalho dos cartões do dashboard) são pequenos, mas não são campo.
- A4. Falta `apresentacao.component.spec.ts`, previsto em `tasks.md` (Prova). O teste de navegação (Entrar leva a `/login`; sessão leva ao painel) não existe. Volta à programação antes da entrega (fase 5), sem barrar o visual.
- A5. `design-v0` segue aberta e sem guard (fora da change, já registrada).
- A6. Tela de criar cliente sem desenho próprio (lacuna já registrada).

## Riscos
- `conversa-whatsapp.jpg`, conferida olhando o arquivo: nome do contato, foto e nome da empresa estão cobertos. A linha de mensagens temporárias está coberta só na primeira parte; o resto do texto fica visível e é inofensivo.
- `docs/gerar-readme-hero.py:15` aponta para `docs/logo-gamb-antiga.jpg`, que existe (arquivo novo, ainda não commitado).
- Nenhum `logo-gamb.jpg` restante em `frontend/src`.

## Navegador (browser embutido, `ng serve` na 4200)
- Medi com JavaScript (rolagem da página, alvo de toque, fonte dos campos, rótulos, `alt`) e vi capturas. A captura saiu pequena e só preenchendo parte da tela (limite da emulação); por isso me apoiei nas medidas.
- 375 px: `/`, `/login`, `/admin`, `/admin/empresas/nova`, `/admin/empresas/fake`, `/admin/avisos`, `/cliente`. Nenhuma tem rolagem horizontal da página (`scrollWidth` = 375). Nenhum botão ou link com menos de 44 px. Barra de abas fixa embaixo no admin (61 px, sem cobrir o conteúdo). Botão Entrar no canto da apresentação. As abas do perfil rolam dentro do próprio seletor.
- 1280 px: as mesmas telas, mais `/cliente`. Sem rolagem horizontal. Barra lateral no painel (admin e cliente).
- Painéis abertos com JWT falso em `localStorage` (`gamb.jwt`, `gamb.sessao`, papel ADMIN e depois CLIENTE). As chamadas a `/api` falharam (esperado), então vi só estado de erro e vazio, não listas com dados nem estado de sucesso.
- Não vi: 768 px, tabela de empresas com dados (a partir de 1100 px), foco por teclado, interruptores ligando, QR Code.
- Limpeza feita: `localStorage` falso apagado, `ng serve` encerrado, emulação de tela desfeita.

Estado: aprovado
