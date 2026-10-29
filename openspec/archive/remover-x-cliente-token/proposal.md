# remover-x-cliente-token

## Problema

O cabeçalho legado `x-cliente-token` autentica como cliente qualquer texto não vazio. O `JwtAuthenticationFilter` dá `ROLE_CLIENTE` a quem o envia, sem consultar o banco nem saber de qual empresa a pessoa é. O mesmo vale em `ControllerEmpresa` (`isClienteSemToken`) e em `ControllerWhatsapp` (`recusarSeNecessario`).

Resultado: qualquer pessoa na internet, enviando `x-cliente-token: a`, passa como cliente. Isso abre a busca de empresa por sessão e a leitura do status do WhatsApp. Abre também o início de uma sessão de WhatsApp de qualquer empresa. Mesmo com o JWT correto, as rotas do WhatsApp não conferem se a sessão pertence à empresa do token: um cliente da empresa A pode consultar e iniciar a sessão da empresa B.

O front atual não envia `x-cliente-token`; ele usa o JWT do login. O cabeçalho só serve a quem sabe que ele existe.

## O que muda

### Entra

- Rotas que hoje aceitam `x-cliente-token` passam a exigir o JWT de um usuário `CLIENTE` ou `ADMIN`.
- Rotas do WhatsApp por sessão (status e iniciar): o cliente só age na sessão da própria empresa. Cliente de outra empresa recebe 403; sessão inexistente, 404 só para o admin (para o cliente, 403, sem revelar se existe). O admin continua podendo todas.
- Leitura de empresa por sessão (`findBySessaoWhatsapp`): o cliente só lê a própria empresa; o admin lê todas.

### Altera

- `JwtAuthenticationFilter`: deixa de autenticar pelo `x-cliente-token`. O `x-admin-token` (chave da API) continua, por ser a ferramenta da equipe.
- `ControllerEmpresa` e `ControllerWhatsapp`: perdem o parâmetro `x-cliente-token` e os métodos `isClienteSemToken` e a parte de cliente de `recusarSeNecessario`.
- Contrato YAML: o parâmetro `x-cliente-token` sai de todos os `paths`. `CorsConfig` deixa de permitir o cabeçalho.
- Cliente que ainda use o cabeçalho recebe 401.

### Sai

- O cabeçalho `x-cliente-token` e o papel implícito `cliente-token`.

## Fora de escopo

- Trocar o `x-admin-token` por login de admin. É decisão à parte.
- Rate limit de login e rotação de JWT.
- Rotas novas das changes `lembrar-confirmacao-reserva` e `exibir-previsao-tempo-reservas`, que já exigem o token da própria empresa.

## Domínios

- spec: `openspec/specs/auth/spec.md` (AUTH-04 acesso do cliente só à própria empresa)
- spec: `openspec/specs/whatsapp/spec.md` (WPP-04, só leitura: a regra de conexão não muda, muda quem pode chamar)
- spec: `openspec/specs/empresa/spec.md` (EMP-04, só leitura: busca por sessão)

Pode entrar a qualquer hora, antes ou depois das outras changes. Mexe em `ControllerEmpresa`, `ControllerWhatsapp` e `JwtAuthenticationFilter`, então não deve rodar junto com `corrigir-reserva-automatica` nem com `lembrar-confirmacao-reserva` na mesma janela de edição.
