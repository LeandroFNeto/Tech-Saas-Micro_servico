## 1. Contexto da empresa e contrato

- [x] 1.1 Fechar com o dono o texto de `base-comercial.md`: quem é a Gamb (startup que fecha negócio pelo WhatsApp), o que existe hoje (assistente, reserva automática, conversa humanizada com IA, fotos, preços, regras, avisos, painel), o caminho (nova conexão sob demanda, integração, automação, IoT, análise das câmeras de segurança já instaladas), como funciona a assinatura, como começar e o que o bot não promete
- [x] 1.2 Registrar a propriedade `gamb.contato.whatsapp` (`GAMB_CONTATO_WHATSAPP`, valor 5543998165047) na configuração e no compose
- [x] 1.3 Atualizar `publico-assistente-suporte.yaml` e `SuportePublicoRequestDTO.yaml` com o campo `origem` e a descrição do modo comercial

## 2. Backend

- [x] 2.1 Criar `src/main/resources/assistente/base-comercial.md` com o contexto fechado em 1.1, separando "hoje" de "caminho"
- [x] 2.2 Adicionar `origem` ao `SuportePublicoRequestDTO` (opcional, `LOGIN` ou `APRESENTACAO`, padrão `APRESENTACAO`, inválido responde 400)
- [x] 2.3 Criar `responderComercial(mensagem, origem)` no `AssistenteTutoriaService` com prompt PODE e NÃO PODE, sem `Empresa`, preço sob consulta e trava de burla reaproveitada
- [x] 2.4 Criar o texto de reserva comercial (custo, quem é, entrada de cliente, contato com e sem número) e as sugestões por origem
- [x] 2.5 Trocar a chamada do `PublicoAssistenteSuporteController` para `responderComercial`, mantendo `responderSuporte` no endpoint do cliente
- [x] 2.6 Testes em `AssistenteTutoriaServiceTest` e `PublicoAssistenteSuporteTest`: quem é, o que oferece, custo, nome de cliente, IoT, câmeras, ferramentas em desenvolvimento, integração, funcionalidade a mais junto ao WhatsApp, como virar cliente, assunto fora da Gamb, modelo fora do ar, contato com e sem número, origem válida, ausente e inválida, ausência de dado de empresa no prompt

## 3. Front

- [x] 3.1 Adicionar `origem` ao `SuportePublicoRequestDTO` e a `conversarPublico` no front
- [x] 3.2 Dar a `assistente-cliente` e `chat-flutuante` as entradas `saudacao`, `sugestoesIniciais` e `origem`, mantendo os textos atuais quando ausentes
- [x] 3.3 Colocar saudação e sugestões do login em `especificacao/front/login.content.json` e as da apresentação em `apresentacao.content.json`, e ligá-las nas duas telas
- [x] 3.4 Testes: `assistente-cliente.component.spec.ts`, `apresentacao.component.spec.ts` e `login.component.spec.ts` (sugestões por tela, painel do cliente inalterado)

## 4. Documentação e verificação

- [x] 4.1 Atualizar a linha de `assistente-tutoria` em `openspec/specs/README.md` com as provas novas
- [x] 4.2 Conferir se o limite por IP enxerga o IP real atrás do nginx; se agrupar visitantes, ler `X-Forwarded-For` em tarefa à parte
- [x] 4.3 Rodar `mvn test`, `ng test`, `ng build` e `graphify update .`, e conferir o chat no login e na apresentação a 390px e 1280px
- [x] 4.4 Depois de arquivar `ajustes-visuais-chat-flutuante-tutoriais`, reler o requisito "Suporte público no login e na apresentação" junto com os novos e ajustar o texto se ainda disser "mesmo bot de Suporte"
