# exibir-previsao-tempo-reservas

## Problema

Em espaço aberto (chácara, área de lazer, quadra), chuva forte no dia é um dos principais motivos de desistência. Hoje o dono não tem esse aviso no painel e só percebe o risco quando a pessoa cancela. Ver a previsão dos próximos dias, com os dias já reservados destacados, permite que ele aja antes: confirmar com quem reservou, oferecer outra data ou reforçar o contato.

Hoje a primeira tela do cliente é o formulário de configuração do chatbot, e a conexão do WhatsApp fica espremida no topo dele. Falta uma tela inicial que mostre, de relance, se o WhatsApp está conectado e como vem o tempo, e leve à configuração só quando o dono quiser mexer nela.

## O que muda

### Entra

- Painel inicial do cliente (rota `/cliente`), com três blocos: conexão do WhatsApp, previsão do tempo e um botão "Configuração do chatbot" que leva à tela de configuração.
- Conexão do WhatsApp no painel inicial: o componente que hoje fica no perfil passa a ocupar um cartão do painel, com o estado conectado ou desconectado e a ação de conectar. A regra de conexão (WPP-04) não muda.
- Configuração do chatbot (rota `/cliente/chatbot`): a tela atual do perfil, sem o bloco de conexão do WhatsApp e sem a previsão. Mensagem de saudação, preços, galeria, regras, reserva automática e senha continuam como estão, com um link "Voltar ao painel".
- Localização do espaço no cartão da previsão do painel inicial: o cliente digita a cidade. O sistema resolve latitude e longitude do centro da cidade pela geocodificação do provedor de previsão e mostra o nome, o estado e o país encontrados para ele conferir. Bairro não é usado. Cada empresa pode trocar a cidade até 5 vezes por dia.
- Interruptor "Mostrar previsão do tempo", desligado por padrão. Um espaço coberto pode deixar desligado.
- Bloco "Previsão dos próximos dias" no painel inicial do cliente, com um cartão por dia (até 8 dias, limite do plano gratuito do provedor):
  - dia e dia da semana;
  - marcação "Reservado" quando a data está ocupada no Google Agenda da empresa;
  - ícone e resumo do tempo (ensolarado, nublado, chuva, temporal);
  - temperatura máxima e mínima;
  - chance de chuva (%) e chuva prevista (mm);
  - indicador de risco de desmarcação: **baixo**, **médio** ou **alto**, destacado nos dias reservados.
- Regra do indicador (pela previsão diária):
  - **alto**: chance de chuva ≥ 70%, ou temporal, ou chuva prevista ≥ 10 mm;
  - **médio**: chance de chuva entre 40% e 69%, ou chuva prevista entre 2 mm e 9,9 mm;
  - **baixo**: o resto.
  - A chance é arredondada para inteiro e a chuva para 1 casa decimal antes de comparar.
- A previsão de cada cidade é atualizada no máximo a cada 8 horas e compartilhada entre as empresas da mesma cidade. A marcação "Reservado" é recalculada a cada abertura da tela. A tela mostra "Atualizado às HH:MM".
- Só o cliente da própria empresa (ou o admin) consulta a previsão.

### Altera

- `EMP-02`: `EmpresaUpdateDTO` e `EmpresaResponseDTO` ganham localização e o interruptor da previsão.
- Navegação do cliente: `/cliente` deixa de abrir o formulário e passa a abrir o painel inicial. O formulário atual vai para `/cliente/chatbot`. O menu do cliente ganha os dois destinos.

### Sai

Nada.

## Fora de escopo

- Mensagem automática para quem reservou falando do tempo.
- Outros cartões no painel inicial (reservas do dia, contatos, indicadores). O painel nasce com três blocos.
- Lembrete de confirmação e lista de reservas com nome e telefone (change `lembrar-confirmacao-reserva`, depois desta).
- Previsão por hora para locação por hora: o Google Agenda hoje só informa o dia ocupado.
- Histórico de cancelamentos para calibrar a regra. A primeira versão usa limites fixos.

## Domínios

- spec: `openspec/specs/empresa/spec.md` (EMP-05 configurar localização e previsão, EMP-06 painel inicial do cliente com conexão do WhatsApp, previsão com risco e atalho para a configuração do chatbot)
- spec: `openspec/specs/whatsapp/spec.md` (WPP-04, só leitura: a conexão do WhatsApp muda de lugar na tela, não de regra)

Não depende de outra change. Usa a ocupação que o Google Agenda já fornece (`ServicoReserva.listarReservas`).
