# 2-visual: conexao-whatsapp-por-codigo (tarefas 1.2 e 1.3)

## O que fez
- Canvas "Painel gamb — telas" (https://claude.ai/artifact/J891R7bxYLZtgv2LJsn47p), grupo novo "Conexão WhatsApp por código", sem apagar boards antigos.
- `ConexaoWhatsappCartao`: protótipo clicável do componente (Tweaks: passo, erro, compacto, corAndamento, segundos). É a fonte que todos os outros boards importam.
- Catálogos 1280 e 375, normal e compacto: 9 passos + telefone gerando, codigo Copiado, codigo últimos 30 s; erros 400/403/404/409 preparando/409 iniciando/429/503/sem internet (compacto sem 404).
- `AdminDashboardChipWhatsapp` (1280 e 375): chip "Reconectando" para INITIALIZING.
- Textos em `especificacao/front/conexao-whatsapp.content.json`; classes em `telas.design.json` (`telas.conexaoWhatsapp`, `telas.adminDashboard.chipWhatsapp`).

## Arquivos tocados
- `especificacao/front/conexao-whatsapp.content.json` (novo)
- `especificacao/front/telas.design.json` (blocos `conexaoWhatsapp` e `adminDashboard`)
- Canvas: 11 boards novos `ConexaoWhatsapp*` e `AdminDashboardChipWhatsapp*`, mais `canvas.json`

## O que o próximo precisa saber
- Etapas visíveis só em telefone, codigo, codigo-expirado e conectando: Número, Código, Conectar. No compacto vira "Passo 2 de 3 · Código".
- Código: uma caixa só com `ABCD-EFGH` em mono, `select-all`, `tabindex="-1"`, `aria-live="polite"`, foco ao aparecer. Botão Copiar vira "Copiado" por 2 s.
- Contagem: texto "Vale por mais m:ss" + barra; nos últimos 30 s fica âmbar. Leitor de tela só ouve a contagem em 1:00 e 0:30 (`anuncioMinuto`, `anuncioTrintaSegundos`), nunca a cada segundo.
- Expirado: código riscado, "Gerar outro código" vira botão principal, QR segue como link.
- Conectando e reconectando: sem botão, giro com `motion-reduce:animate-none`. Chip "andamento" azul (`blue-50/blue-800`), opção roxa em `andamentoRoxo` se a pessoa preferir.
- Erros 403/404/429/409 iniciando em aviso no topo do cartão; 400 no campo (`aria-invalid`, `aria-describedby`); 409 preparando é linha com giro e botão "Preparando…" desabilitado.
- 503 abre qrcode com aviso e sem "Voltar para o código".
- Dashboard: "Consultando..." passa de `text-brand-500` para `text-brand-600` e "WhatsApp Online" de `accent-700` para `accent-800` (contraste abaixo de 4,5:1 hoje).
- Cores azul e âmbar são da paleta padrão do Tailwind (o `tailwind.config.js` só mapeia brand, accent e risco); não precisa mexer no config.
- Exemplos: telefone `+55 (11) 90000-0000`, código `ABCD-EFGH`, sessões `sessao_exemplo_0N`. Nunca dado real.
- Textos novos escritos por mim (aprovar): descrição do cabeçalho, texto do inicio, instruções com o caminho Android/iPhone, explicação do repareamento, anúncios de 1:00/0:30. Nenhum [PLACEHOLDER].
- Aprovação da pessoa no canvas (tarefa 1.3) dada, com a cor azul escolhida.

Estado: aprovado
