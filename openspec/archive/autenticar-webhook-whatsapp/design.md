# Design

## Decisão

O backend só aceita o webhook quando o cabeçalho de segredo bate com o valor configurado. A comparação é de tempo constante. Sem segredo configurado no ambiente, o webhook recusa tudo, em vez de aceitar tudo. A recusa responde 401 antes de ler o corpo e antes de qualquer strategy.

O segredo é um só para o WPPConnect da Gamb, porque o webhook recebe de uma instância. Restrição por IP fica como camada extra da infraestrutura, não do código.

A recusa de requisição sem segredo muda o contrato atual do webhook, que hoje responde 200 sempre. A resposta 200 para mensagem filtrada continua valendo para requisição autenticada.

## Arquivos
- Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
- Java: `security/SecurityConfig.java`, `controller/ControllerWhatsapp.java`, filtro novo para o cabeçalho, `application.properties` (propriedade do segredo, lida de variável de ambiente)
- Front: nenhum
- Prova: `src/test/resources/features/webhook.feature` e teste JUnit do filtro; `ControllerWhatsappTest` e os passos Cucumber enviam o cabeçalho

## Alinhamento
- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`

## Riscos
- [WPPConnect não consegue enviar cabeçalho próprio] → confirmar na versão em uso (`wppconnect-server/`). Se não puder, usar o segredo como parte do caminho da URL do webhook, que fica fora do log.
- [Deploy fora de ordem derruba o bot] → configurar o segredo no backend e no WPPConnect antes de publicar e testar uma mensagem real em cada número.
- [Segredo vaza em log] → o filtro nunca registra o valor do cabeçalho.
- [Testes existentes enviam webhook sem cabeçalho] → ajustar os passos Cucumber e `ControllerWhatsappTest` para enviar o cabeçalho.
