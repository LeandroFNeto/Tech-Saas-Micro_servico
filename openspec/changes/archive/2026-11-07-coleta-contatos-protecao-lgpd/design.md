# Design

## Context

O microsserviço já conta com a entidade `Contato.java` e o serviço `ServicoContato.java` capazes de realizar a gravação de contatos utilizando cifragem simétrica (AES) e hashing HMAC com salt para deduplicação sem exposição de dados brutos na base. No entanto, não há ainda:
1. Captura e persistência explícita do nome ou identificador informado pelo cliente (pushName do WhatsApp ou nome sugerido no atendimento);
2. Endpoints padronizados na especificação OpenAPI (`especificacao/back/`) para consulta segura da lista de contatos pelo cliente;
3. Cota e limitador rígido anti-vazamento de até 10 contatos desmascarados por hora por usuário;
4. DTOs que entreguem o telefone mascarado por padrão e tela/aba correspondente no frontend Angular do painel cliente (`features/client/contatos`).

Veja motivação detalhada em `proposal.md`.

## Goals / Non-Goals

**Goals:**
- Projetar o contrato OpenAPI (`especificacao/back/`) para `/api/v1/contatos` contemplando listagem paginada, revelação controlada e exclusão/opt-out.
- Capturar e apresentar o nome/pushName ou sugestão de identidade do remetente para situar o operador sobre quem enviou a mensagem.
- Entregar dados de contato com mascaramento prévio no backend (DTO), evitando tráfego de texto plano no navegador.
- Estabelecer limitador estrito de 10 desmascaramentos ou consultas detalhadas por hora por usuário (HTTP 429 quando excedido) para bloquear coleta ou disparo em massa.
- Prover comandos de opt-out (revogação e anonimização/exclusão) tanto pelo canal WhatsApp quanto pelo painel administrativo.
- Construir interface Angular limpa e responsiva na aba de contatos do painel do cliente, alinhada à paleta e componentes existentes do projeto.

**Non-Goals:**
- Não criar botões ou endpoints de exportação em massa (CSV/XLSX irrestrito), visto que a exportação massiva desprotegida viola diretamente a diretriz de prevenção a vazamentos e a LGPD.
- Não acoplar ferramentas ou CRMs externos nesta etapa.
- Não alterar a inteligência de resposta da IA nem o fluxo do webhook já validado.

## Decisions

### 1. Captura de Nome e Sugestão de Identidade
- **Escolha**: Extrair o pushName do payload do webhook (ou nome extraído da conversa) e persistir junto ao registro de contato (cifrado ou em coluna de exibição sanitizada), permitindo ao operador saber quem é o cliente na listagem.
- **Alternativa descartada**: Salvar apenas o telefone puro. Descartado porque impede o operador de saber quem enviou a mensagem sem abrir cada conversa.

### 2. Mascaramento no Backend (Server-Side Masking)
- **Escolha**: O método de serialização/DTO converte o telefone decifrado diretamente para formato mascarado (`+55 (11) 9****-1234`) antes de enviar o JSON ao cliente.
- **Alternativa descartada**: Mascarar apenas via pipe/CSS no Angular. Descartado porque qualquer usuário mal-intencionado poderia abrir o DevTools e copiar a lista de telefones em texto plano trafegada na rede.

### 3. Cota Anti-Vazamento: Máximo de 10 Contatos Completos por Hora por Operador
- **Escolha**: Implementar contador em janela de 1 hora por usuário autenticado. O operador pode visualizar a lista mascarada com paginação de até 50 itens, porém a revelação/obtenção do número completo fica restrita a no máximo 10 contatos por hora. Ao tentar o 11º contato, a API bloqueia com HTTP 429 Too Many Requests com cabeçalho `Retry-After`.
- **Alternativa descartada**: Acesso irrestrito a todos os números da base. Descartado pelo altíssimo risco de vazamento ou utilização da base para disparos não autorizados em massa.

### 4. Criptografia AES em Repouso e HMAC com Salt para Busca
- **Escolha**: Manter a persistência de `telefoneCifrado` (AES-GCM ou CBC com padding seguro) e `telefoneHash` (HMAC-SHA256 com chave privada). O número puro jamais é persistido em colunas de texto legível.
- **Alternativa descartada**: Hash simples sem salt ou criptografia reversível fraca. Descartado por vulnerabilidade a ataques de dicionário ou tabelas rainbow.

### 5. Gestão de Opt-out e Direito ao Esquecimento
- **Escolha**: Quando o titular envia mensagem de cancelamento ("SAIR", "CANCELAR", "STOP") ou quando o operador confirma a exclusão no painel, o registro é removido ou anonimizado (substituição dos dados pessoais e limpeza do hash), impedindo futuros contatos não solicitados.
- **Alternativa descartada**: Bloqueio apenas visual sem exclusão real do dado. Descartado por violar o Artigo 18 da LGPD (eliminação dos dados pessoais tratados com o consentimento).

## Risks / Trade-offs

- **[Risco] Operador atingir o limite de 10 contatos por hora em horário de pico** → *Mitigação*: A listagem mascarada e o nome do contato já atendem a maioria das consultas de triagem; a cota de 10 protege contra exfiltração em massa e pode ser auditada para exceções administrativas se necessário.
- **[Risco] Nome do contato (pushName) vir genérico ou com emojis** → *Mitigação*: Sanitização no recebimento, mantendo o pushName quando válido ou indicando "Contato WhatsApp" caso ausente.
- **[Risco] Esquecimento ou ausência de configuração das chaves criptográficas em produção** → *Mitigação*: Validação fail-fast na inicialização do serviço com mensagem clara caso `contatos.cripto.chave` ou `contatos.indice.chave` não estejam provisionadas em variáveis de ambiente.

## Migration Plan

1. Definir o contrato OpenAPI em `especificacao/back/` para os endpoints de contatos, revelação limitada e DTOs mascarados.
2. Atualizar o backend Java com o Controller, DTOs, limitador de cota horária (10 contatos/h) e rotinas de opt-out.
3. Desenvolver o componente Angular `ContatosComponent` na área de clientes do frontend.
4. Validar o fluxo ponta a ponta com testes unitários, BDD e regressão automatizada.
