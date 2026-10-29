# Inventário: reserva-por-hora (/cliente/chatbot)

## 1. Rotas
| Rota | Componente | Guard |
|---|---|---|
| `''`, `login`, `design-v0`, `**` | Apresentacao, Login, V0Dashboard, redirect | nenhum |
| `admin/*` | Dashboard, EmpresaForm, GestaoAvisos | authGuard + roleGuard('ADMIN') |
| `cliente` | Painel | authGuard + roleGuard('CLIENTE') |
| `cliente/chatbot` | PerfilComponent | authGuard + roleGuard('CLIENTE') |

## 2. Links
- shell.component.html: `/admin`, `/admin/empresas/nova`, `/admin/avisos`, `/cliente`, `/cliente/chatbot`
- dashboard.component.html: `/admin/empresas/nova`, `/admin/empresas/:sessao`
- painel.component.html: `/cliente/chatbot`; perfil.component.html: `/cliente`
- empresa-form: `/admin`; login, apresentacao, auth.service: `/login`, `/`, `rotaInicial()`

## 3. Componentes
| Componente | Arquivos | Usado em |
|---|---|---|
| PerfilComponent (tela /cliente/chatbot) | perfil/ .ts, .html, .spec.ts | rota `cliente/chatbot` |
| PainelComponent | painel/ com spec | rota `cliente` |
| BannerAvisos | shared/components/banner-avisos/ (sem spec) | perfil, 2 usos |
| ConexaoWhatsapp | shared/components/conexao-whatsapp/ (sem spec) | painel, empresa-form |
| ShellComponent | shared/components/shell/ (sem spec) | rotas admin e cliente |

Tela /cliente/chatbot: spec `perfil.component.spec.ts`; conteudo `especificacao/front/cliente-perfil.content.json`; design `especificacao/front/telas.design.json` (so `botaoChatbot`, sem entrada do perfil); contratos `empresas-funcionamento.yaml`, `empresas-dias-especiais.yaml`, `empresas-dias-especiais-id.yaml`.

## 4. O que a tarefa pede
- Alterada: PerfilComponent ganha o bloco "Funcionamento" com "Personalizar". Sem rota nova.
- Antes do Angular: canvas de design (design.md item 16).
- Ajuste: `cliente-perfil.content.json` e entrada em `telas.design.json`.

## Lacunas
- Board "ClienteChatbot" nao existe no repo; a tela real e `cliente-perfil`.
- `telas.design.json` sem entrada do perfil.
- Rota `design-v0` sem link.
- Sem spec: BannerAvisos, ConexaoWhatsapp, Shell.
- Aba do bloco (dados, robo ou configuracao) nao definida.

Estado: aprovado
