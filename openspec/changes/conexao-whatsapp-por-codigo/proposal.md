# Proposal: Conexão do WhatsApp por código, passo a passo, sem segundo aparelho

## Why

Hoje o cliente conecta o WhatsApp da empresa lendo um QR Code na tela do painel, o que exige um segundo aparelho: um para mostrar o QR e outro (o celular com o WhatsApp) para escaneá-lo. Quem só tem o celular não consegue conectar. O pareamento por código de 8 caracteres, digitado no próprio celular, resolve isso, e a migração para o Baileys (`migrar-wppconnect-para-baileys`, WPP-20) o torna possível.

## What Changes

- O componente `conexao-whatsapp` ganha um fluxo guiado em passos, com o **código de telefone como caminho principal**:
  1. informar o número do WhatsApp da empresa, com DDI 55 e 12 ou 13 dígitos;
  2. receber o código `ABCD-EFGH` em tamanho grande, com botão de copiar e **contagem regressiva da validade**;
  3. seguir as instruções "WhatsApp → Aparelhos conectados → Conectar um aparelho → Conectar com número de telefone" e digitar o código no próprio celular;
  4. a tela acompanha o status sozinha até `CONNECTED`.
- O **QR Code continua como alternativa** ("Prefiro ler o QR Code"), para quem usa o painel no computador.
- Estados da conexão na tela: `INITIALIZING` mostra "Reconectando…" (ou "Conectando…" logo depois de o cliente digitar o código) sem pedir nada; `DISCONNECTED` com sessão **já pareada e encerrada** orienta um novo pareamento. Esse "já pareada e encerrada" vem do **servidor** (campo novo no status), não da memória da tela.
- Erros tratados com mensagem clara: 400, 403, 404, 409 (com dois significados: já conectada ou ainda iniciando), 429 e 503.
- Casos de experiência de usuário tratados: código expirado, aba em segundo plano, navegador sem internet, clique duplo, troca de empresa no admin, sessão expirada (401), página recarregada no meio do pareamento, falha no celular e leitor de tela.
- Telas onde o status aparece e hoje não sabem de `INITIALIZING`: o componente (painel do cliente em versão `compacto`, `empresa-form` do admin em versão normal) e o chip de WhatsApp do `dashboard` do admin.
- O layout é desenhado primeiro no canvas (skill `diasabatico`) e registrado em `especificacao/front/` antes de qualquer Angular.
- **Ajuste pequeno no backend e no gateway**, necessário para o estado vir do servidor: `status-session` passa a informar se a sessão já foi pareada e foi encerrada (`sessaoEncerrada`), com marca persistida em `meta.json`, campo em `SessaoStatusResponseDTO` e contrato YAML.
- **Regra de testes**: toda função criada e todo bug encontrado ganham teste, para não voltar a ocorrer (ver design).
- **Fora desta change**: avisos ao cliente quando a sessão cai (change `alertas-desconexao-whatsapp`, que também cobre o celular sem internet); rota de código, gateway base e `INITIALIZING` no Java (change `migrar-wppconnect-para-baileys`, tarefas 4.2b a 4.2d e 4.2f).

## Capabilities

### Modified Capabilities

- `whatsapp`: ganha WPP-24 (conexão guiada por código de telefone no painel) e WPP-22 (estados da conexão na tela: conectando, reconectando e novo pareamento vindo do servidor). O QR Code (WPP-04) segue valendo.

## Impact

- **Frontend Angular**: `conexao-whatsapp.component` (`.ts`, `.html`, `.spec.ts`), `WhatsappConnectionService` (chamada nova `POST /whatsapp/{sessao}/codigo-pareamento`), `whatsapp.models.ts` (tipo `INITIALIZING`, `sessaoEncerrada` e DTOs do código), `features/admin/dashboard/dashboard.component.html` (chip de WhatsApp com `INITIALIZING`). O componente é usado em `painel` (cliente, `compacto`) e `empresa-form` (admin, versão normal). O `perfil` não o usa.
- **Backend e gateway**: `SessaoStatusResponseDTO` (+ YAML), `ServicoWppConnect` (repassa o campo) e `baileys-gateway` (`status-session` e `meta.json`). Contrato YAML antes do Java.
- **Design**: canvas e `especificacao/front/`, antes do código.
- **Segurança**: o telefone digitado não é gravado nem guardado no navegador além da tela aberta, e não vai para a URL nem para logs do front; o código de pareamento não vai para log, título da aba nem armazenamento (`.cursor/rules/seguranca.mdc`).
