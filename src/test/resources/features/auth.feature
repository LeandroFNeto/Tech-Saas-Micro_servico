# language: pt
# Origem: swagger.yaml → /auth/login, /auth/alterar-senha
# Schemas: LoginRequestDTO, LoginResponseDTO, AlterarSenhaDTO
Funcionalidade: Autenticação JWT do painel
  Como cliente da API documentada em swagger.yaml
  Quero autenticar com e-mail e senha e receber um JWT
  Para o front-end não usar usuários hardcoded

  Cenário: POST /auth/login com o master seedado devolve JWT de ADMIN
    Quando eu envio POST /auth/login com o LoginRequestDTO:
      | email | usuario@gamb.com |
      | senha | SenhaMaster123   |
    Então o status HTTP deve ser 200
    E o corpo deve seguir o schema LoginResponseDTO
    E o campo "role" deve ser "ADMIN"
    E o campo "email" deve ser "usuario@gamb.com"

  Cenário: POST /auth/login com senha inválida retorna 401
    Quando eu envio POST /auth/login com o LoginRequestDTO:
      | email | usuario@gamb.com |
      | senha | senha-errada     |
    Então o status HTTP deve ser 401

  Cenário: PUT /auth/alterar-senha com senha atual correta devolve 204
    Dado o token administrativo "sua-chave-admin"
    Quando eu envio POST /empresas com o EmpresaCreateDTO:
      | nome           | Espaço Troca Senha      |
      | sessaoWhatsapp | sessao_troca_senha      |
      | ramoDeAtuacao  | LOCACAO                 |
      | email          | troca@recanto.com       |
      | senha          | SenhaCliente123         |
    Quando eu envio POST /auth/login com o LoginRequestDTO:
      | email | sessao_troca_senha@gamb.com.br |
      | senha | SenhaCliente123                |
    Quando eu envio PUT /auth/alterar-senha com o JWT do login e o AlterarSenhaDTO:
      | senhaAtual | SenhaCliente123 |
      | novaSenha  | NovaSenha456    |
    Então o status HTTP deve ser 204

  Cenário: PUT /auth/alterar-senha sem JWT retorna 401
    Quando eu envio PUT /auth/alterar-senha sem JWT com o AlterarSenhaDTO:
      | senhaAtual | SenhaMaster123 |
      | novaSenha  | NovaSenha456   |
    Então o status HTTP deve ser 401

  Cenário: PUT /auth/alterar-senha com JWT inválido retorna 401
    Quando eu envio PUT /auth/alterar-senha com JWT inválido e o AlterarSenhaDTO:
      | senhaAtual | SenhaMaster123 |
      | novaSenha  | NovaSenha456   |
    Então o status HTTP deve ser 401

  Cenário: PUT /auth/alterar-senha com senha atual incorreta retorna 401
    Quando eu envio POST /auth/login com o LoginRequestDTO:
      | email | usuario@gamb.com |
      | senha | SenhaMaster123   |
    Quando eu envio PUT /auth/alterar-senha com o JWT do login e o AlterarSenhaDTO:
      | senhaAtual | senha-errada  |
      | novaSenha  | NovaSenha456  |
    Então o status HTTP deve ser 401

  Cenário: PUT /auth/alterar-senha com senha nova curta retorna 400
    Quando eu envio POST /auth/login com o LoginRequestDTO:
      | email | usuario@gamb.com |
      | senha | SenhaMaster123   |
    Quando eu envio PUT /auth/alterar-senha com o JWT do login e o AlterarSenhaDTO:
      | senhaAtual | SenhaMaster123 |
      | novaSenha  | 12345          |
    Então o status HTTP deve ser 400
