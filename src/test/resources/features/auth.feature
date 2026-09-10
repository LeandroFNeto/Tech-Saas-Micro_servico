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
