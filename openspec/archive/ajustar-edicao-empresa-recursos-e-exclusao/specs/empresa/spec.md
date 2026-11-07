## ADDED Requirements

### Requirement: EMP-15 Admin controla os recursos da empresa na edição

O sistema SHALL mostrar na tela de edição da empresa uma seção "Recursos da empresa" com um interruptor para cada recurso: atendente virtual com IA, previsão do tempo, reserva automática e cada módulo do bot do catálogo. Salvar a tela SHALL enviar todos os valores no PUT `/empresas/{sessao}/admin`. A IA SHALL ser o módulo `IA_GEMINI` dentro de `modulosAtivos`; ligar ou desligar o interruptor SHALL ligar ou desligar `usaIA` pelo cálculo que o back já faz. A regra da agenda para a reserva automática (EMP-02) SHALL continuar valendo.

Prova: `frontend/src/app/features/admin/empresa-form/empresa-form.component.spec.ts`

#### Scenario: admin liga a IA
- **WHEN** o admin liga o interruptor de IA na edição e salva
- **THEN** o PUT do admin leva `IA_GEMINI` em `modulosAtivos`
- **AND** a empresa passa a ter `usaIA` verdadeiro

#### Scenario: admin desliga a IA
- **WHEN** o admin desliga o interruptor de IA e salva
- **THEN** `IA_GEMINI` não vai em `modulosAtivos`
- **AND** a empresa passa a ter `usaIA` falso

#### Scenario: tela carrega o estado atual
- **GIVEN** uma empresa com `IA_GEMINI` ativo e previsão do tempo liberada
- **WHEN** o admin abre a edição dela
- **THEN** os interruptores de IA e de previsão aparecem ligados

### Requirement: EMP-16 Excluir empresa pela página de edição

O sistema SHALL oferecer a exclusão da empresa só no fim da página de edição, em uma zona de exclusão separada do formulário e abaixo de "Redefinir senha". O botão SHALL aparecer só para o super admin. O clique SHALL abrir uma confirmação com o nome da empresa; a exclusão só ocorre após a confirmação e usa a mesma regra de EMP-13. Depois de excluir, a tela SHALL voltar para `/admin`. A lista de empresas do dashboard SHALL NOT ter botão de excluir.

Prova: `frontend/src/app/features/admin/empresa-form/empresa-form.component.spec.ts` e `frontend/src/app/features/admin/dashboard/dashboard.component.spec.ts`

#### Scenario: super admin exclui pela edição
- **GIVEN** o super admin na edição de uma empresa
- **WHEN** ele clica em "Excluir empresa" no fim da página e confirma
- **THEN** a tela envia DELETE `/empresas/{sessao}`
- **AND** volta para `/admin`

#### Scenario: cancelar a confirmação
- **WHEN** o super admin abre a confirmação e cancela
- **THEN** nenhum DELETE é enviado e a página continua aberta

#### Scenario: outro perfil não vê a exclusão
- **GIVEN** um admin sem perfil de super admin
- **WHEN** ele abre a edição da empresa
- **THEN** a zona de exclusão não aparece

#### Scenario: lista sem botão de excluir
- **WHEN** o admin abre o dashboard
- **THEN** cada empresa tem só o botão "Editar"

## MODIFIED Requirements

### Requirement: EMP-06 Painel inicial do cliente com WhatsApp, previsão e atalho para o chatbot

O sistema SHALL abrir em `/cliente` um painel inicial com a conexão do WhatsApp (regra de WPP-04, sem mudança), a previsão dos próximos dias (até 8) e um atalho para a configuração do chatbot em `/cliente/chatbot`. Cada dia mostra o tempo, as temperaturas, a chance e o volume de chuva, a marcação "Reservado" quando a data está ocupada no Google Agenda da empresa e o risco de desmarcação BAIXO, MÉDIO ou ALTO. O risco é ALTO com chance de chuva de 70% ou mais, temporal ou 10 mm ou mais; MÉDIO com chance de 40% a 69% ou de 2 mm a 9,9 mm; BAIXO no resto, comparando a chance arredondada para inteiro e a chuva com 1 casa decimal. A previsão de cada cidade é renovada no máximo a cada 8 horas e a ocupação é recalculada a cada consulta, no fuso `America/Sao_Paulo`. Quando há dias na tela, aparece a atribuição "Dados meteorológicos: OpenWeather" e o horário da atualização.

Quando a empresa não tem `permitePrevisaoTempo`, o painel SHALL NOT mostrar o bloco da previsão, nem o interruptor de ativá-la, nem aviso sobre ela, e a tela não consulta a previsão.

A consulta da previsão (GET `/empresas/{sessao}/previsao`) e o PUT do cliente só aceitam o admin ou o cliente da própria empresa. Quem não é admin recebe a mesma resposta 403 para sessão inexistente e para empresa alheia; o 404 de sessão inexistente sai só para o admin. O ID da agenda Google da empresa só é devolvido ao admin.

Contrato: `especificacao/back/paths/empresas-previsao.yaml` (`PrevisaoTempoResponseDTO`, `PrevisaoDiaDTO`), `especificacao/back/paths/empresas-cliente.yaml` e `especificacao/back/paths/empresas-search.yaml`
Conteúdo da tela: `especificacao/front/cliente-painel.content.json`
Prova: `src/test/java/com/example/demo/servico/previsao/CalculadoraRiscoDesmarcacaoTest.java`, `src/test/java/com/example/demo/servico/previsao/ServicoPrevisaoTempoTest.java`, `src/test/resources/features/empresas.feature`, `frontend/src/app/features/client/painel/painel.component.spec.ts` e `frontend/src/app/core/services/previsao.service.spec.ts`

#### Scenario: painel inicial do cliente
- **GIVEN** o cliente autenticado e a previsão liberada pelo admin
- **WHEN** ele abre `/cliente`
- **THEN** vê a conexão do WhatsApp, o bloco da previsão e o atalho "Configuração do chatbot"
- **AND** o atalho leva a `/cliente/chatbot`

#### Scenario: dia reservado com chuva forte
- **GIVEN** a previsão ligada, a cidade resolvida e uma data ocupada no Google Agenda
- **AND** o provedor prevê para essa data chance de chuva de 70% ou mais
- **WHEN** o cliente consulta a previsão
- **THEN** a API responde 200
- **AND** esse dia vem marcado como reservado e com risco ALTO
- **AND** a tela mostra a atribuição "Dados meteorológicos: OpenWeather"

#### Scenario: previsão desligada, sem cidade ou provedor fora do ar
- **WHEN** a previsão é consultada nessas condições
- **THEN** a API responde 200 indicando previsão indisponível e sem dias
- **AND** a tela mostra o aviso correspondente sem quebrar o painel
- **AND** nenhuma chamada externa é disparada

#### Scenario: consulta de previsão em empresa sem permissão
- **GIVEN** uma empresa com permissão de previsão revogada ou não concedida
- **WHEN** é requisitado GET `/empresas/{sessao}/previsao`
- **THEN** a resposta retorna status 200 com flag indicando indisponibilidade sem acionar o provedor externo

#### Scenario: painel sem a previsão liberada
- **GIVEN** o cliente autenticado de uma empresa sem `permitePrevisaoTempo`
- **WHEN** ele abre `/cliente`
- **THEN** o painel não mostra o bloco da previsão nem o interruptor de ativar
- **AND** nenhuma consulta de previsão é feita
- **AND** a conexão do WhatsApp e o atalho do chatbot continuam visíveis
