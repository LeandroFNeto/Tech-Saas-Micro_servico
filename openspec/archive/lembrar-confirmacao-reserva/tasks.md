# Tarefas

## Antes da programação (a pessoa faz, fora do código)

- Gerar e guardar, com cópia segura fora do banco e do repositório, `CONTATOS_CHAVE_CRIPTO` (32 bytes em base64) e `CONTATOS_CHAVE_INDICE`, e colocá-las no `.env` do ambiente. Sem as duas, o sistema não grava contatos nem envia lembretes.
- Conferir que `corrigir-reserva-automatica` está no ar no ambiente (ela já está na `main`, AGD-02 e WPP-07): a reserva do bot chega de verdade à agenda e o cliente só recebe "Reserva confirmada" depois do registro.

## Programação

- [x] Contrato YAML: `LembreteConfirmacaoDTO`, campo em `EmpresaUpdateDTO`/`EmpresaResponseDTO`, `ReservaResumoDTO`, `VincularContatoDTO`, `ContatoResumoDTO`, `NumeroContatoDTO`, os cinco `paths` novos (200, 401, 403, 404, 409, 429 e 502 conforme a rota) e `$ref` em `especificacao/back/swagger.yaml`
- [x] Java: `CriptografiaCampo` (AES-256-GCM e HMAC), entidades `Contato`, `Reserva` e `AuditoriaAcessoContato`, enums `SituacaoReserva` e `ModoLembrete`, repositórios com `exported = false` e campos do lembrete em `Empresa` (nulo vale `DESLIGADO`)
- [x] Java: `ServicoReserva.listarEventosProximos` e `ServicoSincronizacaoAgenda` (upsert por evento, telefone no título ou na descrição, evento removido ou remarcado)
- [x] Java: `ServicoMensagem.tentarEnviarMensagemWPP` com resultado
- [x] Java: `ServicoLembreteReserva` agendado com `@EnableScheduling` (modos automático e sugerido, janela, reenvio, `SEM_RESPOSTA`, `ENCERRADA`, limites de envio) e rotina de retenção
- [x] Java: `InterpretadorRespostaLembrete`, `InterpretadorPorPalavras` e `ServicoRespostaLembrete`, chamado pelo `ControllerWhatsapp` só no estado `INICIO`; o controller também registra o contato
- [x] Java: `ObservadorGoogleAgenda` (já registra com resultado real) passa a gravar a `Reserva` do bot com `googleEventId` = `idEventoDoPedido(idPedido)` (`AGUARDANDO_LEMBRETE` no sucesso e no 409, `FALHA_AGENDA` na falha final, nada em data ocupada), sem alterar tentativas, `concluirRegistro` nem a mensagem ao cliente; tornar `idEventoDoPedido` acessível ao observador
- [x] Java: rotas do painel (reservas, enviar lembrete, ligar contato, contatos, número completo), só com o token da própria empresa e 403 para o admin, e mapeamento dos DTOs no PUT do cliente
- [x] Infra: `CONTATOS_CHAVE_CRIPTO`, `CONTATOS_CHAVE_INDICE` e `retencao.meses` em `application.properties`, `docker-compose.yml` e `.env.example`
- [x] Front, canvas: board `ClienteAutomacao` (abas Lembrete, Reservas e Contatos) e botão "Automação" no board `ClientePainel`, no artefato "Painel gamb — telas", antes do Angular
- [x] Front: `cliente-automacao.content.json`, `cliente-painel.content.json`, `shell.content.json` e `telas.design.json`
- [x] Front: `features/client/automacao/` com a aba Lembrete, a aba Reservas (tabela do dia, destaque de risco alto e ação "ligar contato") e a aba Contatos ("ver número" um por vez); rota `/cliente/automacao`; botão no painel inicial; item "Automação" no menu lateral
- [x] Prova: `ServicoSincronizacaoAgendaTest`, `ServicoLembreteReservaTest`, `InterpretadorPorPalavrasTest`, `ServicoRespostaLembreteTest`, `CriptografiaCampoTest`, persistência cifrada, `ObservadorGoogleAgendaTest` e `observer-agenda.feature` estendidos, cenários em `empresas.feature` (inclusive PUT do cliente com lembrete e `permiteReservaAutomatica` diferente respondendo 400) e testes Angular, conforme `design.md`, seção Arquivos

## Fechamento (não é da programação)

- [x] Revisão: gravar AGD-03, AGD-04, WPP-08, WPP-09, EMP-07, EMP-08 e EMP-09 nos `spec.md` dos domínios (conferir os próximos números livres antes de gravar) e as linhas em `openspec/specs/README.md`
- [x] Deploy: mover esta pasta para `openspec/archive/lembrar-confirmacao-reserva/`
