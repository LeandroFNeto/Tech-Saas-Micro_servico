# Lacunas do front: exibir-previsao-tempo-reservas

Quadro final da fase 5 (entrega) da skill diasabatico, coluna Claude. Fonte: `diasabatico/1-inventario.md`, `2-visual.md`, `3-navegacao.md` e `4-auditoria.md`. Nenhum arquivo de produto foi alterado.

## 1. O que ficou pronto, por tela

- **Painel `/cliente`**: tela nova com cartão do WhatsApp (reusa `app-conexao-whatsapp`), cartão de previsão com 8 dias, selo de risco e botão para o chatbot. Tem 9 estados visuais e o estado de reservas indisponíveis, com dados de exemplo. Rota e guard CLIENTE conferidos. Build e testes verdes.
- **Chatbot `/cliente/chatbot`**: antigo perfil, sem a conexão do WhatsApp. Ficam banner, abas Dados, Robô e Configuração (senha), com "Voltar ao painel". Ida e volta conferida por leitura.
- **Menu do cliente (shell)**: "Painel" e "Configuração do chatbot" na barra lateral (desktop) e na barra inferior (celular e tablet). Resolve a lacuna de cliente sem menu no celular.
- **Rota `design-v0`**: sem link, segue como área de teste. Não é lacuna.

## 2. Lacunas

| Tela | O que falta | Quem resolve |
|---|---|---|
| Painel | Seletor "Só para visualização" no topo (estado e "Simular reservas indisponíveis"). Sai quando a tela ligar à API. | ligação à API |
| Painel | Dados de exemplo fixos: 8 dias, cidade "Atibaia, São Paulo, BR", "Atualizado às 14:05". O salvar da cidade só troca o estado local. | ligação à API |
| Painel | Mensagens de erro 429 e 503 já estão no `content.json`, sem uso. | ligação à API |
| Painel | Avisos (banner) só aparecem na configuração do chatbot. Levar ao painel pede o `AvisoService`. | ligação à API (e decisão, ver item 4) |
| Painel e Chatbot | Estados da conexão do WhatsApp (conectado, aguardando, desconectado) vêm da API de status real. Confirmar contra o contrato atual. | contrato |
| Shell (cliente) | Textos fixos "Painel", "Configuração do chatbot" e "Sair". Não há `content.json` do shell. Admin já é assim. Não bloqueia. | texto |
| Shell e admin | `ConexaoWhatsapp` também serve o cadastro de empresa do admin. Mudança nele afeta o admin. Conferir a tela admin antes de dar a entrega. | visual (checagem) |
| Chatbot | Estados herdados do perfil. Nenhum campo novo. | sem lacuna |
| Todas | Clique real no navegador não foi feito nas fases 3 e 4. Ida e volta só foi verificada por leitura de rotas, links e specs. Também falta a checagem visual em 375, 768 e 1280 px. | verificação (navegador) |
| Board do cliente | `lembrar-confirmacao-reserva` também mexe no board `ClientePerfil` (ver `design.md`, riscos). Disputa o mesmo arquivo de tela. | decisão (item 4) |

## 3. O que depende de API (pedido de contrato)

Nada disso fecha com o front atual. Pedido de contrato em YAML antes do Java (`openapi-contract-first.mdc`). Nenhum código nesta skill.

| Campo ou estado | Onde falta hoje | O que o contrato precisa dizer |
|---|---|---|
| Cidade (editável) | `EmpresaUpdateDTO` e `EmpresaResponseDTO` | campo de texto da cidade e como é salva |
| Cidade resolvida | não existe | nome encontrado pela API de clima, e o estado "cidade não encontrada" |
| Interruptor (ligar ou desligar previsão) | `EmpresaUpdateDTO` e `EmpresaResponseDTO` | booleano e o que acontece quando desligado |
| Previsão (lista de dias) | `PrevisaoTempoResponseDTO` e `PrevisaoDiaDTO` não existem em `especificacao/back/schemas/` | dias, data, estimativa e condição |
| Risco por dia | `PrevisaoDiaDTO` (não existe) | valores baixo, médio, alto |
| Reservado por dia | `PrevisaoDiaDTO` (não existe) | booleano por dia |
| Atualizado em | resposta da previsão (não existe) | data e hora da última consulta |
| Reservas indisponíveis | não existe | de qual endpoint vem o sinal (previsão ou reservas) |
| Erros da previsão | content.json tem texto para 400, 429 e 503 | códigos e formato do erro |
| Conexão real do WhatsApp | serviço já existe (`WhatsappConnectionService`) | confirmar que o status tem os três estados usados no painel |

## 4. O que a pessoa precisa decidir

1. **Disputa com `lembrar-confirmacao-reserva`.** As duas mudanças mexem no board do cliente. Escolha uma ordem: (a) fechar esta change antes e a outra se ajusta depois, ou (b) parar as duas e ajustar o board juntas. Sugestão: fechar esta primeiro, porque a tela já está pronta e a outra depende dela. Confirmar no `design.md` antes de decidir.
2. **Clique real no navegador em 375, 768 e 1280 px.** Ainda não foi feito. Autorizar agora (exige o front rodando em local) ou aceitar a entrega só com leitura e build.
3. **Seletor "Só para visualização".** Manter até a API existir, para demonstração, ou tirar já.
4. **Avisos no painel.** Levar o banner ao painel ou deixar só na configuração do chatbot.
5. **Textos fixos do shell.** Manter como o admin, ou criar `content.json` do shell agora.
