# expurgar-contato-no-opt-out

## Problema

O requisito "Direito ao esquecimento e opt-out" de `openspec/specs/contatos/spec.md` diz que o opt-out por WhatsApp SHALL expurgar os dados pessoais. A regra 5.3 de segurança diz o mesmo. O código não faz isso:

- `ServicoContato.processarOptOutPorMensagem` só grava `nao_contatar = true`. Nome e WhatsApp cifrados ficam na base.
- `ServicoRespostaLembrete`, na intenção `PARAR`, faz o mesmo.
- A próxima mensagem do cliente passa por `registrarOuAtualizarContato`, que regrava o nome.
- `contatos.whatsapp_id` é `not null` no banco e na entidade. Expurgar o WhatsApp exige mudar a coluna, por isso a correção não cabe num ajuste de bug e precisa desta change.

## O que muda

### Entra
- Migração Flyway `V2` que torna `contatos.whatsapp_id` anulável.
- Método único de expurgo por opt-out em `ServicoContato`, usado pelo comando por mensagem e pela intenção `PARAR` do lembrete.
- Resposta 422 documentada em `especificacao/back/paths/empresas-contatos-numero.yaml` para contato sem número (o código já devolve 422 para `@lid`).
- Tratamento de contato expurgado na rotação de chaves de `endurecer-login-e-gateway`.

### Altera
- Opt-out apaga `whatsapp_id`, `nome` e `final_telefone` e mantém a linha com `numero_indice` e `nao_contatar = true`.
- Mensagem nova de contato expurgado só atualiza `ultima_conversa_em`; não regrava nome, WhatsApp nem final do telefone.
- Revelar o número de contato expurgado responde 422 e não grava auditoria, nos dois caminhos (`ControllerClienteAutomacao` e `ServicoContato.obterNumeroCompleto`).

### Sai
- Nada.

## Fora de escopo

- Voltar a aceitar contato depois do opt-out (opt-in). Fica para quando houver pedido.
- `reservas.titulo` e demais dados da reserva. A reserva é registro da agenda da empresa e continua ligada à linha expurgada, que bloqueia o lembrete.
- Retenção periódica. Continua apagando contatos inativos, inclusive os expurgados.
- Exclusão pelo painel. Continua apagando a linha inteira.

## Domínios
- spec: `openspec/specs/contatos/spec.md`
