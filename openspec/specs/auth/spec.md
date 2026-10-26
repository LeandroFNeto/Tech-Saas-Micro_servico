# Auth

Comportamento observável do login e da senha do painel. A forma HTTP fica no contrato; esta spec não copia campos de DTO.

## Requisitos

### AUTH-01 Login com JWT

O sistema deve validar e-mail e senha e devolver um JWT com o papel do usuário. A rota é pública.

Contrato: `especificacao/back/paths/auth-login.yaml` (`LoginRequestDTO`, `LoginResponseDTO`)
Prova: `src/test/resources/features/auth.feature`

Cenário: master seedado entra como ADMIN

- Dado o usuário master `usuario@gamb.com`
- Quando o painel envia POST `/auth/login` com a senha correta
- Então a API responde 200
- E o corpo segue `LoginResponseDTO`
- E o papel é `ADMIN`
- E o e-mail devolvido é o do master

Cenário: senha inválida

- Quando o painel envia POST `/auth/login` com senha errada para o master
- Então a API responde 401

### AUTH-02 Alterar a própria senha

O sistema deve trocar a senha do usuário autenticado. Exige JWT Bearer. A senha nova é persistida como hash BCrypt.

Contrato: `especificacao/back/paths/auth-alterar-senha.yaml` (`AlterarSenhaDTO`)
Prova: `src/test/resources/features/auth.feature`

Cenário: senha atual correta

- Dado um JWT válido
- Quando o usuário envia PUT `/auth/alterar-senha` com a senha atual e uma senha nova válida
- Então a API responde 204

Cenário: credencial ou senha atual inválida

- Quando o JWT está ausente ou é inválido, ou a senha atual não confere
- Então a API responde 401

Cenário: senha nova inválida

- Dado um JWT válido
- Quando a senha nova não passa na validação
- Então a API responde 400
