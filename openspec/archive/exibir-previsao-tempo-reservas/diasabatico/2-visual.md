# Fase 2: só o visual (exibir-previsao-tempo-reservas)
## O que foi feito
- Canvas "Painel gamb — telas" (https://claude.ai/artifact/J891R7bxYLZtgv2LJsn47p, versão 25): o board ClientePerfil (e o do celular) virou `ClientePainel` e `ClienteChatbot`, cada um com a versão de celular 390 px. O painel tem o seletor de estados no Tweaks (normal, carregando, vazio, sem cidade, desligada, erro, salvando, sucesso, cidade não encontrada; WhatsApp conectado, aguardando ou desconectado; reservas indisponíveis).
- Angular: painel novo com dados de exemplo; perfil sem a conexão do WhatsApp, com "Voltar ao painel" e o título "Configuração do chatbot"; menu do cliente com "Painel" e "Configuração do chatbot" na barra lateral (desktop) e na barra de baixo (celular e tablet).
- Selo de risco: verde (baixo), âmbar (médio), vermelho (alto). Suave nos dias livres, cheio nos dias reservados, sempre com o texto "Risco ...". Dia reservado tem borda roxa e selo "Reservado". Cor sozinha não carrega a informação.
## Arquivos tocados
- novo: `frontend/src/app/features/client/painel/painel.component.{ts,html}`
- `frontend/src/app/features/client/perfil/perfil.component.html`; `perfil.component.ts` só ganhou `RouterLink` (o HTML exige)
- `frontend/src/app/shared/components/shell/shell.component.html`
- `especificacao/front/telas.design.json` (`tema.cores.risco`, `telas.clientePainel`, `clientePerfil.pagina.linkVoltar`)
- `frontend/tailwind.config.js` (uma linha: `risco` nas cores, para as classes `bg-risco-*`)
- novo: `especificacao/front/cliente-painel.content.json`; `cliente-perfil.content.json` (título, descrição e `voltar`)
- `especificacao/front/referencia-canvas/`: `ClientePainel.dc.html` e `ClienteChatbot.dc.html` entram, `ClientePerfil.dc.html` sai; tabela do README atualizada
## O que a fase 3 precisa saber
- Rotas: `client.routes.ts` com `''` levando a `PainelComponent` (`./painel/painel.component`) e `chatbot` levando a `PerfilComponent`. Os links já apontam para `/cliente` e `/cliente/chatbot`.
- `perfil.component.ts`: tirar `ConexaoWhatsappComponent` dos imports (aviso NG8113 hoje).
- `shell.component.ts`: o comentário de `abaInferior` diz "só admin"; agora o cliente também tem abas.
- Painel: `sessaoExemplo` é fixo; trocar por `AuthService.sessao()?.sessaoWhatsapp`, como o perfil faz. Os estados do cartão do WhatsApp vêm do próprio componente, não do seletor.
- `perfil.component.spec.ts` pode procurar "Meu perfil" ou o cartão do WhatsApp; ajustar.
## Lacunas para a fase 4
- Seletor "Só para visualização" no topo do painel (estado + reservas indisponíveis): sai quando a tela for ligada à API.
- Dados de exemplo: dias, cidade resolvida, "Atualizado às 14:05", o salvar da cidade. Mensagens de 429 e 503 já estão no content.json, ainda sem uso.
- Avisos (banner) seguem só na configuração do chatbot; levar ao painel pede o AvisoService.
## Verificação
- `npx tsc --noEmit -p frontend/tsconfig.app.json` sem erro. `ngc` com strictTemplates incluindo o painel: sem erro, só o aviso NG8113 acima. Build e testes ficam para a fase 4.
Estado: aprovado
