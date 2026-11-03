# Design

## Decisão
1. **Timestamp e descarte de mensagens antigas**:
   - `WhatsappWebhookDTO` adiciona os campos `timestamp` e `@JsonProperty("t") Long t`.
   - Se o timestamp estiver presente e for anterior a 2 minutos (120 segundos) comparado ao relógio do sistema, a mensagem é classificada como antiga (`ehMensagemAntiga(Clock clock)`).
   - O `ControllerWhatsapp` descarta com `observador.logFiltro(sessao, rawFrom, "Mensagem antiga descartada")` e responde 200 OK sem acionar as estratégias nem alterar o estado.

2. **Soberania do proprietário (trava de 20 minutos)**:
   - No `GerenciadorSessao`, adiciona `ativarTravaProprietario(String numeroCliente)` e `estaComTravaProprietarioAtiva(String numeroCliente)`. A constante `TRAVA_PROPRIETARIO_MINUTOS = 20` governa a expiração.
   - Quando `payload.enviadaPorMim()` for verdadeiro: o controller ativa a trava de 20 minutos para o contato envolvido e descarta a mensagem do fluxo do bot com log de filtro `Anti-X9`.
   - Quando chegar mensagem do cliente (`fromMe=false`), o controller verifica `estaComTravaProprietarioAtiva(chaveEstado)`. Se ativo, descarta o evento com log de filtro `Atendimento pausado: proprietário respondeu há menos de 20 minutos` e responde 200 OK sem disparar resposta automática.
   - Decorridos os 20 minutos sem nova mensagem do dono, a trava expira e o bot volta a processar as interações normalmente.

## Arquivos
- Contrato: `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`, `especificacao/back/paths/webhook-whatsapp.yaml`
- Java: `WhatsappWebhookDTO.java`, `GerenciadorSessao.java`, `ControllerWhatsapp.java`
- Front: nenhum
- Prova: `GerenciadorSessaoTest.java`, `WhatsappWebhookDTOTest.java`, `ControllerWhatsappTest.java`, `src/test/resources/features/webhook.feature`

## Alinhamento
- Stack de `00-architecture.md`
- DTO nomeado e imutável (record); sem entidade JPA nem Map genérico
- YAML em `especificacao/back/` antes do Java
- Sem comentários no código Java (`//` ou `/* */`)

## Riscos
- Relógio descalibrado entre o servidor WPPConnect e a API Java: margem de tolerância de 2 minutos mitiga variações leves de rede ou latência.
