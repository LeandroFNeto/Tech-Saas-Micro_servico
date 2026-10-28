# language: pt
# Origem: swagger.yaml → POST /webhook/whatsapp
# Schema: WhatsappWebhookDTO
Funcionalidade: Webhook WhatsApp tipado
  Como provedor WPPConnect
  Quero postar apenas WhatsappWebhookDTO
  Para o backend filtrar ruído e ainda responder 200, conforme o contrato

  Cenário: mensagem de chat válida é aceita com 200
    Dado uma empresa persistida com sessão "sessao_recanto_01" e ramo "LOCACAO"
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_recanto_01      |
      | type    | chat                   |
      | fromMe  | false                  |
      | from    | 5511999999999@c.us     |
      | body    | Olá, gostaria de reservar |
    Então o status HTTP deve ser 200
    E o observador deve registrar sucesso da sessão "sessao_recanto_01"

  Cenário: Anti-X9 ignora mensagem do dono e não aciona a Factory
    Dado uma empresa persistida com sessão "sessao_recanto_01" e ramo "LOCACAO"
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_recanto_01  |
      | type    | chat               |
      | fromMe  | true               |
      | from    | 5511999999999@c.us |
      | body    | Teste do dono      |
    Então o status HTTP deve ser 200
    E o observador deve registrar filtro contendo "Anti-X9"
    E a FactoryModulo não deve ser consultada

  Cenário: grupo e tipo inválido também devolvem 200
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_recanto_01 |
      | type    | image             |
      | fromMe  | false             |
      | from    | 120363@g.us       |
      | body    | foto              |
    Então o status HTTP deve ser 200
    E o observador deve registrar filtro contendo "ignorada"

  Cenário: mensagem recebida e resposta enviada não deixam número completo nem texto no log
    Dado que a saída de log está sendo capturada
    E uma empresa persistida com sessão "sessao_logs" e ramo "LOCACAO"
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_logs                         |
      | type    | chat                                |
      | fromMe  | false                               |
      | from    | 5511955553333@c.us                  |
      | body    | Quero alugar o salao para sabado    |
    Então o status HTTP deve ser 200
    E a LocacaoStrategy deve ter processado a mensagem
    E a saída de log deve conter "***3333"
    E a saída de log não deve conter "5511955553333"
    E a saída de log não deve conter "Quero alugar o salao para sabado"
