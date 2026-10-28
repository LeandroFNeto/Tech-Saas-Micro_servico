# aplicar-novo-design-front

## Problema

O painel foi pensado para o celular, mas o front atual não se comporta assim. O login é um cartão escuro com efeito de vidro. O menu lateral de 260 px vira um bloco alto acima do conteúdo no celular. As listas são tabelas largas que pedem rolagem para o lado. Os interruptores são checkboxes comuns, vários botões e links têm menos de 44 px e os campos têm fonte pequena (o iPhone dá zoom ao digitar). No perfil do cliente os avisos aparecem duas vezes. A logo é um JPG com cantos e anel. Quem abre o endereço cai direto no login, sem ninguém explicar o que é o gamb.

O design novo já foi desenhado e aceito no canvas. Falta aplicá-lo de forma oficial no Angular.

## O que muda
### Entra
- Página pública de apresentação em `/`: o conceito do gamb, três passos, benefícios e botão **Entrar** no canto, que leva a `/login`.
- Novo login: faixa da marca e cartão do formulário, com título fixo nos campos, botão Mostrar/Ocultar senha e mensagem de erro sem dizer qual dos dois dados falhou.
- Menu novo nos painéis: barra superior com Sair e abas na parte de baixo no celular (admin); barra lateral no desktop.
- Telas Empresas, Editar cliente, Avisos e Meu perfil refeitas no padrão do canvas: cartões no celular, linhas de tabela no desktop, interruptores de verdade, chips de cor por tipo de aviso, abas do perfil como seletor, aviso de manutenção em destaque e avisos sem repetição.
- Logo nova em PNG transparente e escrito "gamb" com o degradê cobre da logo.

### Altera
- A rota `/` deixa de redirecionar para `/login` e mostra a apresentação. Quem já tem sessão válida vai direto ao painel, como já acontece no login. Rota desconhecida passa a cair em `/`.
- `logo-gamb.jpg` sai e `logo-gamb.png` entra, inclusive no ícone da aba.
- Campos de formulário com 48 px de altura e fonte de 16 px em todo o front.
- Textos: erro de login, rótulos de envio de foto e do botão de salvar do cliente, dica de senha mínima (lista no `design.md`).

### Sai
- Nada de rota de API, DTO, Java ou YAML. O comportamento de login, cadastro e avisos não muda.

## Fora de escopo

- Busca e filtro na lista de empresas.
- Tela de criar cliente (ramo, preço base, senha inicial): o canvas só tem o modo de edição. O modo criar segue o mesmo padrão de campos e botões, sem desenho próprio.
- Estado do QR Code do WhatsApp: continua como está, só com o visual de cartão novo.
- Recuperação automática de senha: segue sendo o admin quem redefine.
- Qualquer mudança em `src/main/java` ou em `especificacao/back/`.

## Domínios
- spec: `openspec/specs/auth/spec.md` (novo requisito AUTH-03, a página pública)
