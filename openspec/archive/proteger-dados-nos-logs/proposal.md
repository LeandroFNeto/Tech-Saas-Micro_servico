# proteger-dados-nos-logs

## Problema

O sistema escreve dado pessoal de quem conversa com o bot no console, em texto claro, a cada mensagem:

- `ControllerWhatsapp` imprime o "raio-x" de todo evento recebido, com o número de quem escreveu (`from`), o tipo e o texto da mensagem.
- `Observadorwhatsapp` imprime o número e o texto de cada mensagem recebida e de cada resposta enviada. As respostas incluem tabela de preços, regras, nomes e, nas próximas changes, lembretes de reserva.
- `ObservadorGoogleAgenda` imprime o nome de quem reservou.
- Mensagens de erro de `ServicoWppConnect`, `ServicoReserva` e dos outros serviços usam `e.getMessage()` e `printStackTrace()`. Erros de HTTP e do Google costumam trazer no texto o corpo da resposta, a URL e o identificador da agenda, que muitas vezes é o e-mail do dono.
- `AdminInitializer` imprime e-mails de usuários.
- `spring.jpa.show-sql=true` deixa o SQL sempre ligado.

Esses logs ficam no disco do servidor e no histórico do Docker, e quem tem acesso à hospedagem os lê. Isso contradiz a promessa de que os dados do cliente da plataforma ficam só com ele, e piora com as changes de contatos e lembretes. É um problema de segurança, e não apenas de organização.

## O que muda

### Entra

- Toda saída de console do código passa a usar o logger do projeto (SLF4J), com nível. Acabam `System.out`, `System.err` e `printStackTrace`.
- Regra única: o log nunca contém número de telefone completo, texto de mensagem, nome de cliente final, e-mail completo, token, senha, nem o identificador da agenda do Google.
- Um utilitário de máscara para o que precisa aparecer para o suporte: número como `***7777`, contato `@lid` como `lid:***4321`, e-mail como `a***@dominio.com`. O suporte continua conseguindo acompanhar uma conversa pelo final do número e pelo nome da sessão.
- O "raio-x" do webhook passa a mostrar só dados técnicos: id da mensagem, tipo, se é grupo, se foi enviada pelo dono, número mascarado e tamanho do texto. Fica em nível DEBUG, desligado por padrão.
- Erros registram a classe da exceção e, quando houver, o código HTTP. Não registram a mensagem da exceção nem o corpo da resposta.
- `show-sql` desligado por padrão.
- Rotação dos logs do Docker (tamanho e quantidade de arquivos), para dado residual não ficar guardado para sempre.
- Os comentários dos arquivos alterados são removidos, conforme a regra do projeto (o comportamento está na spec).

### Altera

- `Observadorwhatsapp`: os três métodos mantêm a assinatura, mas deixam de imprimir o texto e o número completo.
- `WhatsappWebhookDTO.resumoRaioX`: devolve só o resumo técnico mascarado.

### Sai

Os prints com emoji e os `printStackTrace`.

## Fora de escopo

- Os logs do próprio WPPConnect Server (outro programa, em outro contêiner). Só registrar como risco e checar o nível dele na implantação.
- Auditoria de quem acessou dados dos clientes (já prevista na change `lembrar-confirmacao-reserva` para o número completo do contato).
- Mudar a estratégia de mensagens, rotas ou contratos HTTP. Nada muda para quem usa o sistema.
- Mascarar o que já foi escrito em logs antigos. O dono do servidor deve apagar ou rotacionar os arquivos existentes ao colocar a change no ar.

## Domínios

- spec: `openspec/specs/whatsapp/spec.md` (WPP-09 atendimento sem dado pessoal nos logs)
- spec: `openspec/specs/00-architecture.md` (regra transversal de logs, em uma linha)

Vem antes de `corrigir-reserva-automatica`, `exibir-previsao-tempo-reservas` e `lembrar-confirmacao-reserva`: as três passam a gerar mais dado pessoal e mexem nos mesmos arquivos de log.
