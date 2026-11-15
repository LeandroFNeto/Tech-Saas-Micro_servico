# endurecer-login-e-gateway

## Problema

A change `fechar-pendencias-de-seguranca` foi publicada e aprovada com ressalvas não bloqueantes. Elas continuam abertas no código de produção:

- **Chaves de contatos expostas.** `CONTATOS_CHAVE_CRIPTO` e `CONTATOS_CHAVE_INDICE` passaram por um chat. A regra 2.5 de `seguranca.mdc` manda trocar o segredo, mas não existe rotina que decifre com a chave antiga e cifre com a nova. Por isso o `atualizar-env-producao.sh` deixa essas duas chaves como pendência.
- **Enumeração de e-mail por tempo.** `ServicoAuth.login` só roda BCrypt quando o e-mail existe, e a resposta de e-mail inexistente sai bem mais rápido.
- **Limite de login sem teto rígido.** Em `LimiteTentativasLogin`, passar de 10.000 entradas só dispara uma varredura completa do mapa a cada falha. Entradas ainda válidas não saem, então a memória cresce e cada falha fica mais cara.
- **Entrada do login sem tamanho máximo.** `LoginRequestDTO` e o YAML não limitam e-mail nem senha, e esses textos viram chave do mapa em memória.
- **DNS no tratamento de IP.** `IpDoCliente.bytesDe` usa `InetAddress.getByName`. Um texto como `cafe.de` passa pelo filtro de caracteres e dispara consulta DNS. Formas antigas como `1.2.3` são aceitas como IP.
- **API publicada em todas as interfaces.** `docker-compose.yml` publica `8080:8080`, o que permite chegar à API sem passar pelo proxy com HTTPS (regra 10.1).
- **Bloqueio forçado de login.** O limite conta por e-mail e IP. Quem está no mesmo IP da vítima consegue bloquear o login dela. Com `PROXIES_CONFIAVEIS` errado, todos os visitantes dividem o IP do proxy e qualquer pessoa consegue bloquear qualquer conta.
- **Gateway com erro bruto.** `deviceController.ts` tem 52 `logger.error` com o objeto do erro e devolve esse objeto na resposta. `descreverErroSeguro` copia `name` e `code` sem limite de tamanho nem de caracteres.
- **Número da Gamb sem validação.** `atualizar-env-producao.sh` grava qualquer texto em `GAMB_CONTATO_WHATSAPP`.

## O que muda

### Entra
- Rotação das chaves de contatos em um boot, com `CONTATOS_CHAVE_CRIPTO_ANTIGA` e `CONTATOS_CHAVE_INDICE_ANTIGA`, retomável e antes de a API aceitar tráfego.
- `scripts/rotacionar-chaves-contatos.sh`, que gera as chaves novas e move as atuais para as variáveis `_ANTIGA`, sem exibir valor.
- Comparação BCrypt contra um hash de referência quando o e-mail não existe.
- Teto de 30 falhas de login por IP em 15 minutos, resposta 429, para compensar o custo de BCrypt que o item acima cria.
- Teto rígido de 10.000 entradas no limite de login, descartando as mais antigas.
- Parser próprio de IP literal, sem DNS.
- Aviso no boot quando `PROXIES_CONFIAVEIS` está vazia.

### Altera
- `LoginRequestDTO`: e-mail com até 254 caracteres e senha com até 128, no DTO e no YAML, com resposta 400 documentada.
- Senha nova (`AlterarSenhaDTO`, `ResetSenhaAdminDTO` e `EmpresaCreateDTO`) com até 128 caracteres, para nenhuma senha aceita ficar impedida de entrar.
- `docker-compose.yml`: API em `127.0.0.1:8080:8080`, depois da conferência no servidor descrita no design.
- `PROXIES_CONFIAVEIS`: IP exato do proxy, no servidor, no `.env.example` e no script. O script deixa de usar a faixa inteira da rede como padrão.
- `descreverErroSeguro`: `name` e `code` com lista de caracteres e tamanho máximo.
- `deviceController.ts`: log e resposta sem o objeto do erro.
- `atualizar-env-producao.sh`: número da Gamb só com dígitos, começando em 55, com 12 ou 13 dígitos.

### Sai
- A varredura completa do mapa em `LimiteTentativasLogin`.
- O uso de `InetAddress.getByName` em `IpDoCliente`.
- As chaves padrão escritas nas anotações `@Value` de `CriptografiaCampo`.
- A pendência das chaves de contatos em `atualizar-env-producao.sh`, que passa a apontar para o script novo.

## Fora de escopo

- Os outros controllers do fork com o mesmo padrão de erro bruto: `groupController` (23 ocorrências), `sessionController` (12), `communityController` (7) e 5 controllers com 1 cada. O fork será substituído pela change aprovada `migrar-wppconnect-para-baileys`. Fica registrado nos Riscos.
- Desbloqueio manual de login pelo painel. Foi avaliado no design e não compensa nesta escala.
- Trocar o JWT do `localStorage` por cookie `httpOnly`, já registrado como exceção na change anterior.
- Rotação automática ou periódica de chaves. Aqui a rotação é uma operação manual.
- Reciframento de backups antigos, que fica nos Riscos.

## Domínios
- spec: `openspec/specs/auth/spec.md`
- spec: `openspec/specs/contatos/spec.md`
- spec: `openspec/specs/whatsapp/spec.md`
- spec: `openspec/specs/desenvolvimento/spec.md`
- regra: `.cursor/rules/seguranca.mdc`
