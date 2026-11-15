# Tarefas

- [x] 1 Contrato: resposta 422 em `especificacao/back/paths/empresas-contatos-numero.yaml` para contato sem número (`@lid` ou expurgado)
- [x] 2 Banco: `V2__whatsapp_id_anulavel_no_opt_out.sql` com `drop not null` em `contatos.whatsapp_id` e `update` que anula `whatsapp_id`, `nome` e `final_telefone` onde `nao_contatar = true`; `Contato.whatsappId` com `nullable = true`
- [x] 3 `ServicoContato.expurgarPorOptOut`; `processarOptOutPorMensagem` e a intenção `PARAR` de `ServicoRespostaLembrete` passam a usá-lo
- [x] 4 `registrarOuAtualizarContato` com `nao_contatar = true` só atualiza `ultima_conversa_em`; revelação de contato expurgado responde 422 sem auditoria em `ControllerClienteAutomacao` e `ServicoContato.obterNumeroCompleto`; `ObservadorGoogleAgenda.gravarReservaBot` também não grava nome em contato com `nao_contatar = true`
- [x] 5 Se `RotacaoChavesContatos` já existir: linha com `whatsapp_id` nulo recebe `numero_indice` aleatório, mantém `nao_contatar` e entra no log só como quantidade. Se não existir: registrar o cenário em `openspec/changes/endurecer-login-e-gateway/`
- [x] 6 Prova:
  - `ServicoContatoTest`: opt-out por "SAIR" anula `whatsapp_id`, `nome` e `final_telefone` e mantém `numero_indice` e `nao_contatar`; mensagem seguinte do mesmo número não regrava nome nem WhatsApp; revelação de expurgado responde 422 sem auditoria
  - `ServicoRespostaLembreteTest`: intenção `PARAR` expurga
  - `ControllerClienteAutomacaoTest`: `GET .../contatos/{id}/numero` de expurgado responde 422 sem auditoria; listagem mostra o contato com máscara vazia e `naoContatar = true`
  - `RotacaoChavesContatosTest`, se a rotação existir: linha expurgada não para o boot e sai sem índice da chave antiga
- [ ] 7 Atualizar o requisito "Direito ao esquecimento e opt-out" em `openspec/specs/contatos/spec.md` com a linha `Prova:` e os cenários de `specs/contatos/spec.md` desta change
- [ ] 8 Mover esta pasta para `openspec/archive/expurgar-contato-no-opt-out/`
