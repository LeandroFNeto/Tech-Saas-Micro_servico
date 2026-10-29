# Design

## Decisão

1. **Sem cabeçalho de cliente.** `autenticarPorHeadersLegados` mantém só o `x-admin-token`. O bloco do `x-cliente-token` é removido. Sem JWT válido e sem chave de admin, a rota protegida responde 401.
2. **Dono da sessão.** Método único (por exemplo `ServicoAcessoEmpresa.podeAcessarSessao(autenticacao, sessao)`) usado pelos dois controllers: admin passa; cliente passa só se `Usuario.empresa.sessaoWhatsapp` for igual à `{sessao}`. Fica num lugar só para as rotas novas das outras changes reaproveitarem.
3. **Sem vazar existência.** Cliente que pede sessão de outra empresa ou inexistente recebe o mesmo 403. 404 só aparece para o admin.
4. **Busca por sessão.** `GET /empresas/search/findBySessaoWhatsapp` segue a mesma regra: cliente só a própria empresa, com o DTO sem `googleCalendarId` (já é assim para não-admin).
5. **Compatibilidade.** O front usa JWT; nenhuma tela muda. Integração externa que use o cabeçalho quebra de propósito, e o 401 é a resposta esperada.

## Arquivos

- Contrato: remover o parâmetro `x-cliente-token` de `especificacao/back/paths/` (rotas de empresa e de WhatsApp) e deixar 403 nas rotas por sessão.
- Java:
  - `security/JwtAuthenticationFilter.java` (remove o bloco do cliente)
  - `controller/ControllerEmpresa.java` (remove parâmetro e `isClienteSemToken`; dono da empresa)
  - `controller/ControllerWhatsapp.java` (remove parâmetro; `recusarSeNecessario` usa o dono da sessão)
  - `servico/ServicoAcessoEmpresa.java` (novo) e `config/CorsConfig.java` (tira o cabeçalho)
- Prova:
  - `ControllerEmpresaTest` e `ControllerWhatsappTest`: `x-cliente-token` sozinho dá 401; JWT de cliente na própria sessão 200; em sessão de outra empresa 403; sessão inexistente 403 para cliente e 404 para admin; admin passa em todas
  - `JwtAuthenticationFilterTest`: o cabeçalho não autentica
  - `empresas.feature` e `whatsapp.feature`: cenários acima

## Alinhamento

- `00-architecture.md` (Spring Security com JWT)
- YAML antes do Java

## Riscos

- **Quem usa o cabeçalho fora do front quebra.** Confirmar com a equipe se há script ou integração antes de entrar.
- **Cliente sem `empresa` vinculada.** Usuário `CLIENTE` sem empresa não acessa sessão nenhuma. É o comportamento seguro, mas o cadastro precisa garantir o vínculo.
- **`x-admin-token` continua sendo chave compartilhada.** Quem a tem é admin total. Fica fora desta change.
