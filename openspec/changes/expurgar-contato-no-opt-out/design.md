# Design

## Decisão

### 1. O que fica na linha depois do opt-out
Ficam `id`, `empresa_id`, `numero_indice`, `nao_contatar = true`, `origem`, `primeira_conversa_em` e `ultima_conversa_em`. Saem `whatsapp_id`, `nome` e `final_telefone`.

- `numero_indice` (HMAC) é o que acha o contato quando o mesmo número escreve de novo. Sem ele o bloqueio some.
- `final_telefone` não ajuda a bloquear. Com a chave de índice exposta, os 4 dígitos reduzem em 10 mil vezes a busca por força bruta do número. Sai.
- O painel já trata `final_telefone` nulo: mostra `Contato (**) *****-****` com a marca "não contatar".

| Alternativa | Motivo da recusa |
|---|---|
| Apagar a linha | O próximo "oi" do cliente cria contato novo com `nao_contatar = false`. Reservas e auditorias apontam para a linha |
| Manter `final_telefone` | Só serve para exibição e facilita reidentificar o número |
| Sentinela em `whatsapp_id` (texto fixo cifrado) | A rotação de chaves decifraria o sentinela e calcularia o mesmo índice para todas as linhas expurgadas da empresa, quebrando `uk_contato_empresa_numero_indice` |

### 2. Coluna `whatsapp_id`
Migração `V2__whatsapp_id_anulavel_no_opt_out.sql` com `alter table contatos alter column whatsapp_id drop not null`. Na entidade, `nullable = true`. `ddl-auto=validate` confere a entidade contra o banco no boot. `CriptografiaCampo` não cifra nulo.

### 3. Um só caminho de expurgo
`ServicoContato.expurgarPorOptOut(Contato)` grava `nao_contatar = true` e anula os três campos. `processarOptOutPorMensagem` e a intenção `PARAR` de `ServicoRespostaLembrete` chamam esse método. `ServicoRespostaLembrete` envia a mensagem de confirmação pelo `rawFrom` da conversa, que não depende do contato.

### 4. Mensagem depois do opt-out
O webhook chama `registrarOuAtualizarContato` antes do opt-out. Para contato com `nao_contatar = true`, o método só atualiza `ultima_conversa_em`. O bot continua respondendo, porque responder a quem escreveu não é envio proativo. Reserva nova pelo bot liga à mesma linha e o lembrete continua bloqueado.

### 5. Revelação do número
Contato com `whatsapp_id` nulo responde 422, sem auditoria e sem gastar a cota, igual ao `@lid`. `ServicoContato.obterNumeroCompleto` hoje devolveria `numero: null` com 200; passa a 422 também.

### 6. Rotação de chaves (`endurecer-login-e-gateway`, decisão 10)
A rotação recalcula `numero_indice` a partir do `whatsapp_id` decifrado. Linha expurgada não tem de onde recalcular.

- Manter o índice antigo não serve: ele foi gerado com a chave que vazou, e com ela se descobre o número por força bruta (o espaço de celulares do Brasil é pequeno).
- Decisão: a rotação troca o `numero_indice` dessas linhas por um valor aleatório no mesmo formato, mantém `nao_contatar = true`, não toca `nome` nem `whatsapp_id` (já nulos) e conta essas linhas no log, só com a quantidade.
- Efeito aceito: depois da rotação, o mesmo número escrevendo de novo vira contato novo. As reservas antigas continuam ligadas à linha expurgada e bloqueadas. O contato novo só recebe lembrete se fizer reserva nova, por iniciativa própria.
- Sem este tratamento, `RotacaoChavesContatos` tenta decifrar nulo e para o boot.

Ordem: `endurecer-login-e-gateway` é a próxima a ser implementada. Quem entrar depois ajusta o outro lado:
- se a rotação já existir quando esta change for aplicada, a tarefa 5 altera `RotacaoChavesContatos` e `RotacaoChavesContatosTest`;
- se esta change entrar antes, `endurecer-login-e-gateway` precisa incluir o cenário "Contato expurgado na rotação" antes de codar a rotação.

## Arquivos
- Contrato: `especificacao/back/paths/empresas-contatos-numero.yaml` ganha a resposta 422.
- Banco: `src/main/resources/db/migration/V2__whatsapp_id_anulavel_no_opt_out.sql`.
- Java: `model/Contato.java`, `servico/ServicoContato.java`, `servico/ServicoRespostaLembrete.java`, `controller/ControllerClienteAutomacao.java`; `servico/RotacaoChavesContatos.java` se já existir.
- Front: nada. `ContatoResumoDTO` já trata nome e final nulos.
- Prova: `ServicoContatoTest`, `ServicoRespostaLembreteTest`, `ControllerClienteAutomacaoTest`; `RotacaoChavesContatosTest` se a rotação já existir.

## Alinhamento
- Stack de `00-architecture.md`.
- DTO nomeado; sem entidade JPA e sem `Map` no payload. Nenhum DTO muda.
- YAML em `especificacao/back/` antes do Java. Nenhum `$ref` novo.

## Riscos
- [Linhas que já fizeram opt-out em produção] → continuam com nome e WhatsApp. Por isso a tarefa 2 inclui um `update` na mesma migração que anula `whatsapp_id`, `nome` e `final_telefone` onde `nao_contatar = true`.
- [Opt-out pela intenção `PARAR` com texto ambíguo] → hoje já marca `nao_contatar`; agora também apaga. O cliente que se arrepender precisa de opt-in, que está fora do escopo.
- [Retenção apaga o marcador] → após a janela sem interação a linha some e o bloqueio também. Não há dado guardado; um contato novo só nasce se o cliente escrever.
- [Backups] → dumps anteriores ao deploy ainda têm os dados. A regra 5.3 cobre backup criado pela change; aqui nenhum é criado. Os antigos saem pela retenção de backups.
