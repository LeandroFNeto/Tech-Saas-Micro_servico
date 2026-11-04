## 1. Conteúdo e desenho

- [x] 1.1 Ler a prancha da apresentação no canvas de design do front e desenhar as seções Quem somos, Hoje e o que vem e Contato, com o botão no cabeçalho
- [x] 1.2 Fechar com o dono o texto de cada seção, usando o vocabulário do chat (startup, assinatura, hoje, em desenvolvimento) e a frase de princípio das câmeras

## 2. Conteúdo e estilo

- [x] 2.1 Acrescentar a `apresentacao.content.json` os blocos `quemSomos`, `hojeEEmDesenvolvimento` e `contato` (inclui `whatsapp` e `whatsappTexto`), e o botão do cabeçalho em `topo`
- [x] 2.2 Acrescentar a `telas.design.json` as classes das seções novas, do selo "Em desenvolvimento", dos cartões e dos botões de WhatsApp, com alvo de toque de 44px

## 3. Tela

- [x] 3.1 Montar as seções em `apresentacao.component.html` na ordem definida, com `aria-labelledby`, links `wa.me` em nova aba com `rel="noopener"` e o número no rodapé
- [ ] 3.2 Conferir a página a 390px, 768px e 1280px, sem rolagem horizontal, com a ordem de empilhamento correta

## 4. Testes e verificação

- [x] 4.1 Atualizar `apresentacao.component.spec.ts`: seção Quem somos, hoje e em desenvolvimento, câmeras como apoio à decisão, ausência de "R$" e de nome de cliente, links `wa.me` com o número, nova aba, número escrito, nenhuma requisição à API
- [x] 4.2 Conferir que o número da página é o mesmo da propriedade `gamb.contato.whatsapp` do backend
- [x] 4.3 Rodar `ng test`, `ng build` e `graphify update .`
- [x] 4.4 Atualizar a linha de Login e senha em `openspec/specs/README.md` se a prova mudar

## 5. Publicação

- [ ] 5.1 Levar o front para a branch `producao` (Vercel) e conferir a página publicada, os links do WhatsApp e o chat
