# language: pt
# Origem: swagger.yaml → /empresas, /empresas/{sessao}/cliente, /empresas/{sessao}/admin
# Schemas: EmpresaCreateDTO, EmpresaUpdateDTO, EmpresaUpdateAdminDTO, EmpresaResponseDTO
Funcionalidade: Contratos de empresa do SaaS
  Como cliente da API documentada em swagger.yaml
  Quero que POST e PUT usem DTOs isolados por intenção
  Para o front-end não receber a entidade JPA

  Cenário: POST /empresas cria com EmpresaCreateDTO e devolve EmpresaResponseDTO
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome            | Recanto Vista Alegre |
      | sessaoWhatsapp  | sessao_recanto_01    |
      | ramoDeAtuacao   | LOCACAO              |
      | precoBase       | 500.0                |
      | email           | cliente@recanto.com  |
      | senha           | SenhaCliente123      |
    Então o status HTTP deve ser 201
    E o corpo deve seguir o schema EmpresaResponseDTO
    E o campo "sessaoWhatsapp" deve ser "sessao_recanto_01"

  Cenário: POST /empresas rejeita token administrativo inválido
    Dado o token administrativo "token-invalido"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Recanto Vista Alegre |
      | sessaoWhatsapp | sessao_recanto_02    |
      | ramoDeAtuacao  | LOCACAO              |
      | email          | outro@recanto.com    |
      | senha          | SenhaCliente123      |
    Então o status HTTP deve ser 401

  Cenário: POST /empresas rejeita sessão WhatsApp já ocupada
    Dado uma empresa persistida com sessão "sessao_ocupada" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Outro Recanto  |
      | sessaoWhatsapp | sessao_ocupada |
      | ramoDeAtuacao  | LOCACAO        |
      | email          | ocupado@recanto.com |
      | senha          | SenhaCliente123     |
    Então o status HTTP deve ser 400

  Cenário: PUT /empresas/{sessao}/cliente atualiza só o perfil com EmpresaUpdateDTO
    Dado uma empresa persistida com sessão "sessao_cliente" e ramo "LOCACAO"
    Quando eu envio PUT /empresas/sessao_cliente/cliente com o EmpresaUpdateDTO:
      | nome                       | Recanto Vista Alegre Atualizado |
      | mensagemSaudacao           | Olá! Como posso ajudar?         |
      | linkGoogleMaps             | https://maps.app.goo.gl/exemplo |
      | permiteReservaAutomatica   | true                            |
    Então o status HTTP deve ser 200
    E o corpo deve seguir o schema EmpresaResponseDTO
    E o campo "nome" deve ser "Recanto Vista Alegre Atualizado"
    E o campo "linkGoogleMaps" deve ser "https://maps.app.goo.gl/exemplo"

  Cenário: PUT /empresas/{sessao}/admin exige token e usa EmpresaUpdateAdminDTO
    Dado uma empresa persistida com sessão "sessao_admin" e ramo "LOCACAO"
    E o token administrativo "sua-chave-admin"
    Quando eu envio PUT /empresas/sessao_admin/admin com o EmpresaUpdateAdminDTO:
      | locacaoPorHora | false |
    Então o status HTTP deve ser 200
    E o corpo deve seguir o schema EmpresaResponseDTO

  Cenário: PUT /empresas/{sessao}/admin sem token retorna 401
    Dado uma empresa persistida com sessão "sessao_admin_negado" e ramo "LOCACAO"
    Quando eu envio PUT /empresas/sessao_admin_negado/admin sem token com o EmpresaUpdateAdminDTO:
      | locacaoPorHora | true |
    Então o status HTTP deve ser 401
