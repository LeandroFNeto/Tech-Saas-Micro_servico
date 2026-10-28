# Lacunas do front (aplicar novo design)

Base: relatórios de inventário, visual, navegação e auditoria da skill diasabatico (26/10/2026).
Ajuste feito nesta entrega: campos "Sessão WhatsApp" e "ID da agenda Google" do cadastro de cliente passaram de 14 px para 16 px (`text-base`), para o iPhone não dar zoom. `npm run build` em `frontend/` passou.

## (a) Pronto, por tela

| Tela | Estado |
|---|---|
| Apresentação (`/`, rota lazy) | Pronta no visual, Entrar leva a `/login`. Sem teste automático |
| Login (`/login`) | Pronta: marca, campos com rótulo, mostrar/ocultar senha, "← Conhecer o gamb" |
| Painel admin (`/admin`) | Pronto: cartões no celular, tabela a partir de 1100 px, estados de erro e vazio |
| Cadastrar/editar cliente (`/admin/empresas/nova` e `/:sessao`) | Pronta: duas colunas, fotos com miniatura, interruptores |
| Gestão de avisos (`/admin/avisos`) | Pronta: cartões de aviso e formulário |
| Perfil do cliente (`/cliente`) | Pronto: abas, robô com interruptor, dica de senha |
| Menu (shell) | Pronto: abas fixas no celular, barra lateral no desktop |
| Conexão WhatsApp | Pronta no visual (QR Code não visto no navegador) |
| Banner de avisos | Pronto: manutenção e avisos compactos |

## (b) Lacunas

| Tela | O que falta | Quem resolve |
|---|---|---|
| Criar cliente (`admin/empresas/nova`) | Não tem desenho próprio; usa o mesmo formulário da edição | Visual (a pessoa aceita ou pede desenho) |
| Login e apresentação | Contato da gamb (`[CONTATO DA GAMB]`) não está no código nem no JSON | Pessoa |
| Painel admin | Com erro de API, a faixa de erro e "Nenhum cliente cadastrado ainda." aparecem juntos (A1) | Visual (ajuste pequeno) |
| Painel admin | Rótulos de 11 px no cabeçalho dos cartões | Visual (opcional) |
| Cadastro de cliente | Interruptor `.switch` mede 48x28; altura menor que 44 px, mas a linha inteira com rótulo é maior (A3) | Visual (aceito, sem ação) |
| Abas do perfil | Navegação por setas não existe; todas as abas são focáveis por Tab | Prova (decidir se entra) |
| Telas de admin, banner e conexão | Texto ainda fixo no HTML, como o `design.md` prevê | Texto (opcional, mover para JSON depois) |
| Apresentação, login, painel | Navegação (Entrar, Sessão, Voltar) sem teste automático | Testes (ver d) |
| Cadastro de cliente | Campos não cruzados com o DTO de `especificacao/back/` | Contrato |
| Conversa do WhatsApp (`conversa-whatsapp.jpg`) | Conferir antes do deploy: a linha de mensagens temporárias está coberta só na primeira parte | Pessoa |
| Logo antiga (`docs/logo-gamb-antiga.jpg`) | Arquivo mantido só para o script do README; decidir se fica | Pessoa |

## (c) Depende de API ou da pessoa

Depende de API (contrato antes do Java):
- Lista de empresas com dados reais no painel (só vi estado de erro e vazio, porque a API não respondeu).
- Status do WhatsApp, QR Code e botão Conectar (dependem de rota de conexão).
- Salvar cliente, criar aviso, editar aviso, excluir aviso: confirmar o formato de resposta.
- Mensagem de senha incorreta (401): hoje vem do JSON, o backend precisa devolver 401.

Depende da pessoa:
- Contato da gamb para a apresentação e o login.
- Desenho da tela de criar cliente (hoje reaproveita a de editar).
- Rota `design-v0` continua aberta e sem guard (fora desta change; decidir se remove ou protege).
- Foto e conversa reais antes do deploy (ver item de conversa acima).

Não verificado no navegador (precisa de teste manual):
- Layout em 768 px.
- Navegação por teclado completa.
- Interruptores ligando e desligando com dados.
- QR Code.
- Dados reais em todas as listas.

## (d) Para a etapa de Testes

- Criar `frontend/src/app/features/apresentacao/apresentacao.component.spec.ts` (não existe). Cobrir: Entrar leva a `/login`; sessão ativa leva ao painel.
- Hoje só existe `app.component.spec.ts`; a navegação do login e do painel não tem teste.
- Rodar `npm test -- --watch=false --browsers=ChromeHeadless` depois de criar o spec.

Estado: aprovado
