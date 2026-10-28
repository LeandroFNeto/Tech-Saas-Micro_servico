# Navegação e estado local: corrigir-reserva-automatica

## O que foi feito

- `perfil.component.ts`: `salvar()` separa `permiteReservaAutomatica` do valor do form e não o envia no PUT. O controle continua no form e em `preencher()` só para mostrar o valor.
- `empresa-form.component.ts`: controle `permiteReservaAutomatica: [false]`, preenchido em `preencher()` e enviado no POST e no PUT.
- `erroReservaAutomatica = signal('')`: limpo junto com `erro()` no início de `salvar()`. No erro do POST/PUT (novo método privado `tratarErroSalvar`), `erro()` recebe a mensagem do back como antes; se o status for 400 e a mensagem citar "agenda", `erroReservaAutomatica()` recebe "Cadastre a agenda do Google antes de ligar a reserva automática."
- `shared/models/empresa.models.ts`: `EmpresaUpdateAdminDTO` ganhou `permiteReservaAutomatica?: boolean` (já existia em `EmpresaCreateDTO` e na resposta); nenhum serviço mudou.

## Rotas e guards

Nada mudou: nenhuma rota nova, mesmos guards. Cadastro e edição usam o mesmo componente e as mesmas rotas.

## Verificação

- `npm run build` em `frontend/`: passou (o erro NG9 de `erroReservaAutomatica` da fase 2 sumiu).
- Tarefa "Front: opção só para leitura..." marcada em `tasks.md`.

## Lacunas

- O 400 da agenda é reconhecido por status 400 + a palavra "agenda" na mensagem do back; se o back mudar o texto sem essa palavra, só o aviso geral do topo aparece. Convém conferir com o texto real do back.
- Specs unitárias (`*.spec.ts`) não foram alteradas nem rodadas.

Estado: aprovado
