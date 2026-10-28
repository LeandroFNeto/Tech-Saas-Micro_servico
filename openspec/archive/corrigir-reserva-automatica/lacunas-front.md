# Lacunas front: corrigir-reserva-automatica

Mesmo quadro de `diasabatico/5-entrega.md`.

## 1. Pronto, por tela

- Cliente, `/cliente/chatbot` (aba Robô): a opção "Reserva Automática pelo Bot" ficou só para leitura, com aviso de que o administrador libera. O campo deixou de ser enviado no salvamento.
- Admin, cadastro e edição (`/admin/empresas/nova` e `/admin/empresas/:sessao`): interruptor novo "Reserva automática pelo bot". Sem agenda Google, a linha fica em vermelho com a mensagem do back.
- Canvas `ClienteChatbot.dc.html` e `AdminClienteForm.dc.html`: espelham as duas telas.
- Rotas e guards: sem mudança. Build passou e 48 de 48 testes passaram.

## 2. Lacunas

| Tela | O que falta | Quem resolve |
|---|---|---|
| Admin (form) | Cores vermelhas e textos fixos no HTML e no TS; sem `*.content.json` nem entrada em `telas.design.json` | Visual e texto |
| Admin (form) | Frase do erro da agenda repetida no TS; o aviso só acende se a mensagem citar "agenda" | Contrato |
| Admin e cliente | Responsivo em 375 px não medido em navegador | Visual (checagem manual) |
| Admin e cliente | Console do navegador não aberto (app não rodou) | Visual (checagem manual) |

Os specs do perfil do cliente e do formulário do admin (interruptor e `tratarErroSalvar`) existem e cobrem o campo.

## 3. Depende de API

- PUT do cliente sem `permiteReservaAutomatica`: o back mantém o valor atual (o campo ausente é ignorado; valor diferente responde 400).
- Erro da agenda: o back responde 400 com "Cadastre a agenda do Google antes de ligar a reserva automática." no POST do admin e no PUT do admin quando a opção passa de desligada para ligada; apagar a agenda de empresa já ligada é aceito. O front reconhece pelo status 400 e pela palavra "agenda". Se o texto mudar, só o aviso do topo aparece.
- Contrato: `permiteReservaAutomatica` já existe nos DTOs de criação, atualização admin, atualização do cliente e resposta. Nenhum campo novo.

## 4. Para a pessoa decidir

1. Aprovar o aviso do cliente "A reserva automática é liberada pelo administrador.". Ele já está em `cliente-perfil.content.json`, mas a regra pedia `[PLACEHOLDER]` até a pessoa aprovar o texto.
2. Admin: criar agora o `*.content.json` e a entrada em `telas.design.json`, ou seguir com texto no HTML como o resto do admin?
3. Dica do interruptor do admin: reaproveita a do cliente. Aprovar?
4. Resolvido: os testes de Prova entraram nesta change (`tasks.md`, seção Programação).

Estado: entregue e revisado; os itens 1 a 3 ainda aguardam a decisão da pessoa e não bloqueiam o deploy.
