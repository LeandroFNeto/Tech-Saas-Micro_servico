# Tarefas

## 1. Conferência no servidor (pessoa, sem alterar nada)
- [ ] 1.1 Conferir qual processo atende as portas 443 e 80, para onde encaminha a API e qual caso da tabela da decisão 6 vale; registrar o caso nesta tarefa, sem IP público nem segredo

## 2. Contrato
- [ ] 2.1 `LoginRequestDTO.yaml` com `maxLength` 254 e 128; `AlterarSenhaDTO.yaml`, `ResetSenhaAdminDTO.yaml` e `EmpresaCreateDTO.yaml` com `maxLength: 128` nas senhas; `auth-login.yaml` com a resposta 400 e o 429 cobrindo o teto por IP

## 3. Login
- [ ] 3.1 DTOs com `@Size` iguais ao YAML; verificar com `ControllerAuthTest` (400 acima do tamanho) e cenário em `auth.feature`
- [ ] 3.2 `ServicoAuth`: BCrypt contra o hash de referência quando o e-mail não existe, mesmo 401; verificar com `ServicoAuthTest` (`matches` chamado uma vez com e-mail inexistente)
- [ ] 3.3 `LimiteTentativasLogin`: mapa com teto de 10.000, descarte da falha mais antiga, sem varredura completa, fila limitada a `MAXIMO_FALHAS`; verificar com `LimiteTentativasLoginTest`
- [ ] 3.4 Teto de 30 falhas por IP em 15 minutos, conferido antes do BCrypt, só com `PROXIES_CONFIAVEIS` preenchida; verificar com `LimiteTentativasLoginTest` e `ControllerAuthTest` (429 com `Retry-After`, e-mails diferentes do mesmo IP)
- [ ] 3.5 `LiteralDeIp` e uso em `IpDoCliente`, sem `InetAddress.getByName`, com aviso no boot para lista vazia; verificar com `LiteralDeIpTest` e `IpDoClienteTest` (`cafe.de`, `1.2.3`, `010.0.0.1`, `::ffff:10.0.0.1`, IPv6 comprimido, zona `%`)

## 4. Chaves de contatos
- [ ] 4.1 `RotacaoChavesContatos` com as variáveis `_ANTIGA`, antes de o servidor web subir, por `JdbcTemplate`, uma transação por linha; verificar com `RotacaoChavesContatosTest` em banco real de teste (rotação de `contatos` e `reservas`, índice recalculado, busca pelo índice novo, retomada, valor sem `v1:`, chave errada interrompe, configuração incompleta não sobe, log só com quantidades, tabela e `id`)
- [ ] 4.2 `CriptografiaCampo` sem chave padrão no `@Value`; `application.properties`, `docker-compose.yml` e `.env.example` com as variáveis `_ANTIGA` sem valor; verificar com `CriptografiaCampoTest` e `ServicoContatoTest`
- [ ] 4.3 `scripts/rotacionar-chaves-contatos.sh`: cópia do `.env` com `chmod 600`, chaves atuais movidas para `_ANTIGA`, chaves novas geradas (AES com 32 bytes em base64), recusa se já houver `_ANTIGA`, nenhum valor na tela; `atualizar-env-producao.sh` aponta para ele; verificar contra um `.env` temporário

## 5. Infra e script
- [ ] 5.1 `docker-compose.yml` com `127.0.0.1:8080:8080` e `docker-compose.override.example.yml` conforme o caso da tarefa 1.1; verificar com `docker compose config`
- [ ] 5.2 `.env.example` com IP único em `PROXIES_CONFIAVEIS`; `atualizar-env-producao.sh` sem gravar a faixa inteira no Enter; verificar contra um `.env` temporário
- [ ] 5.3 `atualizar-env-producao.sh` com validação do número da Gamb e pendência para valor inválido já gravado; verificar contra um `.env` temporário com entrada inválida e válida

## 6. Gateway
- [ ] 6.1 `descreverErroSeguro` com lista de caracteres, tamanhos máximos e `status` inteiro; verificar em `wppconnect-server/src/tests` (nome longo, caracteres de controle, número de telefone em `code`)
- [ ] 6.2 `deviceController.ts` sem o objeto do erro no log e na resposta; verificar em `wppconnect-server/src/tests` capturando log e corpo

## 7. Specs
- [ ] 7.1 Gravar os requisitos desta change em `openspec/specs/auth/spec.md`, `contatos/spec.md`, `whatsapp/spec.md` e `desenvolvimento/spec.md`, com a linha `Prova:` dos testes que existirem

## 8. Deploy e encerramento
- [ ] 8.1 Antes do deploy, aplicar no servidor a rede ou a porta do caso da tarefa 1.1 e preencher `PROXIES_CONFIAVEIS` com o IP exato
- [ ] 8.2 Publicar e conferir o site pelo HTTPS, `curl` na porta 8080 pelo IP público sem resposta e um login errado contando o IP real do cliente
- [ ] 8.3 Rotacionar as chaves na ordem da decisão 10, conferir as quantidades no log, a lista de contatos no painel e apagar as variáveis `_ANTIGA`, as cópias do `.env` e o dump anterior
- [ ] 8.4 Mover esta pasta para `openspec/archive/endurecer-login-e-gateway/`
