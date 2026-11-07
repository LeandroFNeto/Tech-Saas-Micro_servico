# language: pt
Funcionalidade: Gestão e proteção de contatos do cliente com conformidade LGPD
  Como cliente autenticado do SaaS
  Quero consultar a lista mascarada de contatos e gerenciar o ciclo de vida
  Para atender clientes com segurança e garantia contra vazamentos em massa

  Cenário: Listar contatos mascarados com JWT do próprio cliente retorna 200
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Empresa Contatos Teste |
      | sessaoWhatsapp | sessao_contatos_propria |
      | ramoDeAtuacao  | LOCACAO                |
      | email          | contatos@teste.com     |
      | senha          | SenhaCliente123        |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_contatos_propria@gamb.com.br |
      | senha | SenhaCliente123                     |
    E eu envio GET /empresas/sessao_contatos_propria/contatos com o JWT do login
    Então o status HTTP deve ser 200

  Cenário: Listar contatos de outra empresa com JWT de cliente retorna 403
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Empresa Contatos Alvo   |
      | sessaoWhatsapp | sessao_contatos_alvo    |
      | ramoDeAtuacao  | LOCACAO                 |
      | email          | contatos_alvo@teste.com |
      | senha          | SenhaCliente123         |
    E eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Empresa Contatos Intruso   |
      | sessaoWhatsapp | sessao_contatos_intruso    |
      | ramoDeAtuacao  | LOCACAO                    |
      | email          | contatos_intruso@teste.com |
      | senha          | SenhaCliente123            |
    E eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_contatos_intruso@gamb.com.br |
      | senha | SenhaCliente123                     |
    E eu envio GET /empresas/sessao_contatos_alvo/contatos com o JWT do login
    Então o status HTTP deve ser 403

  Cenário: Listar contatos sem autenticação retorna 401
    Quando eu envio GET /empresas/qualquer_sessao/contatos sem token
    Então o status HTTP deve ser 401
