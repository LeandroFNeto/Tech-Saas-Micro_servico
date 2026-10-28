# Tarefas

## Antes da programação (a pessoa faz, fora do código)

- Gerar e guardar, com cópia segura fora do banco e do repositório, `CONTATOS_CHAVE_CRIPTO` (32 bytes em base64) e `CONTATOS_CHAVE_INDICE`, e colocá-las no `.env` do ambiente. Sem as duas, o sistema não grava contatos nem envia lembretes.
- Ter aplicado `corrigir-reserva-automatica`, para a reserva do bot chegar de verdade à agenda.

## Programação

- [ ] Contrato YAML: `LembreteConfirmacaoDTO`, campo em `EmpresaUpdateDTO`/`EmpresaResponseDTO`, `ReservaResumoDTO`, `VincularContatoDTO`, `ContatoResumoDTO`, `NumeroContatoDTO`, os cinco `paths` novos (200, 401, 403, 404, 409, 429 e 502 conforme a rota) e `$ref` em `especificacao/back/swagger.yaml`
- [ ] Java: `CriptografiaCampo` (AES-256-GCM e HMAC), entidades `Contato`, `Reserva` e `AuditoriaAcessoContato`, enums `SituacaoReserva` e `ModoLembrete`, repositórios com `exported = false` e campos do lembrete em `Empresa` (nulo vale `DESLIGADO`)
- [ ] Java: `ServicoReserva.listarEventosProximos` e `ServicoSincronizacaoAgenda` (upsert por evento, telefone no título ou na descrição, evento removido ou remarcado)
- [ ] Java: `ServicoMensagem.tentarEnviarMensagemWPP` com resultado
- [ ] Java: `ServicoLembreteReserva` agendado com `@EnableScheduling` (modos automático e sugerido, janela, reenvio, `SEM_RESPOSTA`, `ENCERRADA`, limites de envio) e rotina de retenção
- [ ] Java: `InterpretadorRespostaLembrete`, `InterpretadorPorPalavras` e `ServicoRespostaLembrete`, chamado pelo `ControllerWhatsapp` só no estado `INICIO`; o controller também registra o contato
- [ ] Java: `ObservadorGoogleAgenda` grava a `Reserva` do bot (`AGUARDANDO_LEMBRETE` ou `FALHA_AGENDA`)
- [ ] Java: rotas do painel (reservas, enviar lembrete, ligar contato, contatos, número completo), só com o token da própria empresa e 403 para o admin, e mapeamento dos DTOs no PUT do cliente
- [ ] Infra: `CONTATOS_CHAVE_CRIPTO`, `CONTATOS_CHAVE_INDICE` e `retencao.meses` em `application.properties`, `docker-compose.yml` e `.env.example`
- [ ] Front: canvas de design (`ClientePerfil`, `ClienteReservas` e `ClienteContatos`) antes do Angular
- [ ] Front: `cliente-perfil.content.json`, `cliente-reservas.content.json`, `cliente-contatos.content.json` e `telas.design.json`
- [ ] Front: bloco do lembrete no perfil, tabela do dia com destaque de risco alto e ação "ligar contato", e lista de contatos com "ver número" um por vez
- [ ] Prova: `ServicoSincronizacaoAgendaTest`, `ServicoLembreteReservaTest`, `InterpretadorPorPalavrasTest`, `ServicoRespostaLembreteTest`, `CriptografiaCampoTest`, persistência cifrada, cenários em `empresas.feature` e testes Angular, conforme `design.md`, seção Arquivos

## Fechamento (não é da programação)

- [ ] Revisão: gravar AGD-03, AGD-04, WPP-07, WPP-08, EMP-07, EMP-08 e EMP-09 nos `spec.md` dos domínios e as linhas em `openspec/specs/README.md`
- [ ] Deploy: mover esta pasta para `openspec/archive/lembrar-confirmacao-reserva/`
