# Entrega: corrigir-reserva-automatica

Quadro da fase 5, a partir de 1-inventario, 2-visual, 3-navegacao e 4-auditoria. Nada corrigido nesta fase.

## 1. Pronto, por tela

- Cliente, `/cliente/chatbot` (aba Robô): a opção "Reserva Automática pelo Bot" ficou só para leitura, com aviso de que o administrador libera. O campo deixou de ser enviado no salvamento.
- Admin, cadastro e edição (`/admin/empresas/nova` e `/admin/empresas/:sessao`): interruptor novo "Reserva automática pelo bot". Sem agenda Google, a linha fica em vermelho com a mensagem do back.
- Canvas `ClienteChatbot.dc.html` e `AdminClienteForm.dc.html`: espelham as duas telas.
- Rotas e guards: sem mudança. Build passou e 38 de 38 testes passaram.

## 2. Lacunas

| Tela | O que falta | Quem resolve |
|---|---|---|
| Cliente (perfil) | Nenhum spec cobre o campo fora do PUT | Prova (tarefa nova) |
| Cliente (perfil) | Variável `_somenteLeitura` declarada e não usada | Navegação (cosmético) |
| Admin (form) | Nenhum spec cobre o interruptor nem `tratarErroSalvar` | Prova |
| Admin (form) | Cores vermelhas e textos fixos no HTML e no TS; sem `*.content.json` nem entrada em `telas.design.json` | Visual e texto |
| Admin (form) | Frase do erro da agenda repetida no TS; o aviso só acende se a mensagem citar "agenda" | Contrato |
| Admin e cliente | Responsivo em 375 px não medido em navegador | Visual (checagem manual) |
| Admin e cliente | Console do navegador não aberto (app não rodou) | Visual (checagem manual) |

## 3. Depende de API

- PUT do cliente sem `permiteReservaAutomatica`: o back precisa manter o valor atual. Não confirmado; conferir antes de fechar a tarefa.
- Erro da agenda: o back responde 400 com "Cadastre a agenda do Google antes de ligar a reserva automática." no POST e no PUT do admin. O front reconhece pelo status 400 e pela palavra "agenda". Se o texto mudar, só o aviso do topo aparece.
- Contrato: `permiteReservaAutomatica` já existe nos DTOs de criação, atualização admin, atualização do cliente e resposta. Nenhum campo novo.

## 4. Para a pessoa decidir

1. Aprovar o aviso do cliente "A reserva automática é liberada pelo administrador.". Ele já está em `cliente-perfil.content.json`, mas a regra pedia `[PLACEHOLDER]` até a pessoa aprovar o texto.
2. Admin: criar agora o `*.content.json` e a entrada em `telas.design.json`, ou seguir com texto no HTML como o resto do admin?
3. Dica do interruptor do admin: reaproveita a do cliente. Aprovar?
4. Os testes de Prova entram nesta change ou em outra?

Estado: aprovado
