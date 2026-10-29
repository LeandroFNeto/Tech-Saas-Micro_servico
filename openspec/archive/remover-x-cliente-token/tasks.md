# Tarefas

## Antes da programação (a pessoa faz, fora do código)

- Confirmar que nenhum script ou integração fora do front envia `x-cliente-token`.

## Programação

- [x] Contrato YAML: remover `x-cliente-token` dos `paths` e documentar 403 nas rotas por sessão
- [x] Java: `ServicoAcessoEmpresa` (dono da sessão) e remoção do cabeçalho em `JwtAuthenticationFilter`, `ControllerEmpresa`, `ControllerWhatsapp` e `CorsConfig`
- [x] Prova: `ControllerEmpresaTest`, `ControllerWhatsappTest`, `JwtAuthenticationFilterTest` e cenários em `empresas.feature` e `whatsapp.feature`, conforme `design.md`, seção Arquivos

## Fechamento (não é da programação)

- [x] Revisão: gravar AUTH-04 em `openspec/specs/auth/spec.md` e a linha em `openspec/specs/README.md`
- [x] Deploy: mover esta pasta para `openspec/archive/remover-x-cliente-token/`
