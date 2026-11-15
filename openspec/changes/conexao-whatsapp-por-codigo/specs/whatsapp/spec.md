# Spec Delta: Conexão do WhatsApp por código de telefone no painel

## ADDED Requirements

### Requirement: WPP-24 Conexão guiada por código de telefone no painel
O sistema SHALL permitir ao cliente e ao admin conectar o WhatsApp da empresa, no componente `conexao-whatsapp`, em passos (número, código, digitar no celular, conectado) usando só o próprio celular, com o QR Code como alternativa, mostrando por quanto tempo o código vale e sem guardar o telefone informado.

Contrato: `especificacao/back/paths/whatsapp-codigo-pareamento.yaml` (change `migrar-wppconnect-para-baileys`); layout em `especificacao/front/`
Prova: `frontend/src/app/shared/components/conexao-whatsapp/conexao-whatsapp.component.spec.ts`, `frontend/src/app/features/client/painel/painel.component.spec.ts` e `frontend/src/app/features/admin/empresa-form/empresa-form.component.spec.ts`

#### Scenario: Conectar pelo código
- **GIVEN** uma sessão desconectada no painel
- **WHEN** o cliente escolhe "Conectar com número de telefone", informa o número com DDI 55 e pede o código
- **THEN** a tela inicia a sessão, pede o código e mostra `ABCD-EFGH` em tamanho grande com botão de copiar, a contagem de validade e as instruções "WhatsApp → Aparelhos conectados → Conectar com número de telefone"
- **AND** quando o status vira `CONNECTED` a tela mostra "WhatsApp online"

#### Scenario: Telefone inválido
- **WHEN** o número informado não tem 12 ou 13 dígitos com 55
- **THEN** a tela mostra a mensagem do campo e não chama a API

#### Scenario: Telefone colado com símbolos
- **WHEN** o cliente cola `+55 (11) 99999-9999`
- **THEN** a tela usa só os dígitos e valida o resultado

#### Scenario: Copiar o código
- **WHEN** o cliente toca em "Copiar"
- **THEN** o código vai para a área de transferência e a tela mostra "Copiado"

#### Scenario: Contagem de validade
- **WHEN** o código aparece
- **THEN** a tela mostra quanto tempo ele ainda vale e a contagem diminui até zero

#### Scenario: Código expirado
- **WHEN** a contagem chega a zero sem o status virar `CONNECTED`
- **THEN** a tela mostra "O código expirou" e o botão "Gerar outro código", mantendo o QR Code como alternativa

#### Scenario: Gerar outro código
- **WHEN** o cliente pede outro código
- **THEN** a tela pede um novo código, substitui o anterior e reinicia a contagem

#### Scenario: Clique duplo
- **WHEN** o cliente clica duas vezes em "Gerar código"
- **THEN** a tela faz só um pedido e o segundo clique não consome o limite de pedidos

#### Scenario: Muitos pedidos
- **WHEN** a API responde 429
- **THEN** a tela mostra a mensagem de espera e oferece o QR Code

#### Scenario: Já conectado
- **WHEN** a API responde 409 e o status consultado em seguida é `CONNECTED`
- **THEN** a tela mostra "WhatsApp online"

#### Scenario: Conexão ainda iniciando
- **WHEN** a API responde 409 e o status consultado em seguida não é `CONNECTED`
- **THEN** a tela mostra "Preparando a conexão…" e repete o pedido sozinha até 3 vezes, a cada 2 segundos
- **AND** se o 409 continuar, mostra "A conexão ainda está iniciando. Tente de novo em alguns segundos" com o botão "Tentar de novo"

#### Scenario: Sessão de outra empresa
- **WHEN** a API responde 403
- **THEN** a tela mostra "Você não tem acesso a esta conexão." e volta ao início

#### Scenario: Empresa sem sessão
- **WHEN** a API responde 404 para o admin
- **THEN** a tela mostra "Não encontramos a conexão desta empresa." e volta ao início

#### Scenario: Sessão do painel expirada
- **WHEN** a API responde 401
- **THEN** o painel leva ao login e a tela limpa telefone, código e contagem

#### Scenario: Código indisponível
- **WHEN** a API responde 503 (gateway sem pareamento por código)
- **THEN** a tela abre o QR Code como saída

#### Scenario: QR Code como alternativa
- **WHEN** o cliente escolhe "Prefiro ler o QR Code"
- **THEN** a tela mostra o QR Code e o polling segue até `CONNECTED`, como hoje

#### Scenario: Telefone e código fora do armazenamento
- **WHEN** o cliente informa o número e recebe o código
- **THEN** número e código ficam só na memória do componente, não vão para URL, `localStorage`, log nem título da aba, e são limpos ao sair do passo, ao trocar de empresa e no 401

#### Scenario: Troca de empresa no admin
- **WHEN** o admin muda a empresa exibida no formulário
- **THEN** a tela volta ao início e limpa telefone, código e contagem

#### Scenario: Página recarregada com o código na tela
- **WHEN** o cliente recarrega a página durante o passo do código
- **THEN** o código anterior não reaparece e a tela mostra o estado do servidor, oferecendo gerar outro código

#### Scenario: Aba em segundo plano
- **WHEN** a aba fica oculta durante o pareamento e depois volta a ficar visível
- **THEN** a tela consulta o status na hora

#### Scenario: Navegador sem internet
- **WHEN** o navegador perde a conexão durante o pareamento
- **THEN** a tela avisa "Sem internet neste aparelho" e retoma o polling quando a conexão voltar

#### Scenario: Acessibilidade do código
- **WHEN** o código aparece
- **THEN** o foco vai para o código, que é anunciado por leitor de tela e pode ser selecionado, e os botões têm rótulo e alvo de toque de pelo menos 44 px

#### Scenario: Versões da tela
- **WHEN** o cliente abre o `painel` ou o admin abre `empresa-form`
- **THEN** cada tela mostra a versão definida no design (cliente em `compacto`, admin na versão normal), com o mesmo fluxo

### Requirement: WPP-22 Estados da conexão na tela
O sistema SHALL mostrar na tela de conexão os estados de conectando, reconectando e novo pareamento, sem pedir ação ao cliente quando a reconexão é automática, e SHALL decidir o novo pareamento pelo estado informado pelo servidor.

Contrato: `SessaoStatusResponseDTO` com o status `INITIALIZING` (change `migrar-wppconnect-para-baileys`, tarefa 4.2f) e o campo `sessaoEncerrada` (esta change, tarefa 2.1)
Prova: `frontend/src/app/shared/components/conexao-whatsapp/conexao-whatsapp.component.spec.ts`, `baileys-gateway/test/sessao.test.ts` e `src/test/java/com/example/demo/servico/ServicoWppConnectTest.java`

#### Scenario: Reconectando
- **GIVEN** uma sessão que estava conectada
- **WHEN** o status vira `INITIALIZING`
- **THEN** a tela mostra "Reconectando… isso é automático, não precisa fazer nada" e nenhum botão de conectar

#### Scenario: Conectando depois do código
- **GIVEN** que o cliente estava no passo do código ou do QR Code
- **WHEN** o status vira `INITIALIZING`
- **THEN** a tela mostra "Conectando… confirmando com o WhatsApp" e não "Reconectando…"

#### Scenario: Reconexão enquanto a pessoa digita o telefone
- **GIVEN** que a pessoa está no passo do telefone
- **WHEN** o status vira `INITIALIZING`
- **THEN** a tela mantém o passo e o que foi digitado, sem mostrar "Reconectando…"

#### Scenario: Volta sozinha
- **WHEN** o status volta a `CONNECTED`
- **THEN** a tela mostra "WhatsApp online" sem o cliente fazer nada

#### Scenario: Novo pareamento necessário
- **GIVEN** que o servidor informa `DISCONNECTED` com `sessaoEncerrada` verdadeiro
- **WHEN** a tela é aberta ou recarregada
- **THEN** a tela mostra "A conexão foi encerrada. Conecte de novo" e o botão de conectar por código

#### Scenario: Nunca pareada
- **GIVEN** que o servidor informa `DISCONNECTED` com `sessaoEncerrada` falso ou ausente
- **WHEN** a tela é aberta
- **THEN** a tela mostra o início do fluxo e não a mensagem de conexão encerrada

#### Scenario: Marca de encerramento no gateway
- **WHEN** o gateway recebe `loggedOut`, `badSession` ou `multideviceMismatch`
- **THEN** grava `encerradaEm` em `meta.json` e `status-session` devolve `sessaoEncerrada` verdadeiro enquanto o status for `DISCONNECTED`
- **AND** um novo pareamento limpa a marca, e `close-session` apaga a pasta sem deixá-la

#### Scenario: Sem piscar durante o pareamento
- **WHEN** o cliente acabou de pedir o código e o status ainda vem `DISCONNECTED`
- **THEN** a tela fica no passo do código enquanto ele vale, e no passo conectando por 45 segundos depois do último `INITIALIZING`
- **AND** depois de conectada, um `DISCONNECTED` sem `sessaoEncerrada` mostra "Reconectando…" por até 45 segundos antes de voltar ao início

#### Scenario: Chip do dashboard do admin
- **WHEN** o status da sessão de uma empresa é `INITIALIZING`
- **THEN** o chip de WhatsApp do dashboard mostra "Reconectando" em vez de "Consultando..."
