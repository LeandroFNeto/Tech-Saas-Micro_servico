# Testes - aplicar-novo-design-front

Spec escrito: `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts` (3 cenarios, AuthService stub com jasmine.Spy, Router.navigateByUrl espionado, sem backend):
- sem sessao: renderiza, 2 links `a[href="/login"]` (topo e fim), sem navegacao
- com sessao: chama `rotaInicial()` e `navigateByUrl('/admin/empresas')`
- `a[href="#como"]` existe, `#como` existe, HTML sem `[CONTATO DA GAMB]`
Nenhum codigo de produto alterado; nada impediu o teste.

Comandos (resultado real):
- `npm run build` (frontend/): sucesso, bundle gerado (chunk apresentacao-component presente)
- `npm test -- --watch=false --browsers=ChromeHeadless`: TOTAL 4 SUCCESS (1 antigo + 3 novos)
- `./mvnw -q test` (raiz): exit 0, 157 testes nos surefire-reports, 0 falhas/erros (igual ao anterior)

Estado: aprovado

## Regressão visual

Caminho: `getComputedStyle`. O target `test` do `angular.json` já inclui `src/styles.css` e o `postcss.config.js` aplica o Tailwind, então o computed style é real. Para não depender de suposição, cada teste chama `garantirTailwindCarregado`, que falha se `text-xs` não resultar em 12 px.

Arquivos criados (sem alterar código de produto):
- `frontend/src/app/testing/campos-fonte.testing.ts` (medição e piso de 16 px)
- `frontend/src/app/features/auth/login/login.component.spec.ts`
- `frontend/src/app/features/admin/empresa-form/empresa-form.component.spec.ts` (cadastro novo e edição)
- `frontend/src/app/features/admin/gestao-avisos/gestao-avisos.component.spec.ts`
- `frontend/src/app/features/client/perfil/perfil.component.spec.ts` (abas dados, robo e configuracao)

O que conferem: todo `input` (exceto checkbox, radio, hidden, file), `select` e `textarea` renderizado tem fonte computada >= 16 px. Serviços em stub, sem backend. No empresa-form de edição confere também que `sessaoWhatsapp` e `googleCalendarId` existem e passam.

Resultado: `npm test -- --watch=false --browsers=ChromeHeadless` em `frontend/`: TOTAL 11 SUCCESS (4 antigos + 7 novos). Nenhum campo de produto com fonte < 16 px.

Prova de falha: troquei temporariamente `text-base` por `text-sm` no campo `sessaoWhatsapp` do `empresa-form.component.html`. Resultado: 2 FAILED (`cadastro novo` e `edicao`, "Expected $.length = 1 to equal 0", campo `input#sessaoWhatsapp` com 14 px). Arquivo revertido (idêntico ao backup) e suíte voltou a TOTAL 11 SUCCESS.
