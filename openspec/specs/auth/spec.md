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


### AUTH-04 Acesso do cliente restrito à própria empresa

O sistema deve rejeitar requisições com o cabeçalho legado `x-cliente-token` com 401 em rotas protegidas. Para rotas por sessão (empresa e WhatsApp), o usuário com papel `CLIENTE` autenticado por JWT só pode ler ou alterar dados da sessão pertencente à sua própria empresa. Se o cliente tentar acessar sessão de outra empresa ou uma sessão inexistente, a API responde 403 sem revelar se a sessão existe. O usuário `ADMIN` pode acessar qualquer sessão (e recebe 404 se a sessão não existir).

Contrato: `especificacao/back/paths/whatsapp-status.yaml`, `especificacao/back/paths/whatsapp-iniciar.yaml`, `especificacao/back/paths/empresas-cliente.yaml`, `especificacao/back/paths/empresas-previsao.yaml`, `especificacao/back/paths/empresas-search.yaml`
Prova: `src/test/java/com/example/demo/security/JwtAuthenticationFilterTest.java`, `src/test/java/com/example/demo/controller/ControllerWhatsappTest.java`, `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`, `src/test/resources/features/whatsapp.feature`, `src/test/resources/features/empresas.feature`

Cenário: token legado rejeitado
- Dado uma requisição enviando o cabeçalho `x-cliente-token`
- Quando a rota protegida é chamada
- Então a API responde 401

Cenário: cliente na própria sessão
- Dado um usuário `CLIENTE` autenticado via JWT pertencente à empresa da sessão
- Quando ele acessa rotas de status ou iniciar do WhatsApp, ou dados da sua empresa
- Então a API responde 200

Cenário: cliente em sessão de outra empresa ou inexistente
- Dado um usuário `CLIENTE` autenticado via JWT
- Quando ele tenta acessar rotas de sessão de outra empresa ou uma sessão inexistente
- Então a API responde 403

Cenário: admin em sessão inexistente
- Dado um usuário `ADMIN` autenticado
- Quando ele tenta acessar rotas de uma sessão que não existe
- Então a API responde 404

### AUTH-05 Níveis Administrativos e Controle Granular

O sistema suporta papéis administrativos com permissões escalonadas:
1. `SUPER_ADMIN` (e `ADMIN` legado): acesso total (excluir empresas, resetar senhas de clientes, gerenciar avisos do sistema, cadastrar e editar empresas).
2. `ADMIN_OPERACIONAL`: operação diária (cadastrar empresas, editar dados e configurações), porém proibido de excluir empresas, resetar senhas e alterar avisos (retorna 403 Forbidden).
3. `ADMIN_LEITURA`: auditoria e suporte (somente visualização de empresas, status e avisos), proibido de qualquer mutação (POST/PUT/DELETE retornam 403 Forbidden).

Contrato: `especificacao/back/schemas/auth/LoginResponseDTO.yaml`, `especificacao/back/paths/empresas-sessao.yaml`, `especificacao/back/paths/admin-avisos.yaml`, `especificacao/back/paths/admin-avisos-id.yaml`
Prova: `src/test/java/com/example/demo/servico/ServicoAcessoEmpresaNiveisTest.java`, `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`, `src/test/java/com/example/demo/controller/ControllerAvisoTest.java`

Cenário: exclusão de empresa restrita ao super admin
- Dado um usuário autenticado como `ADMIN_OPERACIONAL` ou `ADMIN_LEITURA`
- Quando envia DELETE `/empresas/{sessao}`
- Então a API responde 403 Forbidden

Cenário: criação de empresa proibida para admin de leitura
- Dado um usuário autenticado como `ADMIN_LEITURA`
- Quando envia POST `/empresas`
- Então a API responde 403 Forbidden

Cenário: gestão de avisos restrita ao super admin
- Dado um usuário autenticado como `ADMIN_OPERACIONAL` ou `ADMIN_LEITURA`
- Quando envia POST, PUT ou DELETE para `/admin/avisos`
- Então a API responde 403 Forbidden

