# Auditoria: reserva-por-hora (aba Funcionamento em /cliente/chatbot)

Fase 3 sem troca: a aba é estado local (`aba` signal do PerfilComponent), sem rota nova nem guard novo. Auditado: `FuncionamentoComponent`, `PerfilComponent`, `cliente-perfil.content.json`, `telas.design.json` (`clientePerfil.funcionamento`).

## Verificação
- `npm run build` (frontend): exit 0, sem erro.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 56 de 56 ok, exit 0.
- Código novo sem comentário (`//`, `/*`, `<!--` não achados) e sem `HttpClient` nem `core/services` no Funcionamento. Navegador não aberto: 375/768/1280 só por leitura das classes.

## Achados
| Item | Resultado | Gravidade |
|---|---|---|
| Disponível | rota `cliente/chatbot` com authGuard + roleGuard('CLIENTE'); aba abre por clique. Console não conferido no navegador | baixa |
| Alcançável | botão da aba com `role=tab`; volta pelas outras abas e pelo link `/cliente` do perfil. Painel não abre direto nesta aba | baixa |
| Estados | normal, carregando, vazio, erro, sucesso, salvar desabilitado e reservas na data presentes. "Tentar de novo" só volta ao normal (visual) | ok |
| Texto | todo texto vem do content.json. Fica no HTML só o separador " / " do contador de motivo | baixa |
| Estilo | classes só dos tokens do `telas.design.json`; `class="switch"` fixo no HTML do "Fechado o dia todo" (as demais chaves vêm de `visual.dia.chave`) | baixa |
| Responsivo | grids empilham (`grid-cols-1 sm:grid-cols-3`, `auto-fit`, `min-w-0`). Risco em 375 px: linha do dia com 3 colunas (`grid-cols-3`, `px-2`) corta "Usar a geral". Sem teste em tela | média |
| Acesso | todo campo com label ou aria-label; foco da chave tratado; `role=alert` nos erros. Faltam: `aria-invalid` em "Abre às" e no horário do novo dia; `func-nova-data-erro` e o erro do horário não ligados por `aria-describedby` | baixa |
| Dado | nomes, HH:mm, yyyy-MM-dd, duração 30–720 de 30 em 30, intervalo 0–120, motivo 120, UF e 180 dias batem com os DTOs. Diferença: `step="5"` no intervalo (o DTO aceita qualquer inteiro de 0 a 120) | baixa |

## Depende da API
- `GET/PUT /empresas/{sessao}/funcionamento`: `abre`, `fecha`, `duracao`, `horarios`, `intervalo`, `uf`, `especiais`; carregando, erro, vazio e sucesso viram resposta real.
- Modo simples grava o mesmo abre/fecha nos 7 dias e no feriado; "Usar a geral" vai como `duracaoMinutos: null`; `fechado` vem de `!aberto`.
- `POST/DELETE` de dias especiais; `reservasNaData` (lista de texto, sem data) alimenta o aviso, e a data vem do POST.
- Janela de 180 dias calculada no relógio do navegador; o servidor decide o limite final.

## A pessoa decide
- O painel `/cliente` deve abrir direto nesta aba (`?aba=funcionamento`)?
- `step="5"` no intervalo fica ou sai (o contrato não pede)?
- Textos de exemplo `[MOTIVO]` e `[EVENTO DA AGENDA]` seguem como placeholder.

Estado: aprovado
