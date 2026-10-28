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

### AUTH-03 Página pública de apresentação

O sistema deve exibir em `/` uma página pública que apresenta o gamb, sem consumir API, com o link Entrar que leva a `/login`. Quem já tem sessão válida vai direto ao painel do seu papel. Rota desconhecida abre a apresentação.

Contrato: nenhum. Rota do front, sem rota de API.
Prova: `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts`

Cenário: visitante sem sessão

- Dado um visitante sem sessão
- Quando abre `/`
- Então vê a apresentação
- E o link Entrar leva a `/login`

Cenário: usuário com sessão válida

- Dado um usuário com sessão válida
- Quando abre `/`
- Então é levado ao painel do seu papel

Cenário: rota desconhecida

- Dado qualquer visitante
- Quando abre um endereço que não existe no painel
- Então é levado à apresentação em `/`
