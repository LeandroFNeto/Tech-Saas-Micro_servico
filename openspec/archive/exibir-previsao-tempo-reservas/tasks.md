# Tarefas

## Antes da programação (a pessoa faz, fora do código)

- Criar a conta no OpenWeatherMap, assinar o One Call 3.0, limitar a 1.000 chamadas por dia, confirmar os termos do plano gratuito e guardar a chave em `OPENWEATHER_API_KEY` no `.env`. Sem isso, os testes passam com o provedor simulado, mas a tela real mostra "Previsão indisponível".

## Programação

- [x] Front, canvas: dividir o board `ClientePerfil` em `ClientePainel` e `ClienteChatbot` no artefato "Painel gamb — telas" antes do Angular
- [x] Front, `diasabatico` (só visual e navegação, dados de exemplo, sem API): `cliente-painel.content.json`, `cliente-perfil.content.json`, `telas.design.json`; `features/client/painel/` com conexão do WhatsApp, cartões da previsão com selo de risco, "reservado", "estimativa" e atribuição, cidade e interruptor no cartão, botão "Configuração do chatbot"; `/cliente` e `/cliente/chatbot` em `client.routes.ts`; menu do cliente
- [x] Contrato YAML: campos em `EmpresaUpdateDTO`/`EmpresaResponseDTO`, `PrevisaoTempoResponseDTO`, `PrevisaoDiaDTO`, `paths/empresas-previsao.yaml` (200, 401, 403, 404), 400, 429 e 503 em `empresas-cliente.yaml` e `$ref` em `especificacao/back/swagger.yaml`
- [x] Java: campos em `Empresa` (`previsaoTempoAtiva` nulo vale `false`), `ProvedorPrevisao`, `ProvedorOpenWeather`, `ServicoPrevisaoTempo` com cache de 8 h por coordenada e `CalculadoraRiscoDesmarcacao`
- [x] Java: `listarReservas` devolve os 30 dias "Disponível" quando a agenda não tem eventos; fuso `America/Sao_Paulo` no cruzamento das datas
- [x] Java: geocodificação no PUT do cliente (400, 429 após 5 trocas por dia, 503) e GET `/empresas/{sessao}/previsao` que só aceita admin ou o cliente da própria empresa
- [x] Infra: `OPENWEATHER_API_KEY` em `application.properties`, `docker-compose.yml` e `.env.example`
- [x] Front, ligação à API: serviço Angular do GET previsão e do PUT da cidade; erros 400, 429 e 503 no cartão; aviso "Previsão indisponível no momento" e "Reservas indisponíveis no momento"
- [x] Prova: `CalculadoraRiscoDesmarcacaoTest`, `ServicoPrevisaoTempoTest`, cenários em `empresas.feature` e testes Angular do painel (atribuição, botão, conexão do WhatsApp) e do perfil ajustado, conforme `design.md`, seção Arquivos

## Fechamento (não é da programação)

- [x] Revisão: gravar EMP-05 e EMP-06 em `openspec/specs/empresa/spec.md` e a linha em `openspec/specs/README.md`
- [x] Deploy: mover esta pasta para `openspec/archive/exibir-previsao-tempo-reservas/`
