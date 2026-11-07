## Context

A apresentação tem hero, "Como funciona" (3 passos), "Por que o gamb" (4 benefícios), uma chamada "Já é cliente? Entre no painel" e o rodapé. Todo o texto vem de `apresentacao.content.json` e as classes de `telas.design.json`. O chat flutuante já usa o contexto de `base-comercial.md`. O número da Gamb é `55 43 99816-5047` e hoje só existe no backend.

## Goals / Non-Goals

**Goals:**
- A página conta o mesmo posicionamento do chat: startup, grupo pequeno, assinatura, acompanhamento, crescimento controlado.
- Separar com clareza o que existe hoje do que está em desenvolvimento.
- Contato por WhatsApp visível no topo, em seção própria e com o número escrito.

**Non-Goals:**
- Preço, planos, prazo, vagas ou nome de cliente na página.
- Formulário, captura de lead ou qualquer chamada de API.
- Mudar o chat, o login ou o painel.
- Afirmar conformidade legal ou apresentar ferramenta em desenvolvimento como pronta.

## Decisions

1. **Duas seções novas entre as existentes.** Ordem final: hero, Como funciona, Quem somos, Por que o gamb, Hoje e o que vem, Contato, chamada "Já é cliente?", rodapé. Quem somos vem logo depois de entender o produto; Hoje e o que vem vem depois dos benefícios, que já falam do que existe.
2. **Hoje e o que vem em duas colunas de cartões.** "Hoje" lista o que a Gamb entrega, com ícone de marcado. "Em desenvolvimento" usa selo discreto "Em desenvolvimento" e traz a frase de que cada caso é avaliado em conversa. Em tela estreita as colunas empilham, com "Hoje" primeiro.
3. **Câmeras com a frase de princípio.** O cartão de câmeras diz que é ferramenta de apoio à decisão (tempo de espera, quem já foi atendido, tempo médio, meta de espera), que analisa sem guardar informação de ninguém e que serve à eficiência do estabelecimento e à satisfação do cliente. Fica no grupo "Em desenvolvimento". Sem afirmação legal.
4. **Contato com a mesma fonte para o link e o texto.** O número vai em `apresentacao.content.json` como `contato.whatsapp` (`5543998165047`) e `contato.whatsappTexto` (`(43) 99816-5047`); o link é montado como `https://wa.me/` mais o número, com `target="_blank"` e `rel="noopener"`. O backend mantém a própria propriedade para o chat; os dois valores devem coincidir e a tarefa de verificação confere isso.
5. **Três pontos de contato.** Botão secundário no cabeçalho ao lado de Entrar, seção "Contato" com botão principal e número escrito, e o número no rodapé. Em celular, o botão do cabeçalho fica compacto e a seção de contato é o ponto principal.
6. **Texto do contato sem preço.** A seção diz que o plano de assinatura é combinado conversando e que a Gamb atende poucos clientes por vez, sem prometer vaga.
7. **Fluxo visual primeiro.** O desenho das seções passa pelo canvas de design do front e pela fase visual do `diasabatico` antes de portar para o Angular, como nas outras telas. Nenhuma seção nova chama API.
8. **Acessibilidade.** Cada seção com `aria-labelledby`, link do WhatsApp com texto que descreve a ação e alvo de toque de pelo menos 44px, foco visível igual ao dos outros botões.

## Risks / Trade-offs

- [Página prometer mais do que a Gamb entrega] -> tudo que não existe fica sob "Em desenvolvimento", com a frase de avaliação caso a caso; os testes procuram "R$", "prazo" e a palavra "garantia" fora de contexto de negação.
- [Número divergir entre front e backend] -> tarefa de verificação confere os dois e o teste do front trava o número esperado.
- [Página longa demais no celular] -> as seções novas são curtas, em cartões, e a de contato fica perto do fim, onde o visitante decide.
- [Publicação em branch diferente] -> o front sai pela `producao` (Vercel) e o backend pela `main` (Hostinger); a entrega inclui levar o front para a `producao`.

## Open Questions

- Texto final das chamadas e dos cartões, a revisar na fase visual com o dono.
- Se o botão do cabeçalho deve trocar o Entrar como destaque ou ficar ao lado como secundário (a proposta é secundário).
