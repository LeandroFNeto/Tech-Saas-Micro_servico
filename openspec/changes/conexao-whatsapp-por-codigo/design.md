# Design: Conexão guiada por código de telefone

## Context

O componente `ConexaoWhatsappComponent` (`frontend/src/app/shared/components/conexao-whatsapp/`) faz polling de `GET /whatsapp/{sessao}/status` a cada 1,5 s, dispara `POST /whatsapp/{sessao}/iniciar` e mostra o QR Code (`status` `QRCODE`). Tem os estados `CONNECTED`, `QRCODE` e `DISCONNECTED`, e usa `ignorarDesconectadoAte` (45 s) para não piscar `DISCONNECTED` logo depois de iniciar. É usado em `painel` e `perfil` do cliente e em `empresa-form` do admin.

A change `migrar-wppconnect-para-baileys` entrega `POST /whatsapp/{sessao}/codigo-pareamento` (`CodigoPareamentoRequestDTO { telefone }`, `CodigoPareamentoResponseDTO { sessao, codigo }`), 400/403/409/429/503, e o status `INITIALIZING` repassado pelo Java.

## Goals / Non-Goals

### Goals
- O cliente conecta só com o celular: número → código → digitar no WhatsApp → conectado.
- O QR Code segue disponível como alternativa.
- A tela mostra "reconectando" e "pareie de novo" nos casos certos.

### Non-Goals
- Avisar o cliente fora do painel (e-mail, push): change `alertas-desconexao-whatsapp`.
- Mudar rota, DTO ou gateway: change `migrar-wppconnect-para-baileys`.
- Gravar o telefone do cliente.

## Decisions & Architecture

### 1. Fluxo em passos (máquina de estados da tela)

| Passo | Quando aparece | O que mostra |
|---|---|---|
| `inicio` | status `DISCONNECTED` sem sessão pareada antes | Botão principal "Conectar com número de telefone" e link "Prefiro ler o QR Code" |
| `telefone` | cliente escolheu o código | Campo do número com máscara `+55 (DD) 9XXXX-XXXX`, validação de 12 ou 13 dígitos, botão "Gerar código" |
| `codigo` | API respondeu 200 | Código grande `ABCD-EFGH`, botão "Copiar", instruções numeradas, "Aguardando você digitar o código no celular…" e botão "Gerar outro código" |
| `qrcode` | cliente escolheu o QR, ou o código está indisponível (503) | QR atual, como hoje, e link "Voltar para o código" |
| `reconectando` | status `INITIALIZING` | "Reconectando… isso é automático, não precisa fazer nada" |
| `conectado` | status `CONNECTED` | "WhatsApp online", como hoje |
| `repareamento` | status `DISCONNECTED` depois de a tela ter visto `CONNECTED` ou `INITIALIZING` | "A conexão foi encerrada. Conecte de novo" e o botão de conectar do passo `inicio` |

O polling de status segue igual (1,5 s) e é ele que muda a tela de `codigo` ou `qrcode` para `conectado`. O sinal `ignorarDesconectadoAte` vale também para o passo `codigo`, para a tela não voltar a `inicio` enquanto o cliente digita o código.

### 2. Tratamento do código
- A chamada só acontece depois de `POST /whatsapp/{sessao}/iniciar` (a sessão precisa estar aguardando pareamento). A tela faz as duas em sequência ao clicar em "Gerar código".
- O telefone vive só no signal do componente, enquanto o formulário está aberto; não vai para URL, `localStorage`, log ou analytics. Ao sair do passo `telefone` o campo é limpo.
- O código vale enquanto a sessão aguarda pareamento. "Gerar outro código" repete a chamada; o 429 (mais de 3 em 10 min) mostra "Muitos pedidos. Aguarde alguns minutos ou use o QR Code".
- O código é texto selecionável (`user-select: all`) com `aria-live="polite"`, e o botão copiar usa `navigator.clipboard` com a mensagem "Copiado".

### 3. Mensagens de erro (nunca o texto cru da API)
| Código | Mensagem | Saída |
|---|---|---|
| 400 | "Confira o número: DDD e número, com 12 ou 13 dígitos contando o 55." | fica no passo `telefone` |
| 403 | "Você não tem acesso a esta conexão." | passo `inicio` |
| 409 | "Este WhatsApp já está conectado." | a tela consulta o status e vai a `conectado` |
| 429 | mensagem acima | oferece o QR Code |
| 503 | "Conexão por código indisponível agora." | abre o passo `qrcode` |

### 4. Painel do admin
Mesmo componente, mesmo fluxo. A opção compacta (`compacto`) mostra só os passos `codigo` e `conectado` em formato reduzido.

### 5. Processo de design
Antes do Angular, o layout dos passos é desenhado no canvas (skill `diasabatico`) em pelo menos mobile (375 px) e desktop, com os estados da tabela acima, e registrado em `especificacao/front/` (conteúdo e classes, como nos demais `*.content.json` e `telas.design.json`). Só depois o componente é alterado.

### 6. Compatibilidade com a reversão para o WPPConnect
Sem a rota de código (503), o passo `qrcode` aparece direto e o comportamento atual é preservado.

## Risks / Trade-offs

- [Cliente digita o código errado ou deixa expirar] → "Gerar outro código", com o limite de 3 em 10 min mostrado como 429.
- [Cliente no celular precisa trocar de app para copiar o código] → botão de copiar e código grande selecionável.
- [Status `INITIALIZING` chega do Java só depois da change da migração] → o tipo novo é opcional no front; sem ele, `INITIALIZING` chega como `QRCODE` e a tela mostra o QR como hoje.
- [Telefone no navegador] → só em memória do componente, limpo ao sair do passo.
