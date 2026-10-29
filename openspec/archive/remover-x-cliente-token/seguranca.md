# Segurança: remover-x-cliente-token

Revisor: claude-opus-5-5-medium / Antigravity. Veredito: **aprovado**.

## Análise de Segurança dos Arquivos Tocados

1. **`JwtAuthenticationFilter.java`**:
   - `x-cliente-token` completamente removido de `autenticarPorHeadersLegados`.
   - Apenas o `x-admin-token` configurado é aceito para tarefas administrativas da plataforma.
   - Requisições sem credenciais válidas não recebem papel algum (resultando em 401).

2. **`ServicoAcessoEmpresa.java`**:
   - Regra estrita de autorização:
     - Admin acessa qualquer sessão; sessão inexistente retorna 404.
     - Cliente autenticado só acessa a sessão da própria empresa vinculada (`sessao.equals(usuario.getEmpresa().getSessaoWhatsapp())`).
     - Tentativas de acessar sessão inexistente ou pertencente a outra empresa resultam no mesmo 403, sem enumeração de sessões ou vazamento de existência.
   - Nenhuma credencial ou dado sensível é exposto.

3. **`ControllerEmpresa.java` e `ControllerWhatsapp.java`**:
   - Parâmetro `x-cliente-token` eliminado das assinaturas e das anotações Swagger.
   - Validações delegadas ao `ServicoAcessoEmpresa`.

4. **`CorsConfig.java`**:
   - Removido `x-cliente-token` dos cabeçalhos permitidos.

Veredito: **Aprovado** para seguir para a etapa de Revisão.
