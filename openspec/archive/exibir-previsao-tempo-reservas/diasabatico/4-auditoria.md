# Fase 4: auditoria (exibir-previsao-tempo-reservas)
Agente novo, só leitura de produto. Navegador não aberto: Responsivo e Disponível foram por leitura de rotas, markup, classes e specs.
## Verificação (resultado literal)
- `npm run build` (frontend): "Application bundle generation complete. [2.840 seconds]"; chunks lazy `painel-component` (16.03 kB) e `perfil-component` (25.08 kB); sem erro nem aviso.
- `npm test -- --watch=false --browsers=ChromeHeadless`: "TOTAL: 16 SUCCESS" (Chrome Headless 156, 16 de 16).
## Tabela por tela
| Tela | Disponível | Alcançável | Estados | Texto | Estilo | Responsivo | Acesso | Dado |
|---|---|---|---|---|---|---|---|---|
| Painel `/cliente` | ok (lazy, guard CLIENTE na rota pai) | ok (menu, abas, marca) | ok: 9 estados + reservas indisponíveis | ok | ok | ok (leitura) | ok | depende de API |
| Chatbot `/cliente/chatbot` | ok | ok (menu, abas, "Voltar ao painel" -> `/cliente`) | herdados do perfil | ok | ok | ok (leitura) | ok | sem campo novo |
| Shell cliente | ok | ok: lateral (lg) e barra inferior (celular/tablet) | n/a | falha leve | ok | ok | ok | n/a |
## Notas
- Disponível: `client.routes.ts` tem `''` -> PainelComponent e `chatbot` -> PerfilComponent sob ShellComponent. `**` cai em `''`, não conta.
- Texto: no `shell.component.html` "Painel" e "Configuração do chatbot" (menu lateral e abas) e "Sair" estão fixos; admin já era assim, não há `content.json` do shell. Menor, não bloqueia.
- Estilo: nenhuma classe de cor fora dos tokens; `risco-*` vem de `telas.design.json` via `tailwind.config.js` (`risco: design.tema.cores.risco`). Inline só `min-w-0`, `switch` (styles.css) e `sr-only`.
- Responsivo: lista de dias `grid-cols-2` (375) e `md:grid-cols-4` (768/1280); linha WhatsApp e chatbot quebra com `flex-wrap` e base 320 px; abas inferiores com 2 itens cabem em 375 px. Texto 13 px só em dicas, campos usam `input-field`.
- Acesso: `label for` no campo da cidade e no seletor, `aria-describedby`, `aria-invalid`, `role=alert/status`, ícones SVG `aria-hidden`, risco com texto além da cor, link externo com aviso "abre em nova aba". Sem pendência.
## Dado (depende de API nova, YAML antes do Java)
- Dias, risco, `reservado`, `estimativa`, `atualizadoEm`: `PrevisaoTempoResponseDTO` e `PrevisaoDiaDTO` ainda não existem em `especificacao/back/schemas/` (não há pasta `previsao`).
- Cidade, cidade resolvida e interruptor: faltam em `EmpresaUpdateDTO` e `EmpresaResponseDTO`. Erros 400, 429 e 503 têm texto no content.json, sem uso.
## Lacunas
1. Seletor "Só para visualização" (estado e "Simular reservas indisponíveis") no topo do painel: sai quando ligar à API (tarefa "Front, ligação à API").
2. Dados de exemplo fixos (8 dias, "Atibaia, São Paulo, BR", "14:05"); salvar da cidade só troca o estado local.
3. Avisos (banner) só na configuração do chatbot; painel sem eles (fase 2, fora de escopo).
4. Clique real no navegador não foi feito nas fases 3 e 4; ida e volta só por leitura.
5. Painel monta `app-conexao-whatsapp`, que chama a API real; ConexaoWhatsapp também serve o admin (EmpresaForm).
6. `lembrar-confirmacao-reserva` ainda disputa o board do cliente (design.md, riscos).
7. Texto fixo no shell (ver Notas).
## Veredito
Sem lacuna de rota, build e testes verdes. As lacunas acima são do escopo declarado (só visual, sem API) e seguem para a fase 5.
Estado: aprovado
