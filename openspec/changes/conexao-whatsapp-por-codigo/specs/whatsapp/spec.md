# Spec Delta: Conexão do WhatsApp por código de telefone no painel

## ADDED Requirements

### Requirement: WPP-21 Conexão guiada por código de telefone no painel
O sistema SHALL permitir ao cliente e ao admin conectar o WhatsApp da empresa, no componente `conexao-whatsapp`, em passos (número, código, digitar no celular, conectado) usando só o próprio celular, com o QR Code como alternativa, sem guardar o telefone informado.

Contrato: `especificacao/back/paths/whatsapp-codigo-pareamento.yaml` (change `migrar-wppconnect-para-baileys`); layout em `especificacao/front/`
Prova: `frontend/src/app/shared/components/conexao-whatsapp/conexao-whatsapp.component.spec.ts`

#### Scenario: Conectar pelo código
- **GIVEN** uma sessão desconectada no painel
- **WHEN** o cliente escolhe "Conectar com número de telefone", informa o número com DDI 55 e pede o código
- **THEN** a tela inicia a sessão, pede o código e mostra `ABCD-EFGH` em tamanho grande com botão de copiar e as instruções "WhatsApp → Aparelhos conectados → Conectar com número de telefone"
- **AND** quando o status vira `CONNECTED` a tela mostra "WhatsApp online"

#### Scenario: Telefone inválido
- **WHEN** o número informado não tem 12 ou 13 dígitos com 55
- **THEN** a tela mostra a mensagem do campo e não chama a API

#### Scenario: Copiar o código
- **WHEN** o cliente toca em "Copiar"
- **THEN** o código vai para a área de transferência e a tela mostra "Copiado"

#### Scenario: Gerar outro código
- **WHEN** o cliente pede outro código
- **THEN** a tela pede um novo código e substitui o anterior

#### Scenario: Muitos pedidos
- **WHEN** a API responde 429
- **THEN** a tela mostra a mensagem de espera e oferece o QR Code

#### Scenario: Já conectado
- **WHEN** a API responde 409
- **THEN** a tela consulta o status e mostra "WhatsApp online"

#### Scenario: Sessão de outra empresa
- **WHEN** a API responde 403
- **THEN** a tela mostra "Você não tem acesso a esta conexão." e volta ao início

#### Scenario: Código indisponível
- **WHEN** a API responde 503 (gateway sem pareamento por código)
- **THEN** a tela abre o QR Code como saída

#### Scenario: QR Code como alternativa
- **WHEN** o cliente escolhe "Prefiro ler o QR Code"
- **THEN** a tela mostra o QR Code e o polling segue até `CONNECTED`, como hoje

#### Scenario: Telefone fora do armazenamento
- **WHEN** o cliente informa o número
- **THEN** o número fica só na memória do componente, não vai para URL, `localStorage` nem log, e é limpo ao sair do passo

#### Scenario: Painel do admin
- **WHEN** o admin abre a conexão de uma empresa em `empresa-form`
- **THEN** o mesmo fluxo funciona na versão compacta

### Requirement: WPP-22 Estados da conexão na tela
O sistema SHALL mostrar na tela de conexão os estados de reconexão e de novo pareamento, sem pedir ação ao cliente quando a reconexão é automática.

Contrato: `SessaoStatusResponseDTO` com o status `INITIALIZING` (change `migrar-wppconnect-para-baileys`, tarefa 4.2f)
Prova: `frontend/src/app/shared/components/conexao-whatsapp/conexao-whatsapp.component.spec.ts`

#### Scenario: Reconectando
- **GIVEN** uma sessão que estava conectada
- **WHEN** o status vira `INITIALIZING`
- **THEN** a tela mostra "Reconectando… isso é automático, não precisa fazer nada" e nenhum botão de conectar

#### Scenario: Volta sozinha
- **WHEN** o status volta a `CONNECTED`
- **THEN** a tela mostra "WhatsApp online" sem o cliente fazer nada

#### Scenario: Novo pareamento necessário
- **GIVEN** que a tela já viu a sessão `CONNECTED` ou `INITIALIZING`
- **WHEN** o status vira `DISCONNECTED`
- **THEN** a tela mostra "A conexão foi encerrada. Conecte de novo" e o botão de conectar por código

#### Scenario: Sem piscar durante o pareamento
- **WHEN** o cliente acabou de pedir o código e o status ainda vem `DISCONNECTED`
- **THEN** a tela fica no passo do código por até 45 segundos
