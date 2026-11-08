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
      | event   | onselfmessage      |
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

  Cenário: primeira mensagem na sessão da atendente da Gamb recebe o cumprimento e o menu de dúvida e reunião
    Dado uma empresa persistida com sessão "sessao_gamb_atendente" e ramo "ATENDENTE_GAMB"
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_gamb_atendente |
      | type    | chat                  |
      | fromMe  | false                 |
      | from    | 5543911112222@c.us    |
      | body    | Oi                    |
    Então o status HTTP deve ser 200
    E a FactoryModulo deve ter resolvido o ramo "ATENDENTE_GAMB"
    E a mensagem enviada ao cliente deve conter "Marcar reunião com a equipe"
    E a mensagem enviada ao cliente deve começar com um cumprimento

  Cenário: sessão de teste da Gamb sem número autorizado responde a mensagem fixa e roda nenhum módulo
    Dado uma empresa persistida com sessão "sessao_gamb_teste" e ramo "TESTE_MODULOS"
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_gamb_teste       |
      | type    | chat                    |
      | fromMe  | false                   |
      | from    | 5543933334444@c.us      |
      | body    | /modulo teste-virtual oi |
    Então o status HTTP deve ser 200
    E a FactoryModulo deve ter resolvido o ramo "TESTE_MODULOS"
    E a mensagem enviada ao cliente deve conter "Não há número autorizado"


  Cenário: chamada ao webhook sem o segredo compartilhado é recusada com 401
    Dado uma empresa persistida com sessão "sessao_recanto_01" e ramo "LOCACAO"
    Quando eu envio POST /webhook/whatsapp sem o cabeçalho x-webhook-token com o WhatsappWebhookDTO:
      | session | sessao_recanto_01      |
      | type    | chat                   |
      | fromMe  | false                  |
      | from    | 5511999999999@c.us     |
      | body    | Tentativa sem token    |
    Então o status HTTP deve ser 401
    E a FactoryModulo não deve ser consultada

  Cenário: chamada ao webhook com segredo incorreto é recusada com 401 sem vazar segredo nem mensagem no log
    Dado que a saída de log está sendo capturada
    E uma empresa persistida com sessão "sessao_recanto_01" e ramo "LOCACAO"
    Quando eu envio POST /webhook/whatsapp com x-webhook-token "token-invasor-999" e o WhatsappWebhookDTO:
      | session | sessao_recanto_01         |
      | type    | chat                      |
      | fromMe  | false                     |
      | from    | 5511999999999@c.us        |
      | body    | Mensagem forjada secreta  |
    Então o status HTTP deve ser 401
    E a FactoryModulo não deve ser consultada
    E a saída de log não deve conter "token-invasor-999"
    E a saída de log não deve conter "segredo-webhook-teste"
    E a saída de log não deve conter "Mensagem forjada secreta"
