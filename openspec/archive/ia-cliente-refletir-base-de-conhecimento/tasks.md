# Tarefas

## Conteúdo
- [x] Confirmar com o dono do projeto o "ponto" do documento base que a IA deve refletir (seção 7, histórias US01 a US06 ou seção 1) e ajustar a proposta se for outro
- [x] Criar `src/main/resources/assistente/atendimento-empresa.md` com o recorte do documento para o cliente final (ideia do produto e regras de conduta)

## Back
- [x] `ServicoIA`: carregar o recurso uma vez (com falha segura) e incluí-lo no prompt
- [x] Escrever no recurso a persona humana (quem fala, tom, variação, quando lembrar do menu) no lugar do roteiro de 2 a 3 frases
- [x] `ServicoIA.construirPromptEmpresa`: usar o menu real da empresa no lugar de "1, 2 ou 3", listar módulos ativos e modo de locação, e acrescentar a conduta (só dados cadastrados, não confirmar reserva, não expor credencial)
- [x] `LocacaoStrategy`: passar o menu da empresa para a IA nas duas chamadas

## Provas
- [x] `ServicoIATest`: o prompt traz o menu real e não traz "digite 1, 2 ou 3" fixo; empresa sem agenda não cita opção de agenda; traz módulos e modo de locação; traz a conduta; não traz credencial nem ID de agenda
- [x] `ServicoIATest`: o prompt não impõe teto de frases nem fechamento fixo com o menu, e traz a orientação de tom (responder o que foi perguntado, acompanhar o cliente, variar)
- [x] `ServicoIATest`: recurso ausente cai nas instruções corrigidas; IA-06 continua
- [x] `LocacaoStrategyTest`: a IA é chamada com o menu da empresa
- [x] Rodar `mvn test` (693 testes passam)
- [x] Conferir uma conversa real com uma empresa com IA ligada: mandar a mesma saudação em tons diferentes (formal, informal, só "oi") e ver se as respostas variam e soam naturais

## Datas livres
- [x] `InterpretadorDeData` com os formatos, palavras relativas, dia da semana e casos duvidosos
- [x] `GerenciadorSessao` e `EstadoUsuario`: data pendente e estado `ESPERANDO_CONFIRMACAO_DATA`
- [x] `LocacaoStrategy`: ler data livre antes da IA, nos estados de espera de data e na reserva, e perguntar na dúvida
- [x] `InterpretadorDeDataTest` e novos testes em `LocacaoStrategyTest`; ajustar o teste que esperava "Não entendi essa data" para "amanhã"

## Fechamento
- [x] Atualizar `docs/base-conhecimento-gamb.md` e o recurso juntos, se algo do recorte mudar
- [x] Sincronizar `openspec/specs/ia/spec.md` com o delta
- [x] Mover esta pasta para `openspec/archive/ia-cliente-refletir-base-de-conhecimento/`
