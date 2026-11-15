# Tasks: Conexão do WhatsApp por código, passo a passo

Depende de `migrar-wppconnect-para-baileys` (rota `POST /whatsapp/{sessao}/codigo-pareamento`, tarefas 4.2b a 4.2d, e `INITIALIZING`, tarefa 4.2f). Começar o Angular só com o contrato YAML pronto.

Regra de testes (design, seção 8): toda função criada tem teste, e todo bug achado ganha um teste de regressão escrito antes da correção e registrado em "Registro de bugs" no fim deste arquivo.

## 1. Design antes do código
- [x] 1.1 Decidir o `compacto` do `painel`: opção A, mesmos passos em layout reduzido (design, seção 6)
- [x] 1.2 Desenhar no canvas (skill `diasabatico`) os passos `inicio`, `telefone`, `codigo`, `codigo-expirado`, `qrcode`, `conectando`, `reconectando`, `conectado` e `repareamento`, em 375 px e desktop, na versão normal e na `compacto` com os mesmos passos, e o chip de WhatsApp do `dashboard` com `INITIALIZING`
- [x] 1.3 Registrar conteúdo e classes em `especificacao/front/` (`*.content.json` e `telas.design.json`) e obter a aprovação do canvas

## 2. Backend e gateway: `sessaoEncerrada`
- [x] 2.1 Contrato primeiro: campo `sessaoEncerrada` (boolean, padrão falso) em `especificacao/back/schemas/whatsapp/SessaoStatusResponseDTO.yaml` e nos paths de status e iniciar
- [x] 2.2 Gateway: gravar `pareadaEm` e `encerradaEm` em `meta.json` (`loggedOut`, `badSession`, `multideviceMismatch` marcam `encerradaEm`; novo pareamento limpa; `close-session` apaga a pasta), e `status-session` e `start-session` devolverem `sessaoEncerrada` só com `DISCONNECTED` e marca de encerramento
- [x] 2.3 Java: `SessaoStatusResponseDTO` e `ServicoWppConnect` repassam `sessaoEncerrada`; campo ausente vira falso (gateway antigo e WPPConnect)
- [x] 2.4 Testes das tarefas 2.2 e 2.3: marca gravada em cada motivo, limpa no novo pareamento, ausente após `close-session`, falsa com outro status, campo ausente no Java

## 3. Camada de dados do front
- [x] 3.1 `whatsapp.models.ts`: status `INITIALIZING`, `sessaoEncerrada` opcional, `CodigoPareamentoRequestDTO` e `CodigoPareamentoResponseDTO` iguais ao contrato YAML
- [x] 3.2 `WhatsappConnectionService.solicitarCodigoPareamento(sessao, telefone)` chamando `POST /whatsapp/{sessao}/codigo-pareamento`, sem o telefone na URL nem em log
- [x] 3.3 Testes do serviço: caminho feliz, cada erro (400, 401, 403, 404, 409, 429, 503) e telefone fora da URL

## 4. Componente e telas
- [x] 4.1 Máquina de estados dos passos do design (seção 1) no `conexao-whatsapp.component.ts`, mantendo o polling de 1,5 s e a guarda de `DISCONNECTED` logo após iniciar também no passo `codigo`; `repareamento` só por `sessaoEncerrada`
- [x] 4.2 Passo `telefone`: aceita colar com símbolos, máscara, validação de 12 ou 13 dígitos com 55, dica do número do celular, telefone só em memória, limpo ao sair do passo
- [x] 4.3 Passo `codigo`: código grande selecionável com foco ao aparecer, botão copiar com "Copiado", contagem regressiva com a constante `VALIDADE_CODIGO_SEGUNDOS`, instruções numeradas, "Gerar outro código" e passo `codigo-expirado`
- [x] 4.4 Mensagens e saídas de 400, 401, 403, 404, 429 e 503, e o tratamento do 409 em dois significados (consulta o status; até 3 repetições a cada 2 s; depois "Tentar de novo")
- [x] 4.5 Passos `conectando`, `reconectando` e `repareamento` conforme WPP-22, e QR Code como alternativa com link para voltar ao código
- [x] 4.6 Casos de UX do design (seção 5): aba em segundo plano (polling lento e consulta ao voltar), navegador sem internet, botão desabilitado contra clique duplo, troca de empresa limpa o estado, página recarregada, acessibilidade (foco, rótulos, contraste, alvo de toque de 44 px)
- [x] 4.7 Aplicar a opção A (mesmos passos, layout reduzido) ao `compacto` do `painel` e manter `empresa-form` na versão normal, sem quebrar os usos atuais
- [x] 4.8 `features/admin/dashboard/dashboard.component.html`: o chip de WhatsApp mostra "Reconectando" para `INITIALIZING` em vez de "Consultando..."

## 5. Testes e verificação
- [x] 5.1 `conexao-whatsapp.component.spec.ts`: um teste por cenário de WPP-24 e WPP-22 e um por caso de UX da seção 5 do design; ajustar `painel.component.spec.ts`, `empresa-form.component.spec.ts` e o spec do `dashboard` se dependerem do texto antigo
- [ ] 5.2 Rodar os testes do frontend, do Java e do gateway e o lint com 100% de aprovação e nenhum teste ignorado, e conferir a tela no navegador em 375 px e desktop, inclusive os alvos de toque de 44 px (o teste unitário só confere a classe)
- [ ] 5.3 Homologar com a sessão de teste da Gamb: gerar o código, medir quanto tempo o código vale de fato e ajustar `VALIDADE_CODIGO_SEGUNDOS`, digitar no celular, ver "Conectando…" e depois `CONNECTED`
- [ ] 5.4 Homologar a reconexão reiniciando o container do gateway (`docker restart baileys_gateway`) e vendo "Reconectando…" voltar sozinho a `CONNECTED`. Celular sem internet por dias não é testado aqui: pertence à change `alertas-desconexao-whatsapp`
- [ ] 5.5 Homologar o `repareamento`: desvincular o aparelho pelo celular e ver "A conexão foi encerrada. Conecte de novo" mesmo depois de recarregar a página

## 6. Fechamento
- [ ] 6.1 Conferir que todo bug do "Registro de bugs" tem teste de regressão passando
- [ ] 6.2 Só depois de arquivada a `migrar-wppconnect-para-baileys` (WPP-17 a WPP-20 sincronizados), converter o delta para o formato da spec principal (`### WPP-NN`, "O sistema deve", Contrato/Prova, Dado/Quando/Então/E) e sincronizar `openspec/specs/whatsapp/spec.md` com WPP-24 e WPP-22 e arquivar a change

## Registro de bugs

Cada bug achado em qualidade, segurança, revisão ou homologação entra aqui com o teste de regressão que o cobre, escrito antes da correção.

| # | Bug | Etapa em que foi achado | Teste de regressão | Data |
|---|---|---|---|---|
| 1 | Gravação de `pareadaEm` ainda em andamento quando `loggedOut` apagava a pasta da sessão: `rm` falhava com ENOTEMPTY e a marca `encerradaEm` não era gravada | Programação (2.2) | `baileys-gateway/test/sessao.test.ts`: `sessaoEncerrada no gateway (WPP-22) > regressao: gravação de pareadaEm em andamento não impede encerradaEm quando loggedOut apaga a pasta` | 2026-11-14 |
| 2 | 400 ao gerar outro código nos passos `codigo` e `codigo-expirado` ficava silencioso | Qualidade (volta 1) | `conexao-whatsapp.component.spec.ts`: `400 ao gerar outro código no passo do código mostra a mensagem de número` e `... no passo do código expirado ...` | 2026-11-14 |
| 3 | Resposta tardia do pedido de código sobrescrevia o passo do QR Code ou `conectado` | Revisão de código | `conexao-whatsapp.component.spec.ts`: `regressao: resposta tardia do código não sobrescreve o passo do QR Code` e `... não sobrescreve conectado` | 2026-11-15 |
| 4 | Guarda de 45 s menor que a validade de 120 s do código tirava a pessoa do passo do código, e `INITIALIZING` não a renovava em `conectando` | Revisão de código | `regressao: DISCONNECTED passageiro depois de 45 s ...`, `... não apaga o passo de código expirado`, `... INITIALIZING renova a guarda do passo conectando` | 2026-11-15 |
| 5 | Gateway fora do ar depois de conectado virava início com botões de conectar em vez de "Reconectando…" | Revisão de código | `regressao: gateway fora do ar depois de conectado mostra Reconectando ...` e os dois testes de `sessaoEncerrada` e guarda esgotada | 2026-11-15 |
| 6 | Após 403/404 o polling parava para sempre e conectar não o retomava | Revisão de código | `regressao: depois de 403 ao consultar, iniciar uma conexão retoma o polling` | 2026-11-15 |
