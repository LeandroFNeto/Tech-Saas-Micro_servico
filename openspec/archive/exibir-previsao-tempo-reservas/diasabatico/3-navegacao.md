# Fase 3: troca de telas (exibir-previsao-tempo-reservas)
## O que foi feito
- `client.routes.ts`: dentro do ShellComponent, `''` carrega `PainelComponent` e `chatbot` carrega `PerfilComponent`. O guard do papel continua na rota pai `cliente` (app.routes.ts, sem mudança). Rota inexistente segue em `**`.
- `perfil.component.ts`: saiu o import de `ConexaoWhatsappComponent` (aviso NG8113 resolvido). Nada mais era só da conexão.
- `painel.component.ts`: `sessaoExemplo` virou `sessao = AuthService.sessao()?.sessaoWhatsapp ?? ''`; o HTML usa `[sessao]="sessao"`. Previsão segue com dados de exemplo.
- `shell.component.ts`: comentário de `abaInferior` corrigido (admin e cliente).
- Ida e volta conferida no código: menu lateral e barra do celular levam a `/cliente` e `/cliente/chatbot`; botão do painel leva a `/cliente/chatbot`; "Voltar ao painel" leva a `/cliente`.
## Arquivos tocados
- `frontend/src/app/features/client/client.routes.ts`
- `frontend/src/app/features/client/perfil/perfil.component.ts` e `perfil.component.spec.ts` (sem mock da conexão; novo teste do link "Voltar ao painel" e da ausência de `app-conexao-whatsapp`)
- `frontend/src/app/features/client/painel/painel.component.ts`, `painel.component.html` (uma troca de binding)
- novo: `frontend/src/app/features/client/painel/painel.component.spec.ts` (sessão do login, botão para `/cliente/chatbot`, conexão do WhatsApp, atribuição OpenWeather com cartões)
- `frontend/src/app/shared/components/shell/shell.component.ts` (só o comentário)
## O que a fase 4 precisa saber
- Clique real no navegador não foi feito; a ida e volta foi verificada por leitura de rotas, links e specs.
- Seletor "Só para visualização", dados de exemplo da previsão e avisos fora do painel seguem como lacunas já listadas na fase 2.
- Painel monta `app-conexao-whatsapp`, que chama a API de status de verdade (WhatsappConnectionService já existente, sem serviço novo).
## Resultado
- `npm run build`: ok, sem aviso NG8113; chunks lazy `painel-component` e `perfil-component`.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 16 de 16 passaram.
Estado: aprovado
