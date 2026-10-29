# language: pt
# Origem: swagger.yaml → /whatsapp/{sessao}/status, /whatsapp/{sessao}/iniciar
# Schemas: SessaoStatusResponseDTO
Funcionalidade: Controle de sessão do WhatsApp do SaaS
  Como usuário autenticado do SaaS
  Quero consultar o status e iniciar a sessão do WhatsApp
  Para conectar o bot da minha empresa no WPPConnect

  Cenário: GET e POST no WhatsApp com JWT de cliente na própria sessão retornam 200
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Recanto Wpp Proprio |
      | sessaoWhatsapp | sessao_wpp_propria  |
      | ramoDeAtuacao  | LOCACAO             |
      | email          | proprio@wpp.com     |
      | senha          | SenhaCliente123     |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_wpp_propria@gamb.com.br |
      | senha | SenhaCliente123                |
    E eu envio GET /whatsapp/sessao_wpp_propria/status com o JWT do login
    Então o status HTTP deve ser 200
    Quando eu envio POST /whatsapp/sessao_wpp_propria/iniciar com o JWT do login
    Então o status HTTP deve ser 200

  Cenário: GET e POST no WhatsApp com token legado retornam 401
    Dado uma empresa persistida com sessão "sessao_wpp_legado" e ramo "LOCACAO"
    Quando eu envio GET /whatsapp/sessao_wpp_legado/status com o token legado de cliente
    Então o status HTTP deve ser 401
    Quando eu envio POST /whatsapp/sessao_wpp_legado/iniciar com o token legado de cliente
    Então o status HTTP deve ser 401

  Cenário: GET e POST no WhatsApp de cliente em sessão de outra empresa retornam 403
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Empresa Wpp Alvo |
      | sessaoWhatsapp | sessao_wpp_alvo  |
      | ramoDeAtuacao  | LOCACAO          |
      | email          | alvo@wpp.com     |
      | senha          | SenhaCliente123  |
    E eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Empresa Wpp Intruso |
      | sessaoWhatsapp | sessao_wpp_intruso  |
      | ramoDeAtuacao  | LOCACAO             |
      | email          | intruso@wpp.com     |
      | senha          | SenhaCliente123     |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_wpp_intruso@gamb.com.br |
      | senha | SenhaCliente123                |
    E eu envio GET /whatsapp/sessao_wpp_alvo/status com o JWT do login
    Então o status HTTP deve ser 403
    Quando eu envio POST /whatsapp/sessao_wpp_alvo/iniciar com o JWT do login
    Então o status HTTP deve ser 403

  Cenário: GET e POST no WhatsApp de cliente em sessão inexistente retornam 403
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Empresa Wpp Base |
      | sessaoWhatsapp | sessao_wpp_base  |
      | ramoDeAtuacao  | LOCACAO          |
      | email          | base@wpp.com     |
      | senha          | SenhaCliente123  |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_wpp_base@gamb.com.br |
      | senha | SenhaCliente123             |
    E eu envio GET /whatsapp/sessao_wpp_inexistente/status com o JWT do login
    Então o status HTTP deve ser 403
    Quando eu envio POST /whatsapp/sessao_wpp_inexistente/iniciar com o JWT do login
    Então o status HTTP deve ser 403

  Cenário: GET e POST no WhatsApp de admin em sessão inexistente retornam 404
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio GET /whatsapp/sessao_admin_inexistente/status com o token administrativo
    Então o status HTTP deve ser 404
    Quando eu envio POST /whatsapp/sessao_admin_inexistente/iniciar com o token administrativo
    Então o status HTTP deve ser 404

  Cenário: GET e POST no WhatsApp de admin em qualquer sessão existente retornam 200
    Dado o token administrativo "sua-chave-admin"
    E uma empresa persistida com sessão "sessao_wpp_qualquer" e ramo "LOCACAO"
    Quando eu envio GET /whatsapp/sessao_wpp_qualquer/status com o token administrativo
    Então o status HTTP deve ser 200
    Quando eu envio POST /whatsapp/sessao_wpp_qualquer/iniciar com o token administrativo
    Então o status HTTP deve ser 200

  Cenário: GET e POST no WhatsApp sem token retornam 401
    Dado uma empresa persistida com sessão "sessao_wpp_sem_token" e ramo "LOCACAO"
    Quando eu envio GET /whatsapp/sessao_wpp_sem_token/status sem token
    Então o status HTTP deve ser 401
    Quando eu envio POST /whatsapp/sessao_wpp_sem_token/iniciar sem token
    Então o status HTTP deve ser 401
