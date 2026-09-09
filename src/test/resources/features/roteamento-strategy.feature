# language: pt
# Origem: swagger.yaml → POST /webhook/whatsapp + ramoDeAtuacao do EmpresaCreateDTO
# Arquitetura: FactoryModulo → ModuloAtendimentoStrategy (LocacaoStrategy)
Funcionalidade: Roteamento Factory e Strategy
  Como cérebro do bot
  Quero que o webhook escolha a Strategy pelo ramo da empresa
  Para o atendimento de locação não misturar com outros módulos

  Cenário: ramo LOCACAO é resolvido para LocacaoStrategy
    Dado as Strategies carregadas no contexto Spring
    Quando a FactoryModulo busca o ramo "LOCACAO"
    Então a Strategy retornada deve ser LocacaoStrategy

  Cenário: ramo locacao em minúsculas é resolvido para LocacaoStrategy
    Dado as Strategies carregadas no contexto Spring
    Quando a FactoryModulo busca o ramo "locacao"
    Então a Strategy retornada deve ser LocacaoStrategy

  Cenário: ramo Locação com acento é resolvido para LocacaoStrategy
    Dado as Strategies carregadas no contexto Spring
    Quando a FactoryModulo busca o ramo "Locação"
    Então a Strategy retornada deve ser LocacaoStrategy

  Cenário: ramo ausente no mapa da Factory é rejeitado
    Dado as Strategies carregadas no contexto Spring
    Quando a FactoryModulo busca o ramo "CLINICA"
    Então deve ocorrer erro de ramo não suportado

  Cenário: webhook de locação dispara LocacaoStrategy via Factory
    Dado uma empresa persistida com sessão "sessao_factory" e ramo "LOCACAO"
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_factory         |
      | type    | chat                   |
      | fromMe  | false                  |
      | from    | 5511988887777@c.us     |
      | body    | Quero alugar           |
    Então o status HTTP deve ser 200
    E a FactoryModulo deve ter resolvido o ramo "LOCACAO"
    E a LocacaoStrategy deve ter processado a mensagem

  Cenário: webhook com ramo sem Strategy registra erro e ainda responde 200
    Dado uma empresa persistida com sessão "sessao_clinica" e ramo "CLINICA"
    Quando eu envio POST /webhook/whatsapp com o WhatsappWebhookDTO:
      | session | sessao_clinica     |
      | type    | chat               |
      | fromMe  | false              |
      | from    | 5511977776666@c.us |
      | body    | Quero consultar    |
    Então o status HTTP deve ser 200
    E o observador deve registrar erro contendo "Ramo de atuação não suportado"
