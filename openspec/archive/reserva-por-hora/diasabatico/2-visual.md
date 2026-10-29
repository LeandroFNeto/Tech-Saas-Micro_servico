# Visual: reserva-por-hora (aba Funcionamento)

## O que fiz
- Aba "Funcionamento" no PerfilComponent (/cliente/chatbot), entre "Configurações do Robô" e "Configuração", como no canvas (boards ClienteChatbot, ClienteFuncionamento e ClienteFuncionamentoMobile, versão 1793231914-b9dd).
- Componente standalone novo `FuncionamentoComponent` (`app-funcionamento`): modo simples (Abre às, Fecha às, Duração 30 min a 12 h de 30 em 30) e "Personalizar" (8 dias com chave, Abre, Fecha e Duração com "Usar a geral"; Intervalo; UF; Dias especiais com selo e Remover; Novo dia especial; aviso "Há reservas nesta data").
- Estados no signal `estado` via `mudarEstado()`: normal, carregando, vazio, erro (com "Tentar de novo"), sucesso, salvarDesabilitado, reservasNaData (abre o Personalizar).
- Validação local: Salvar desabilitado se fechar não é depois de abrir (geral e por dia), duração fora de 30–720/30, intervalo fora de 0–120 inteiro, UF fora das 27. Data especial de hoje até hoje+180 (min/max no campo e erro), motivo até 120, horário do dia especial quando não está fechado.
- Recomendação de design: cores do canvas trocadas pelos tokens mais próximos da paleta (sem cor nova). Dia fechado em `brand-50`, selo fechado roxo claro, selo horário laranja claro, aviso em `accent-50`, erro em vermelho claro.

## Arquivos tocados
- novo: `frontend/src/app/features/client/funcionamento/funcionamento.component.ts`, `.html`, `.spec.ts`
- `frontend/src/app/features/client/perfil/perfil.component.ts` (tipo da aba e import), `.html` (botão da aba; o form de dados/robô agora só abre em `dados` ou `robo`), `.spec.ts` (aba funcionamento no teste de fonte 16px)
- `especificacao/front/cliente-perfil.content.json` (`abas.funcionamento` e bloco `funcionamento`)
- `especificacao/front/telas.design.json` (`telas.clientePerfil.funcionamento`)

## Para a fase 3
- Sem rota nova: a aba é estado local (`aba` signal do perfil). Falta só decidir se a aba abre por query (ex.: `?aba=funcionamento`) vindo do painel.
- `estado` e `personalizarAberto` são signals públicos; dados de exemplo em constantes no `.ts` (`HORARIOS_EXEMPLO`, `ESPECIAIS_EXEMPLO`, `AVISO_EXEMPLO`).

## Para a ligação com a API
- `GET/PUT /empresas/{sessao}/funcionamento` preenche `abre`, `fecha`, `duracao`, `horarios`, `intervalo`, `uf`, `especiais`; carregando/erro/vazio/sucesso viram resposta real.
- `POST /dias-especiais` em `adicionarEspecial()`; `reservasNaData` da resposta alimenta `aviso`. `DELETE /dias-especiais/{id}` em `removerEspecial()`.
- Modo simples grava o mesmo abre/fecha nos 7 dias e no feriado; duração "Usar a geral" vai como `null`.
- [PLACEHOLDER] de exemplo: [MOTIVO] e [EVENTO DA AGENDA].

## Verificação
- `npm run build`: ok, sem erro nem aviso.
- `npm test -- --watch=false --browsers=ChromeHeadless`: 56 de 56 ok (7 novos do Funcionamento: modo simples, Personalizar, salvar desabilitado, intervalo, data especial, aviso, estados).
- Não abri no navegador em 375/768/1280: layout feito com grid que empilha no celular e `min-w-0`; conferir na auditoria.

Estado: aprovado
