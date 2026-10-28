# Revisão: exibir-previsao-tempo-reservas

Conferido contra a proposta: código Java, YAML, Angular e provas, mais `qualidade.md` e `seguranca.md` (ambos aprovados na volta 3).

## Cumpre a proposta
- Painel em `/cliente` com conexão do WhatsApp, previsão e atalho para `/cliente/chatbot`; configuração do chatbot em `/cliente/chatbot`; menu do cliente com os dois destinos.
- Cidade e interruptor no cartão da previsão; interruptor nulo vale desligado.
- PUT do cliente geocodifica a cidade nova: 400, 429 na sexta troca e 503 com estorno, sem gravar nada.
- `CalculadoraRiscoDesmarcacao` segue os limites da proposta com arredondamento antes de comparar; temporal pelo código 200-299.
- Cache de 8 h por coordenada no `ServicoPrevisaoTempo`, ocupação recalculada a cada consulta, fuso `America/Sao_Paulo`.
- GET `/empresas/{sessao}/previsao` só para admin ou cliente da própria empresa; 401 sem token, 403 igual para sessão inexistente e empresa alheia, 404 só ao admin.
- Atribuição OpenWeather e "Atualizado às" no rodapé dos cartões; avisos de previsão e reservas indisponíveis.
- `googleCalendarId` só para o admin.

## Gravado
- `openspec/specs/empresa/spec.md`: EMP-05 e EMP-06. EMP-01 a EMP-04 intactos.
- `openspec/specs/README.md`: provas novas na linha "Cadastro e perfil".
- `design.md`, decisão 9: 403 para quem não é admin, 404 só ao admin.

## Pendente
- Deploy: mover a pasta para `openspec/archive/`.
- A pessoa cria a conta OpenWeather e a chave em `OPENWEATHER_API_KEY`.
- Testes não foram rodados de novo nesta etapa.

Estado: aprovado
