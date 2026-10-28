# Qualidade: exibir-previsao-tempo-reservas (volta 3)

Conferido: proposal, design, tasks, lacunas-front, seguranca, spec `empresa`, YAML, Java, Angular e provas.

## Bate com spec e contrato
- PUT `/empresas/{sessao}/cliente`: 403 (outra empresa, token legado, sessão inexistente para não-admin), 404 só admin, 400, 429, 503. `empresas-cliente.yaml` bate com `ControllerEmpresa.atualizarPeloCliente`; a checagem de dono vem antes de geocodificar e gravar.
- GET `/empresas/search/findBySessaoWhatsapp`: admin lê qualquer empresa, cliente só a própria; `empresas-search.yaml` traz 401, 403 e 404 só admin.
- GET `/empresas/{sessao}/previsao`: `empresas-previsao.yaml` com 200, 401, 403, 404 só admin, igual ao controller.
- `googleCalendarId`: `paraPainel(empresa, admin)` devolve nulo ao cliente; `entidades.yaml` marca `nullable` com descrição; `EmpresaResponseDTO` e `EmpresaUpdateDTO` YAML batem com os records.
- Contador de trocas: `consumirTrocaDeCidade` antes de geocodificar; `estornarTrocaDeCidade` só no 503; 400 conta (documentado no teste).
- Front: o cliente não usa `googleCalendarId` (só o admin: `empresa-form`, `empresa.service` mapeia opcional); painel e perfil não quebram com o campo nulo.

## Prova
- Testes informados como passados; não rodei de novo.
- `ControllerEmpresaTest` cobre 403 legado, outra empresa, sessão inexistente igual a outra empresa, estorno no 503, `googleCalendarId` oculto ao cliente e visível ao admin. `empresas.feature` repete os cenários. `ServicoPrevisaoTempoTest`, `CalculadoraRiscoDesmarcacaoTest`, `painel.component.spec.ts`, `previsao.service.spec.ts` continuam.

## Regra "sem comentários no código"
- Varredura própria no diff e nos arquivos novos da tarefa (`servico/previsao`, `dto/previsao`, controller, DTOs, model, testes, `features/client/painel`, `previsao.*`): nenhum comentário. Os de `shell`, `banner-avisos`, `conexao-whatsapp`, `gestao-avisos` e `styles.css` são de outra change e foram ignorados.

## Para a Revisão atualizar na spec (não reprova)
- O design.md (item 9) diz "sessão inexistente: 404". O código devolve 403 a quem não é admin e 404 ao admin, de acordo com `seguranca.md`; a proposta não contradiz.
- A regra de acesso (cliente só da própria empresa) em GET previsão, PUT cliente e busca por sessão entra em EMP-05/EMP-06; EMP-05 e EMP-06 ainda não estão em `openspec/specs/empresa/spec.md`.

## Divergência
Nenhuma entre spec, contrato, código e prova. Pendências: revisão (EMP-05, EMP-06 e texto do design) e deploy.

Estado: aprovado
