# API: reserva-por-hora (aba Funcionamento ligada ao back)

## Arquivos tocados
- novo: `frontend/src/app/shared/models/funcionamento.models.ts` (tipos dos DTOs), `frontend/src/app/core/services/funcionamento.service.ts` e `.spec.ts`
- `frontend/src/app/features/client/funcionamento/funcionamento.component.ts`, `.html`, `.spec.ts`
- `frontend/src/app/features/client/perfil/perfil.component.spec.ts` (mock do serviço novo)
- `especificacao/front/cliente-perfil.content.json` (`funcionamento.erro.salvarTitulo` e `diaTitulo`)

## Como cada ação chama a API (sessão = `AuthService.sessao().sessaoWhatsapp`, token pelo interceptor)
- Abrir: `GET /empresas/{sessao}/funcionamento`. carregando, depois normal; `horarios` vazio vira vazio (padrão 08:00 às 22:00, 60 min); falha vira erro. "Tentar de novo" chama `carregar()` de novo. Sem sessão vai direto ao erro, sem chamada.
- Salvar funcionamento: `PUT` no mesmo caminho. Personalizar fechado grava o mesmo abre/fecha nos 7 dias e no feriado (duração null); aberto envia a tabela por dia (`fechado = !aberto`, "Usar a geral" = `duracaoMinutos: null`). Mostra sucesso ou erro.
- Adicionar dia especial: `POST /empresas/{sessao}/dias-especiais`. `reservasNaData` com itens mostra o aviso com a data enviada; vazio limpa o aviso.
- Remover: `DELETE /empresas/{sessao}/dias-especiais/{id}`; tira da lista ao dar 204.
- Erro 400: mostra a mensagem do corpo (texto ou `message`/`mensagem`/`erro`/`error`/`detail`); sem mensagem usa o texto padrão. Erro do carregamento aparece no bloco de erro; de salvar e de dia especial, num alerta dentro do formulário.
- Botões Salvar, Adicionar e Remover ficam desabilitados enquanto uma chamada roda. Serviço sem estado, sem cache; a leitura sempre vem da sessão do logado.
- Removidos: `HORARIOS_EXEMPLO`, `ESPECIAIS_EXEMPLO`, `AVISO_EXEMPLO` e `mudarEstado()` (não havia seletor no HTML). Validação local mantida.

## O que ficou fora
- Abrir o Personalizar sozinho quando o horário salvo não é uniforme (feito para o modo simples não sobrescrever horários por dia); não há campo geral abre/fecha no DTO, então o modo simples usa o primeiro dia aberto.
- Sem teste em navegador real contra o back; sem mudança de Java, `especificacao/back` ou `swagger.yaml`.
- Pendentes da auditoria: `aria-invalid`/`aria-describedby` nos campos citados, `step="5"` do intervalo, aba por query.

## Resultado
- `npm run build`: ok, bundle gerado sem erro.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 68 de 68 ok (eram 56).
- Código novo sem comentários.

Estado: aprovado
