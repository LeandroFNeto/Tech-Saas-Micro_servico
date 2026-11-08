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

### AUTH-06 Limite de tentativas de login

O sistema deve aceitar no máximo 5 falhas de login do mesmo e-mail e do mesmo IP em 15 minutos. Na sexta tentativa, mesmo com a senha certa, a API responde 429 com o cabeçalho `Retry-After` em segundos até a janela passar. O login certo dentro do limite zera a contagem daquele e-mail e IP. O IP só vem do `X-Forwarded-For` quando a conexão chega de um proxy listado em `PROXIES_CONFIAVEIS` (IP ou CIDR); fora disso vale o endereço da conexão, e a mesma regra serve ao limite do assistente público. O log de login recusado ou bloqueado traz o e-mail só mascarado e nunca a senha. A contagem fica em memória: reinício zera, e cada instância conta à parte.

Contrato: `especificacao/back/paths/auth-login.yaml`, `especificacao/back/schemas/auth/ErroDTO.yaml`
Prova: `src/test/java/com/example/demo/controller/ControllerAuthTest.java`, `src/test/java/com/example/demo/servico/LimiteTentativasLoginTest.java`, `src/test/java/com/example/demo/servico/IpDoClienteTest.java`, `src/test/java/com/example/demo/config/LogsSemDadoSensivelTest.java`, `src/test/resources/features/auth.feature`

Cenário: senha errada repetida
- Dado um e-mail que errou a senha 5 vezes do mesmo IP em 15 minutos
- Quando envia POST `/auth/login` de novo, mesmo com a senha certa
- Então a API responde 429 com `Retry-After`

Cenário: login certo zera a contagem
- Dado um e-mail que errou a senha 3 vezes
- Quando o login dá certo
- Então a contagem de falhas desse e-mail e IP volta a zero

Cenário: IP atrás de proxy confiável
- Dado um pedido que chega de um endereço listado em `PROXIES_CONFIAVEIS`
- Quando o limite é calculado
- Então o IP usado é o do `X-Forwarded-For`
- E, vindo de endereço fora da lista, o cabeçalho é ignorado

Cenário: e-mail fora do log
- Dado um login recusado ou bloqueado
- Quando o sistema registra o evento
- Então o log traz o e-mail mascarado e não traz a senha

### AUTH-07 Nenhuma conta com senha conhecida

O sistema deve criar as contas sem senha conhecida e falhar fechado nos segredos de acesso. A conta de cliente que o boot cria para empresa já cadastrada recebe senha aleatória de 32 caracteres, que não é guardada nem aparece no log; o cliente entra depois que o admin redefine a senha. Com `SENHA_LEGADA_A_BLOQUEAR` definida no boot, toda conta de cliente cuja senha confere com esse valor recebe uma senha aleatória, e o log traz só a quantidade e os e-mails mascarados. Sem `ADMIN_DEFAULT_PASSWORD`, o admin inicial não é criado e o log avisa; com menos de 12 caracteres, a aplicação não sobe. Sem `JWT_SECRET_KEY`, ou com menos de 32 bytes, a aplicação não sobe. Senha nova (troca pelo cliente, redefinição pelo admin e cadastro de empresa) tem 10 caracteres ou mais, no servidor e no front.

Contrato: `especificacao/back/schemas/auth/AlterarSenhaDTO.yaml`, `especificacao/back/schemas/empresa/ResetSenhaAdminDTO.yaml`, `especificacao/back/schemas/empresa/EmpresaCreateDTO.yaml`
Prova: `src/test/java/com/example/demo/config/AdminInitializerTest.java`, `src/test/java/com/example/demo/config/LogsSemDadoSensivelTest.java`, `src/test/java/com/example/demo/servico/TokenServiceTest.java`, `src/test/java/com/example/demo/controller/ControllerAuthTest.java`, `src/test/java/com/example/demo/controller/ControllerEmpresaTest.java`, `src/test/resources/features/auth.feature`, `perfil.component.spec.ts`, `empresa-form.component.spec.ts`. O uso único de `SENHA_LEGADA_A_BLOQUEAR` em produção é prova operacional (log do boot no deploy).

Cenário: seed de cliente legado
- Dado uma empresa cadastrada sem usuário cliente
- Quando o boot cria a conta
- Então a senha é aleatória e não aparece no log
- E o cliente só entra depois de o admin definir a senha

Cenário: conta com a senha antiga
- Dado `SENHA_LEGADA_A_BLOQUEAR` definida no boot
- Quando uma conta de cliente confere com esse valor
- Então a senha é trocada por uma aleatória
- E o log traz só a quantidade e os e-mails mascarados

Cenário: admin inicial sem senha configurada
- Dado `ADMIN_DEFAULT_PASSWORD` vazia
- Quando a aplicação sobe
- Então o admin inicial não é criado e o log avisa
- E, com menos de 12 caracteres, a aplicação não sobe

Cenário: chave do JWT
- Dado `JWT_SECRET_KEY` ausente ou com menos de 32 bytes
- Quando a aplicação sobe
- Então a subida falha

Cenário: senha nova curta
- Dado uma senha nova com menos de 10 caracteres
- Quando envia a troca, a redefinição ou o cadastro
- Então a API responde 400

### AUTH-08 Superfície HTTP mínima

O sistema deve responder erro sem mensagem interna nem rastro de pilha: o corpo de erro traz só `message`, com o texto do próprio sistema para recusa pensada ao usuário, uma descrição curta da regra violada na validação e "Erro interno" no 500. A API aceita só as origens de CORS configuradas em `CORS_ALLOWED_ORIGINS`; curinga é ignorado com aviso no log e lista vazia não aceita origem externa. A documentação da API (`/v3/api-docs` e `/swagger-ui`) fica fechada enquanto `SWAGGER_HABILITADO` não for `true`. O cabeçalho legado `x-admin-token` é comparado em tempo constante.

Contrato: `especificacao/back/schemas/auth/ErroDTO.yaml` (respostas de erro de todos os paths)
Prova: `src/test/java/com/example/demo/controller/TratadorDeErrosTest.java`, `src/test/java/com/example/demo/security/SecurityConfigTest.java`, `src/test/java/com/example/demo/security/SecurityConfigSwaggerLigadoTest.java`, `src/test/java/com/example/demo/config/CorsConfigTest.java`, `src/test/java/com/example/demo/security/JwtAuthenticationFilterTest.java`, `src/test/java/com/example/demo/config/LogsSemDadoSensivelTest.java`

Cenário: exceção interna
- Dado uma rota que lança exceção não tratada
- Quando o pedido é atendido
- Então a API responde 500 com "Erro interno", sem a mensagem nem o rastro da exceção

Cenário: erro pensado para o usuário
- Dado uma rota que recusa o pedido com mensagem do próprio sistema
- Quando o pedido é atendido
- Então o corpo traz essa mensagem em `message`, que o front mostra

Cenário: CORS
- Dado um navegador de origem fora de `CORS_ALLOWED_ORIGINS`
- Quando chama a API
- Então a origem é recusada
- E `*` na configuração é ignorado com aviso no log

Cenário: documentação fechada
- Dado `SWAGGER_HABILITADO` diferente de `true`
- Quando alguém pede `/v3/api-docs` ou `/swagger-ui`
- Então a API responde 401

