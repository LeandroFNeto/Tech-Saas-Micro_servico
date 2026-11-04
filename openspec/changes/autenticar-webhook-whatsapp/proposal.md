# autenticar-webhook-whatsapp

## Problema

O `POST /webhook/**` está liberado a qualquer chamador (`SecurityConfig`, `permitAll`) e não verifica quem envia. O texto, o `from` e a `session` do payload são tratados como verdadeiros. A revisão de segurança da change `bot-whatsapp-gamb-suporte-teste` mostrou o efeito:

- um POST forjado com o número de alguém autorizado executa módulos do bot de teste, que chamam a IA e geram custo;
- quem forja troca o `from` a cada requisição e zera o limite diário por número da atendente da Gamb;
- é possível percorrer o fluxo de reunião e criar eventos em série na Google Agenda da Gamb, com convite por e-mail a todos os responsáveis;
- vale para o webhook de todas as empresas, não só para a Gamb.

A gravidade sobe para alta se a porta do backend for alcançável fora da rede do Docker.

## O que muda

### Entra
- Verificação de origem em `/webhook/**`: o WPPConnect envia um segredo compartilhado em cabeçalho e o backend recusa requisição sem ele ou com valor errado.
- Variável de ambiente para o segredo, sem valor padrão que libere o webhook.
- Registro de log da recusa, sem o conteúdo da mensagem e com o número mascarado.

### Altera
- `SecurityConfig` e `ControllerWhatsapp`: a recusa vem antes de qualquer strategy.
- Configuração do WPPConnect: passa a enviar o cabeçalho em cada chamada ao webhook.
- `especificacao/back/paths/webhook-whatsapp.yaml`: documenta o cabeçalho e as respostas de recusa.

### Sai
- Nada do comportamento das strategies muda.

## Fora de escopo

- Teto de reuniões por número da atendente da Gamb.
- Limpeza do mapa do `LimiteDiarioMensagens`.
- Tratar contato `@lid` na lista de autorizados do bot de teste.
- Rotacionar o segredo de forma automática.

## Domínios
- spec: `openspec/specs/whatsapp/spec.md`
- spec: `openspec/specs/auth/spec.md`
