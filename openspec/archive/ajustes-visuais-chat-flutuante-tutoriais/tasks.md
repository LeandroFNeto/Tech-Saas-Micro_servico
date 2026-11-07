# Tasks

## 1. Contrato

- [x] 1.1 Criar `especificacao/back/paths/publico-assistente-suporte.yaml` (rota pública, 200, 400, 429, sem dado de empresa) e registrar no índice dos paths; conferir que o YAML valida.
- [x] 1.2 Ajustar `cliente-assistente-chat.yaml` com o escopo do Suporte (pode, não pode, recusa) e a regra de não levar dado de empresa; conferir que o texto bate com o spec.

## 2. Backend do Suporte

- [x] 2.1 Fazer `construirPromptSuporteCliente` montar o prompt só com escopo, base de conhecimento e avisos, sem `Empresa`; conferir com teste em `AssistenteTutoriaServiceTest` que o prompt não contém nome, ramo nem sessão.
- [x] 2.2 Acrescentar ao prompt a lista do que pode e do que não pode e a recusa fixa; conferir com testes de dado de empresa pedido, "ignore as regras" e assunto fora do app.
- [x] 2.3 Criar o endpoint público de Suporte com limite por IP, tamanho máximo e sem histórico, liberado na segurança; conferir com `PublicoAssistenteSuporteTest` (200, 400, 429 e sem chamada ao modelo no 429).
- [x] 2.4 Conferir que `ClienteAssistenteChatTest` continua passando, inclusive o modo `TESTE_VIRTUAL`.

## 3. Chat flutuante

- [x] 3.1 Criar o componente `chat-flutuante` (botão 56 px com logo, painel acima, fechado por padrão, dica opcional) e conferir com `chat-flutuante.component.spec.ts` que nasce fechado e abre e fecha.
- [x] 3.2 Ajustar `assistente-cliente` para só o Suporte, com `fechavel`, evento `fechar`, perguntas rápidas que só preenchem o campo e Enviar com texto escuro; conferir com `assistente-cliente.component.spec.ts` que o Simulador não aparece.
- [x] 3.3 Trocar o assistente embutido do painel pelo chat flutuante, com o ajuste de 76 px sobre as abas no celular, sem botão na tela de Tutoriais e sem botão no admin; conferir com `painel.component.spec.ts` e `shell.component.spec.ts`.

## 4. Tutoriais

- [x] 4.1 Reorganizar `tutoriais` com chat no topo e grade de cartões (título curto, resumo, duração, passos, selo); conferir com `tutoriais.component.spec.ts`.
- [x] 4.2 Implementar o tutorial aberto (passos, passo atual, progresso, Anterior e Próximo) com o progresso e os selos em `localStorage`, com leitura protegida contra falha; conferir com teste de selo Em andamento e Concluído.
- [x] 4.3 Implementar "Perguntar ao assistente sobre este passo" só preenchendo o campo; conferir com teste de que a mensagem não é enviada sozinha.
- [x] 4.4 Atualizar `shell.content.json` e os textos de tutoriais; conferir que o `shell.component.spec.ts` continua passando.

## 5. Login e apresentação

- [x] 5.1 Colocar o chat flutuante no login e na apresentação chamando o endpoint público, com a dica na apresentação; conferir com `apresentacao.component.spec.ts` que só o Suporte aparece e que a dica existe.
- [x] 5.2 Reduzir o laranja do fundo da apresentação só no celular em `telas.design.json`, sem camada sobre a imagem; conferir no navegador em 390 px e em 1280 px, e que o desktop não mudou.

## 6. Fechamento

- [x] 6.1 Rodar os testes do backend e `ng test` e `ng build` no front e conferir que passam.
- [x] 6.2 Atualizar `openspec/specs/README.md` com as provas novas e conferir que cada requisito tem prova listada.
