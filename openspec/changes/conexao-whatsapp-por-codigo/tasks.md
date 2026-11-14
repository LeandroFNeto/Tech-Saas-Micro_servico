# Tasks: Conexão do WhatsApp por código, passo a passo

Depende de `migrar-wppconnect-para-baileys` (rota `POST /whatsapp/{sessao}/codigo-pareamento`, tarefas 4.2b a 4.2d, e `INITIALIZING`, tarefa 4.2f). Começar o Angular só com o contrato YAML da rota pronto.

## 1. Design antes do código
- [ ] 1.1 Desenhar no canvas (skill `diasabatico`) os passos `inicio`, `telefone`, `codigo`, `qrcode`, `reconectando`, `conectado` e `repareamento`, em 375 px e desktop, versão normal e `compacto`
- [ ] 1.2 Registrar conteúdo e classes em `especificacao/front/` (`*.content.json` e `telas.design.json`) e obter a aprovação do canvas

## 2. Camada de dados do front
- [ ] 2.1 `whatsapp.models.ts`: status `INITIALIZING`, `CodigoPareamentoRequestDTO` e `CodigoPareamentoResponseDTO` iguais ao contrato YAML
- [ ] 2.2 `WhatsappConnectionService.solicitarCodigoPareamento(sessao, telefone)` chamando `POST /whatsapp/{sessao}/codigo-pareamento`, sem o telefone na URL nem em log

## 3. Componente
- [ ] 3.1 Máquina de estados dos passos (`inicio`, `telefone`, `codigo`, `qrcode`, `reconectando`, `conectado`, `repareamento`) no `conexao-whatsapp.component.ts`, mantendo o polling de 1,5 s e o `ignorarDesconectadoAte` também no passo do código
- [ ] 3.2 Passo `telefone`: máscara, validação de 12 ou 13 dígitos com 55 e telefone só em memória, limpo ao sair do passo
- [ ] 3.3 Passo `codigo`: código grande selecionável, botão copiar com "Copiado", instruções numeradas, "Gerar outro código" e mensagens de 400, 403, 409, 429 e 503
- [ ] 3.4 Passos `reconectando` e `repareamento` conforme WPP-22; QR Code como alternativa com link para voltar ao código
- [ ] 3.5 Versão `compacto` usada em `empresa-form` (admin), `painel` e `perfil` sem quebrar os usos atuais

## 4. Testes e verificação
- [ ] 4.1 `conexao-whatsapp.component.spec.ts`: um teste por cenário de WPP-21 e WPP-22; ajustar `painel.component.spec.ts`, `perfil.component.spec.ts` e `empresa-form.component.spec.ts` se dependerem do texto antigo
- [ ] 4.2 Rodar os testes do frontend e o lint com 100% de aprovação, e conferir a tela no navegador em 375 px e desktop
- [ ] 4.3 Homologar com a sessão de teste da Gamb: gerar o código, digitar no celular, ver `CONNECTED`, derrubar a rede do celular e ver "Reconectando"

## 5. Fechamento
- [ ] 5.1 Sincronizar `openspec/specs/whatsapp/spec.md` com WPP-21 e WPP-22 e arquivar a change
