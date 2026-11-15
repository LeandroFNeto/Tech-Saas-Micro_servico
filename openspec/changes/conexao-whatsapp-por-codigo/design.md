# Design: Conexão guiada por código de telefone

## Context

O componente `ConexaoWhatsappComponent` (`frontend/src/app/shared/components/conexao-whatsapp/`) faz polling de `GET /whatsapp/{sessao}/status` a cada 1,5 s, dispara `POST /whatsapp/{sessao}/iniciar` e mostra o QR Code (`status` `QRCODE`). Tem os estados `CONNECTED`, `QRCODE` e `DISCONNECTED`, e usava `ignorarDesconectadoAte` (45 s) para não piscar `DISCONNECTED` logo depois de iniciar. Aparece em dois lugares: `painel` do cliente (`[compacto]="true"`) e `empresa-form` do admin (versão normal). O `perfil` não usa o componente. O chip de WhatsApp do `dashboard` do admin lê o status por conta própria e hoje mostra "Consultando..." para `INITIALIZING`.

A change `migrar-wppconnect-para-baileys` entrega `POST /whatsapp/{sessao}/codigo-pareamento` (`CodigoPareamentoRequestDTO { telefone }`, `CodigoPareamentoResponseDTO { sessao, codigo }`), 400/403/409/429/503, e o status `INITIALIZING` repassado pelo Java. No gateway, o `409` tem três causas com a mesma mensagem no Java ("Sessão não está aguardando pareamento"): já conectada, ainda iniciando (sem primeiro QR) e fora do pareamento.

## Goals / Non-Goals

### Goals
- O cliente conecta só com o celular: número → código → digitar no WhatsApp → conectado.
- O QR Code segue disponível como alternativa.
- A tela mostra "conectando", "reconectando" e "pareie de novo" nos casos certos, com o "pareie de novo" decidido pelo servidor.
- A tela avisa quanto tempo o código vale e o que fazer quando expira.
- Todo código novo e todo bug achado têm teste.

### Non-Goals
- Avisar o cliente fora do painel (e-mail, push), inclusive o celular sem internet por dias: change `alertas-desconexao-whatsapp`.
- Mudar a rota do código, o `INITIALIZING` ou o gateway base: change `migrar-wppconnect-para-baileys`.
- Gravar o telefone do cliente.

## Decisions & Architecture

### 1. Fluxo em passos (máquina de estados da tela)

| Passo | Quando aparece | O que mostra |
|---|---|---|
| `inicio` | `DISCONNECTED` com `sessaoEncerrada` falso | Botão principal "Conectar com número de telefone" e link "Prefiro ler o QR Code" |
| `telefone` | cliente escolheu o código | Campo do número (aceita colar `+55 (11) 99999-9999`), máscara, validação de 12 ou 13 dígitos, dica "o número do celular onde está o WhatsApp, com o 9", botão "Gerar código" |
| `codigo` | API respondeu 200 | Código grande `ABCD-EFGH`, botão "Copiar", contagem "Vale por mais mm:ss", instruções numeradas, "Aguardando você digitar o código no celular…" e botão "Gerar outro código" |
| `codigo-expirado` | a contagem chegou a zero sem `CONNECTED` | "O código expirou" e o botão "Gerar outro código" em destaque; o QR Code segue como alternativa |
| `qrcode` | cliente escolheu o QR, ou o código está indisponível (503) | QR atual, como hoje, e link "Voltar para o código" |
| `conectando` | `INITIALIZING` logo depois de o cliente estar no passo `codigo` ou `qrcode` | "Conectando… confirmando com o WhatsApp" (o Baileys reinicia a conexão depois do pareamento) |
| `reconectando` | `INITIALIZING` sem passo de pareamento antes | "Reconectando… isso é automático, não precisa fazer nada" |
| `conectado` | `CONNECTED` | "WhatsApp online", como hoje |
| `repareamento` | `DISCONNECTED` com `sessaoEncerrada` verdadeiro | "A conexão foi encerrada. Conecte de novo" e o botão de conectar do passo `inicio` |

O polling de status segue (1,5 s) e é ele que muda a tela de `codigo` ou `qrcode` para `conectando` e `conectado`. A guarda `guardaAte` (constante `GUARDA_DESCONECTADO_MS`, 45 s) vale também para o passo `codigo`, para a tela não voltar a `inicio` enquanto o cliente digita o código; ele deixa de ser um estado falso `QRCODE` e passa a ser uma guarda explícita da máquina de estados.

### 2. Estado "já pareada e encerrada" vem do servidor
A tela não guarda memória de ter visto `CONNECTED`, porque ela se perde ao recarregar a página ou trocar de tela.
- `status-session` do gateway e `SessaoStatusResponseDTO` do Java ganham `sessaoEncerrada` (booleano, padrão falso).
- O gateway grava em `meta.json` a marca `pareadaEm` quando a sessão é pareada e `encerradaEm` quando ela perde o pareamento (`loggedOut`, `badSession`, `multideviceMismatch`). Um novo pareamento limpa `encerradaEm`. `close-session` (exclusão da empresa) apaga a pasta inteira e, portanto, não deixa marca.
- `sessaoEncerrada` é verdadeiro só quando o status é `DISCONNECTED` e existe `encerradaEm` sem novo pareamento. Em qualquer outro status é falso.
- Gateway antigo ou WPPConnect não mandam o campo: a tela trata a ausência como falso e mostra `inicio`.

### 3. Tratamento do código
- A chamada só acontece depois de `POST /whatsapp/{sessao}/iniciar` (a sessão precisa estar aguardando pareamento). A tela faz as duas em sequência ao clicar em "Gerar código".
- O telefone vive só no signal do componente, enquanto o formulário está aberto; não vai para URL, `localStorage`, log ou analytics. Ao sair do passo `telefone` o campo é limpo.
- **Validade do código**: a contagem usa uma constante única `VALIDADE_CODIGO_SEGUNDOS`, com valor inicial de 120 s por estimativa. O prazo real do WhatsApp é medido na homologação (tarefa 5.3) e a constante é ajustada para um valor um pouco menor que o medido. Ao zerar, a tela vai para `codigo-expirado`. O código vale 1 por vez: "Gerar outro código" substitui o anterior e reinicia a contagem.
- "Gerar outro código" repete a chamada; o 429 (mais de 3 em 10 min) mostra "Muitos pedidos. Aguarde alguns minutos ou use o QR Code".
- O código é texto selecionável (`user-select: all`), tem `aria-live="polite"` e recebe o foco ao aparecer. O botão copiar usa `navigator.clipboard` com a mensagem "Copiado". O código nunca vai para log, `localStorage` nem título da aba.

### 4. Mensagens de erro (nunca o texto cru da API)
| Código | Mensagem | Saída |
|---|---|---|
| 400 | "Confira o número: DDD e número, com 12 ou 13 dígitos contando o 55." | fica no passo `telefone` |
| 401 | (interceptor leva ao login) | a tela limpa telefone, código e contagem |
| 403 | "Você não tem acesso a esta conexão." | passo `inicio` |
| 404 | "Não encontramos a conexão desta empresa." | passo `inicio` (admin) |
| 409 | ver abaixo | ver abaixo |
| 429 | mensagem acima | oferece o QR Code |
| 503 | "Conexão por código indisponível agora." | abre o passo `qrcode` |

**409 tem dois significados.** Como o Java devolve o mesmo texto, a tela decide pelo status: ao receber 409, consulta `GET /status`. Se vier `CONNECTED`, vai a `conectado`. Se não, é "a conexão ainda está iniciando": a tela repete o pedido sozinha até 3 vezes, a cada 2 s, mostrando "Preparando a conexão…". Se continuar 409, mostra "A conexão ainda está iniciando. Tente de novo em alguns segundos" com o botão "Tentar de novo". A repetição automática não conta como novo clique do cliente.

**Decisões de implementação registradas na volta 1.**
- Depois de `CONNECTED` o polling continua, em ritmo lento (5 s, `INTERVALO_STATUS_LENTO_MS`), para a tela perceber `INITIALIZING` e a queda. O mesmo ritmo vale com a aba em segundo plano.
- Um 409 devolvido por `POST /iniciar` (e não pelo pedido de código) não tem tratamento próprio: cai no erro genérico ("Não foi possível concluir agora"). O desdobramento em dois significados vale só para `POST /codigo-pareamento`.
- Um 400 em "Gerar outro código" (passos `codigo` e `codigo-expirado`) mostra a mensagem de 400 em aviso no topo do cartão; no passo `telefone` ela fica no campo.

### 5. Casos de experiência de usuário
| Caso | Comportamento |
|---|---|
| Cliente digita o código e a tela vira `INITIALIZING` | passo `conectando` (texto próprio), nunca "Reconectando…" |
| Código expira | passo `codigo-expirado` com "Gerar outro código" |
| Aba em segundo plano | o polling continua devagar (5 s) e, ao voltar à aba (`visibilitychange`), consulta o status na hora |
| Navegador sem internet | "Sem internet neste aparelho. Vamos tentar de novo quando voltar." e o polling pausa até `online` |
| Clique duplo em "Gerar código" | botão desabilitado enquanto a chamada está em andamento; o segundo clique não consome o limite de 3 pedidos |
| Admin troca a empresa no formulário (`sessao` muda) | a tela volta a `inicio`, e limpa telefone, código e contagem |
| Sessão do painel expira (401) | o interceptor leva ao login; nada fica na tela |
| Página recarregada com o código na tela | o código se perde por segurança; a tela mostra o status do servidor (`QRCODE` mostra `inicio`) e oferece gerar outro código |
| Cliente digitou o código no celular mas o WhatsApp recusou | a sessão segue aguardando; a contagem corre e o cliente pode gerar outro código |
| Número digitado diferente do número do celular | dica no campo e nas instruções: tem de ser o número do celular onde está o WhatsApp |
| Leitor de tela e teclado | foco no código ao aparecer, rótulos nos campos e botões, contraste e alvo de toque de pelo menos 44 px, ordem de tabulação lógica |
| `INITIALIZING` enquanto a pessoa digita o telefone | a tela mantém o passo `telefone` e o que foi digitado, sem ir a `reconectando` |
| Telefone colado com símbolos | a tela remove tudo que não for dígito antes de validar |

### 6. Telas e versões do componente
- `painel` (cliente) usa `[compacto]="true"`; `empresa-form` (admin) usa a versão normal.
- O chip de WhatsApp do `dashboard` do admin passa a tratar `INITIALIZING` como "Reconectando" em vez de "Consultando...".
- **Decisão (opção A)**: o `compacto` do `painel` mostra os mesmos passos do fluxo, em layout reduzido e com textos mais curtos. Sem modal nem botão intermediário, e o `painel` mantém `[compacto]="true"`. A versão normal do `empresa-form` segue igual.

### 6b. Cor dos estados em andamento
`conectando` e `reconectando` usam azul com ponto pulsando (escolha da pessoa), para não se confundir com o roxo de `DISCONNECTED`. A animação respeita `prefers-reduced-motion`. Canvas: https://claude.ai/artifact/J891R7bxYLZtgv2LJsn47p, grupo "Conexão WhatsApp por código".

As chaves `chip.andamentoRoxo` (`telas.conexaoWhatsapp`) e `chipWhatsapp.reconectandoRoxo` (`telas.adminDashboard`) ficam no `telas.design.json` como opções reservadas do roxo, sem uso no HTML.

### 7. Processo de design
Antes do Angular, o layout dos passos é desenhado no canvas (skill `diasabatico`) em pelo menos mobile (375 px) e desktop, com os estados da tabela acima e a versão `compacto` com os mesmos passos (decisão da seção 6), e registrado em `especificacao/front/` (conteúdo e classes, como nos demais `*.content.json` e `telas.design.json`). Só depois o componente é alterado.

### 8. Regra de testes
- **Toda função criada tem teste**: cada método novo do componente, do serviço Angular, do Java (`ServicoWppConnect`, `ControllerWhatsapp`) e do gateway (`sessao.ts`, `gerenciador.ts`, `rotas.ts`) tem pelo menos um teste do caminho feliz e um de cada ramo de erro.
- **Todo bug achado ganha teste de regressão**: o teste é escrito antes da correção, falha pelo motivo do bug e passa depois dela. Leva o nome `regressao: <descrição curta>`. O bug entra no "Registro de bugs" do `tasks.md` com o teste que o cobre, a etapa em que foi achado e a data.
- **Um teste por cenário de spec**: cada cenário de WPP-24 e WPP-22 tem pelo menos um teste citado na linha "Prova" do requisito.
- **Casos de UX da seção 5** têm cada um um teste do componente.
- O fechamento só acontece com testes do frontend, do Java e do gateway em 100% e sem teste ignorado.

### 9. Compatibilidade com a reversão para o WPPConnect
Sem a rota de código (503), o passo `qrcode` aparece direto e o comportamento atual é preservado. Sem `sessaoEncerrada`, a tela mostra `inicio` no lugar de `repareamento`.

## Risks / Trade-offs

- [Cliente digita o código errado ou deixa expirar] → contagem, `codigo-expirado` e "Gerar outro código", com o limite de 3 em 10 min mostrado como 429.
- [Prazo real do código diferente do estimado] → constante única ajustada na homologação (tarefa 5.3).
- [Cliente no celular precisa trocar de app para copiar o código] → botão de copiar e código grande selecionável.
- [Status `INITIALIZING` chega do Java só depois da change da migração] → o tipo novo é opcional no front; sem ele, `INITIALIZING` chega como `QRCODE` e a tela mostra o QR como hoje.
- [Telefone e código no navegador] → só em memória do componente, limpos ao sair do passo, ao trocar de empresa e no 401.
- [Contrato novo `sessaoEncerrada` toca a change da migração, em homologação] → implementar depois de a migração ser arquivada, ou numa branch própria, sem mudar o que a migração já prova.
