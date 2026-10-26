# Empresa

Comportamento observável do cadastro e do perfil da empresa. Cada intenção HTTP usa um DTO próprio. A resposta de criação e de atualização é `EmpresaResponseDTO`.

## Requisitos

### EMP-01 Criar empresa

O sistema deve cadastrar uma empresa a partir de `EmpresaCreateDTO` e devolver `EmpresaResponseDTO`. Exige `x-admin-token`. A sessão WhatsApp é única.

Contrato: `especificacao/back/paths/empresas.yaml`
Prova: `src/test/resources/features/empresas.feature`

Cenário: admin cria a empresa

- Dado o token administrativo válido
- Quando o admin envia POST `/empresas` com nome, sessão, ramo, e-mail e senha
- Então a API responde 201
- E o corpo segue `EmpresaResponseDTO`
- E a sessão devolvida é a que foi enviada

Cenário: token administrativo inválido

- Quando o POST `/empresas` chega com token inválido
- Então a API responde 401

Cenário: sessão WhatsApp já ocupada

- Dado uma empresa já persistida com aquela sessão
- Quando o admin tenta criar outra empresa com a mesma sessão
- Então a API responde 400

### EMP-02 Cliente atualiza o próprio perfil

O sistema deve aplicar `EmpresaUpdateDTO` na sessão da empresa: textos, links e aparência. Essa rota não altera cobrança nem infraestrutura.

Contrato: `especificacao/back/paths/empresas-cliente.yaml`
Conteúdo da tela: `especificacao/front/cliente-perfil.content.json`
Prova: `src/test/resources/features/empresas.feature`

Cenário: perfil do cliente é atualizado

- Dado uma empresa persistida na sessão
- Quando o cliente envia PUT `/empresas/{sessao}/cliente` com nome, saudação, link do mapa e reserva automática
- Então a API responde 200
- E o corpo segue `EmpresaResponseDTO`
- E nome e link do mapa devolvidos são os enviados

### EMP-03 Admin atualiza a infraestrutura

O sistema deve aplicar `EmpresaUpdateAdminDTO` (sessão, tipo de locação, agenda Google e módulos). Exige `x-admin-token`.

Contrato: `especificacao/back/paths/empresas-admin.yaml`
Prova: `src/test/resources/features/empresas.feature`

Cenário: admin atualiza com token

- Dado uma empresa persistida e o token administrativo válido
- Quando o admin envia PUT `/empresas/{sessao}/admin`
- Então a API responde 200
- E o corpo segue `EmpresaResponseDTO`

Cenário: admin sem token

- Quando o PUT `/empresas/{sessao}/admin` chega sem token
- Então a API responde 401

### EMP-04 Admin redefine a senha do cliente

O sistema deve gravar um hash novo da senha do cliente daquela sessão, sem conferir a senha atual. Exige `x-admin-token`.

Contrato: `especificacao/back/paths/empresas-reset-senha.yaml` (`ResetSenhaAdminDTO`)
Prova: `src/test/java/com/example/demo/servico/ServicoAuthResetSenhaTest.java` (serviço) e `src/test/resources/features/empresas.feature` (HTTP 204, 400, 401 e 404).

Cenário: reset grava hash novo

- Dado um usuário ligado à sessão da empresa
- Quando o admin redefine a senha dessa sessão
- Então a senha persistida é o hash da senha nova
- E a senha anterior não é consultada

## Superfície HTTP ainda sem requisito

GET `/empresas` (coleção HAL, token admin) e o item Spring Data REST em `especificacao/back/paths/empresas-id.yaml` existem no contrato. Eles ficam de fora destes requisitos até uma change dizer o que o painel deve observar ali. O comportamento de produto desta spec é o das rotas com DTO nomeado.
