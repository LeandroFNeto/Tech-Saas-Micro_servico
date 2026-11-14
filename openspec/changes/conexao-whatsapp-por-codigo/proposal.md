# Proposal: Conexão do WhatsApp por código, passo a passo, sem segundo aparelho

## Why

Hoje o cliente conecta o WhatsApp da empresa lendo um QR Code na tela do painel, o que exige um segundo aparelho: um para mostrar o QR e outro (o celular com o WhatsApp) para escaneá-lo. Quem só tem o celular não consegue conectar. O pareamento por código de 8 caracteres, digitado no próprio celular, resolve isso, e a migração para o Baileys (`migrar-wppconnect-para-baileys`, WPP-20) o torna possível.

## What Changes

- O componente `conexao-whatsapp` (painel do cliente e painel do admin) ganha um fluxo guiado em passos, com o **código de telefone como caminho principal**:
  1. informar o número do WhatsApp da empresa, com DDI 55 e 12 ou 13 dígitos;
  2. receber o código `ABCD-EFGH` em tamanho grande, com botão de copiar;
  3. seguir as instruções "WhatsApp → Aparelhos conectados → Conectar um aparelho → Conectar com número de telefone" e digitar o código no próprio celular;
  4. a tela acompanha o status sozinha até `CONNECTED`.
- O **QR Code continua como alternativa** ("Prefiro ler o QR Code"), para quem usa o painel no computador.
- Estados da conexão na tela: `INITIALIZING` mostra "Reconectando…" sem pedir nada ao cliente; `DISCONNECTED` depois de ter conectado orienta um novo pareamento.
- Erros tratados com mensagem clara: 400 (telefone inválido), 403 (sessão de outra empresa), 409 (já conectado), 429 (muitos pedidos) e 503 (pareamento por código indisponível, com o QR como saída).
- O layout é desenhado primeiro no canvas (skill `diasabatico`) e registrado em `especificacao/front/` antes de qualquer Angular.
- **Fora desta change**: avisos ao cliente quando a sessão cai (change `alertas-desconexao-whatsapp`); rota de código, gateway e `INITIALIZING` no Java (change `migrar-wppconnect-para-baileys`, tarefas 4.2b a 4.2d e 4.2f).

## Capabilities

### Modified Capabilities

- `whatsapp`: ganha WPP-21 (conexão guiada por código de telefone no painel) e WPP-22 (estados da conexão na tela: reconectando e novo pareamento). O QR Code (WPP-04) segue valendo.

## Impact

- **Frontend Angular**: `conexao-whatsapp.component` (`.ts`, `.html`, `.spec.ts`), `WhatsappConnectionService` (chamada nova `POST /whatsapp/{sessao}/codigo-pareamento`), `whatsapp.models.ts` (tipo `INITIALIZING` e DTOs do código). Usado em `painel`, `perfil` e `empresa-form`.
- **Design**: canvas e `especificacao/front/`, antes do código.
- **Backend e gateway**: nenhuma mudança aqui; dependem da change `migrar-wppconnect-para-baileys`. Sem a rota (WPPConnect na reversão), a tela cai para o QR Code.
- **Segurança**: o telefone digitado não é gravado nem guardado no navegador além da tela aberta, e não vai para a URL nem para logs do front (`.cursor/rules/seguranca.mdc`).
