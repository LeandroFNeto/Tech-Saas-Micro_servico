# Tasks

Regra de testes: toda função criada ou alterada tem teste, e todo bug achado ganha um teste de regressão escrito antes da correção. Nenhum comentário no código; nenhum dado pessoal no log (`.cursor/rules/seguranca.mdc`).

## 0. Primeiro passo: responder pelo mesmo endereço (6.7.24)
- [x] 0.1 Teste de regressão: cliente do `@lid` com número conhecido recebe a resposta no `@lid`; número sem conversa por `@lid`, `@lid` sem número e número sem o nono dígito (`baileys-gateway/test/sessao.test.ts`); os três testes principais falham sem a correção
- [x] 0.2 `src/sessao.ts`: guardar `número → @lid` ao tratar a mensagem (do cliente e do dono) e usar em `resolverDestinoDeNumero`; verificação: `npm test` com 155 testes e `typecheck` sem erro
- [ ] 0.3 Deploy e homologação: com um número de teste, dez turnos nos dois sentidos sem "Aguardando mensagem" e sem `Bad MAC` no log (WPP-25). Se fechar, as seções 1 a 5 abaixo são dispensadas e a change segue para o fechamento

Seções 1 a 5: segundo passo, só se a tarefa 0.3 falhar.

## 1. Preparar a volta
- [ ] 1.1 No servidor, copiar o volume `baileys-auth` para fora do container (`docker run --rm -v gamb_baileys-auth:/dados -v "$PWD":/copia alpine tar czf /copia/baileys-auth-6.7.24.tgz -C /dados .`) e conferir que o arquivo existe e não está vazio
- [ ] 1.2 Registrar no `design.md` (Migration Plan) o nome real do volume do servidor, conferido com `docker volume ls`

## 2. Dependência e armazenamento de chaves
- [ ] 2.1 Fixar `@whiskeysockets/baileys` em `7.0.0-rc14` em `baileys-gateway/package.json` e atualizar o `package-lock.json`; verificação: `npm ci` e `npm run typecheck` rodam, e os erros de tipo que a troca causar ficam listados
- [ ] 2.2 `src/auth-state.ts`: trocar `proto.Message.AppStateSyncKeyData.fromObject` por `.create`; verificação: teste de leitura de `app-state-sync-key` gravada pelo 6.7.24
- [ ] 2.3 Testes do `auth-state` para as categorias `lid-mapping`, `device-list` e `tctoken` (gravar, ler, remover, id com `:` e `_`) e para a leitura de arquivos gravados pelo 6.7.24

## 3. Gateway
- [ ] 3.1 `src/fabrica-baileys.ts` e `src/sessao.ts`: ajustar às mudanças de API usadas (`onWhatsApp` sem LID, `isJidUser` ausente, ACKs) mantendo o cache de `getMessage`; verificação: `typecheck` limpo e testes existentes verdes
- [ ] 3.2 Resolução PN/LID: ordem `remoteJidAlt`, depois `lidMapping.getPNForLID`, depois `paresLid`, usando o evento `lid-mapping.update` para alimentar o mapa; teste com socket falso no formato novo, com e sem número resolvido
- [ ] 3.3 `src/mensagem.ts`: teste de regressão do contato `@lid` sem número resolvido (não vai ao Java com `@lid`) e com número resolvido (`from` com o número)
- [ ] 3.4 Teste de que o log do gateway segue sem número completo nem texto nos caminhos novos (WPP-06, WPP-21)

## 4. Verificação automatizada
- [ ] 4.1 Rodar `npm test` e `npm run typecheck` no gateway com 100% de aprovação e nenhum teste ignorado
- [ ] 4.2 Rodar os testes do Java que tocam o webhook (`WhatsappWebhookDTOTest` e os de `ControllerWhatsapp`) para provar que o contrato não mudou

## 5. Homologação no servidor
- [ ] 5.1 Deploy pelo workflow; conferir `docker compose ps` e `Sessoes restauradas no boot` com `restauradas` maior que 0 (WPP-26)
- [ ] 5.2 Com dois números de teste de tipos diferentes, conversar dez turnos seguidos nos dois sentidos e conferir: sem "Aguardando mensagem" em nenhum lado, resposta do bot a cada mensagem e nenhum `Bad MAC` nem `failed to decrypt` no log (WPP-25)
- [ ] 5.3 Provar a volta: restaurar a cópia do volume em ambiente de teste, ou documentar o resultado do repareamento, e registrar o tempo
- [ ] 5.4 Só então liberar o número aos clientes; se a homologação falhar, registrar o resultado e abrir change para a próxima causa

## 6. Fechamento
- [ ] 6.1 Sincronizar a spec (`openspec-sync-specs`), arquivar a change e atualizar `openspec/central-tarefas.md`
