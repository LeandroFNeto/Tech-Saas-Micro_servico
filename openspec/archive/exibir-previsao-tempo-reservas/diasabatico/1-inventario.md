# Inventário: exibir-previsao-tempo-reservas
Só leitura. Nenhum arquivo de produto alterado.
## Rotas (app.routes, admin.routes, client.routes)
| Rota | Componente | Guard |
|---|---|---|
| `''` | ApresentacaoComponent | nenhum |
| `login` | LoginComponent | nenhum |
| `admin` > `''` | DashboardComponent (Shell) | authGuard + ADMIN |
| `admin/empresas/nova` e `:sessao` | EmpresaFormComponent (Shell) | authGuard + ADMIN |
| `admin/avisos` | GestaoAvisosComponent (Shell) | authGuard + ADMIN |
| `cliente` > `''` | PerfilComponent (Shell) | authGuard + CLIENTE |
| `design-v0` | V0DashboardComponent | nenhum |
| `**` | redireciona para `''` | nenhum |
## Links (destino)
| Origem | Destino |
|---|---|
| shell: marca e menu admin (lateral e abas) | `/admin`, `/cliente`, `/admin/empresas/nova`, `/admin/avisos` |
| shell: menu cliente (só desktop) | `/cliente` ("Meu perfil") |
| dashboard | `/admin/empresas/nova`, `/admin/empresas/:sessaoWhatsapp` |
| empresa-form | `/admin` |
| login | `/` (marca e Voltar) |
| apresentacao | `/login` |
| login, apresentacao, role.guard | `rotaInicial()`: `/admin` ou `/cliente` |
| auth.service | `/login` (sair) |
Perfil não tem nenhum link nem botão Voltar.
## Componentes compartilhados
- ShellComponent: admin.routes e client.routes. ConexaoWhatsapp: Perfil (compacto) e EmpresaForm (admin). BannerAvisos: Perfil (topo e lateral).
## O que a tarefa pede
- Painel novo `features/client/painel/` em `/cliente`: cartão WhatsApp (reusa ConexaoWhatsapp), cartão de previsão e botão para `/cliente/chatbot`.
- Perfil vira `/cliente/chatbot`: tirar `app-conexao-whatsapp` (perfil.html, linha 10). Ficam banner, abas Dados, Robô e Configuração (senha). Ganha "Voltar ao painel".
- `client.routes.ts`: hoje `''` leva ao perfil; passa a `''` (painel) e `chatbot` (perfil).
- Menu do cliente hoje: só "Meu perfil" para `/cliente`, só no desktop. Passa a ter Painel e Configuração do chatbot.
## Lacunas (não corrigidas)
1. Celular sem menu do cliente: a barra inferior existe só para admin (`@if (admin)`). Cliente no celular não chega ao chatbot.
2. `/cliente/chatbot` não existe e nenhum link aponta para ele.
3. ConexaoWhatsapp também está no EmpresaForm (admin): mudar o componente afeta o admin.
4. `design-v0`: rota sem link (área de teste, esperado).
5. `lembrar-confirmacao-reserva` também mexe no board ClientePerfil (design.md, riscos).
Estado: aprovado
