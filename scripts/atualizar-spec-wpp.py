from pathlib import Path

p = Path("openspec/specs/whatsapp/spec.md")
c = p.read_text(encoding="utf-8")

velho = """### WPP-02 Filtrar ruído sem tratar como atendimento

O sistema deve responder 200 e não consultar a factory quando o evento não é um chat de cliente: mensagem do próprio dono (Anti-X9), grupo, tipo inválido, QR Code, status de conexão, presença e ack.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`
Prova: `src/test/resources/features/webhook.feature`, `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`

Cenário: mensagem do dono

- Quando o webhook chega com `fromMe=true`
- Então a API responde 200
- E o observador registra filtro contendo "Anti-X9"
- E a factory não é consultada"""

novo = """### WPP-02 Filtrar ruído, mensagens antigas e silenciar o bot por intervenção do dono (Anti-X9 com trava de 20 min)

O sistema deve responder 200 e não consultar a factory de atendimento quando o evento não é um chat de cliente a ser processado:
1. Mensagem enviada pelo próprio número conectado (`fromMe=true`): o sistema ativa a trava de 20 minutos para o cliente/contato alvo e silencia o bot (Anti-X9).
2. Chat de cliente enquanto a trava de 20 minutos do proprietário estiver ativa: o sistema não responde, mantendo o atendimento manual do dono sem interferência do robô.
3. Mensagem antiga recebida pelo webhook: mensagens cujo `timestamp` possua atraso superior a 2 minutos (120 segundos) em relação ao relógio do sistema são descartadas.
4. Ruídos gerais: grupo (`@g.us`), tipo inválido, QR Code, status de conexão, presença e ack.

Contrato: `especificacao/back/paths/webhook-whatsapp.yaml`, `especificacao/back/schemas/webhook/WhatsappWebhookDTO.yaml`
Prova: `src/test/resources/features/webhook.feature`, `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/java/com/example/demo/dto/WhatsappWebhookDTOTest.java`, `src/test/java/com/example/demo/servico/GerenciadorSessaoTest.java`

Cenário: mensagem do dono ativa trava de 20 minutos

- Quando o webhook chega com `fromMe=true` em conversa com cliente
- Então a API responde 200
- E a trava de 20 minutos é ativada para aquele contato
- E o observador registra filtro contendo "Anti-X9"
- E a factory não é consultada

Cenário: cliente envia mensagem durante a pausa do proprietário

- Dado que o dono respondeu ao cliente há menos de 20 minutos
- Quando o cliente envia uma nova mensagem
- Então a API responde 200
- E o observador registra filtro contendo "proprietário atendeu nos últimos 20 minutos"
- E a factory não é consultada

Cenário: mensagem antiga descartada

- Quando o webhook chega com timestamp com mais de 2 minutos de atraso
- Então a API responde 200
- E o observador registra filtro contendo "Mensagem antiga descartada"
- E a factory não é consultada"""

if velho in c:
    c = c.replace(velho, novo, 1)
else:
    c = c.replace(velho.replace("\n", "\r\n"), novo.replace("\n", "\r\n"), 1)

p.write_text(c, encoding="utf-8")
print("spec.md atualizado com sucesso!")
