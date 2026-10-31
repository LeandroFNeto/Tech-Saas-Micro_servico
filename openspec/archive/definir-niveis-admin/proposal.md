# definir-niveis-admin

## Problema

Atualmente, o sistema possui apenas dois papéis (`Role`): `ADMIN` e `CLIENTE`.
Qualquer usuário administrador possui privilégios totais e indistintos no SaaS:
- Pode criar, editar, resetar senhas e desativar/excluir (`DELETE /empresas/{sessao}`) qualquer empresa;
- Pode publicar, alterar e remover comunicados globais do painel (`/admin/avisos`);
- Pode alterar configurações críticas de integrações (Google Calendar, WPPConnect, IA, previsão do tempo);
- Pode ler e manipular todas as informações operacionais de qualquer cliente/empresa.

Isso impede que equipes deleguem acessos administrativos parciais com segurança, gerando riscos de operação:
1. Uma equipe de suporte/atendimento precisa apenas consultar empresas e ajudar na configuração de horários e WhatsApp, sem permissão para apagar empresas ou emitir comunicados institucionais globais;
2. Usuários de auditoria ou gerência precisam apenas visualizar dados e métricas sem permissão de alteração (somente leitura).

## O que muda

### Entra
- Hierarquia e especialização do papel de Administrador em 3 níveis:
  1. `SUPER_ADMIN`: Administrador mestre com controle irrestrito do SaaS (criação e exclusão lógica de empresas, comunicados globais, reset de senhas administrativas e gestão de outros administradores). O seed master padrão assume este nível.
  2. `ADMIN_OPERACIONAL`: Administrador de suporte e operação. Pode cadastrar e atualizar dados operacionais de empresas, funcionamento e integrações, mas **não pode** excluir empresas nem publicar avisos administrativos globais.
  3. `ADMIN_LEITURA`: Administrador auditor/somente leitura. Possui acesso exclusivo a consultas (`GET`) de empresas, status de WhatsApp, agendamentos e avisos, sendo impedido de executar qualquer mutação (`POST`, `PUT`, `DELETE`).
- Compatibilidade retroativa: Usuários legados ou chamadas com o token estático `x-admin-token` são mapeados automaticamente com equivalência a `SUPER_ADMIN`.
- Validação granular de permissões nos endpoints administrativos via Spring Security e `ServicoAcessoEmpresa`.
- Front-end administrativo: ocultação e desativação condicional de botões de exclusão e mutação no painel administrativo com base no nível do admin autenticado.

### Altera
- `openspec/specs/auth/spec.md`: Atualizar requisitos de papéis (AUTH-01 e AUTH-04) e adicionar requisito de níveis de administração (`AUTH-05`).
- `openspec/specs/empresa/spec.md`: Especificar que endpoints destrutivos (como `DELETE /empresas/{sessao}`) exigem nível `SUPER_ADMIN`.
- `openspec/specs/avisos/spec.md`: Especificar que gestão de avisos globais requer `SUPER_ADMIN`.
- Contrato OpenAPI: enum de `Role` atualizado em `LoginResponseDTO.yaml` e restrições descritas nos endpoints correspondentes.

### Sai
- A concessão irrestrita de todas as operações administrativas para qualquer usuário com chave/perfil genérico sem checagem de nível.

## Fora de escopo
- Matriz dinâmica de permissões configurável em tempo de execução via banco de dados (RBAC granular por tela/ação individual). Os níveis são papéis fixos pré-definidos no enum.
- Cadastro self-service de novos administradores por tela pública.
- Autenticação de dois fatores (MFA/2FA) para níveis elevados de admin (pertence a change própria de segurança).

## Domínios
- spec: `openspec/specs/auth/spec.md`
- spec: `openspec/specs/empresa/spec.md`
- spec: `openspec/specs/avisos/spec.md`
