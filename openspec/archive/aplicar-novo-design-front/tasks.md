# Tarefas

- [ ] Contrato YAML: sem mudança em `especificacao/back/` nem em `swagger.yaml`
- [ ] Java: sem mudança de produto
- [ ] Front, na ordem da skill `.cursor/skills/diasabatico/SKILL.md`, com a referência em `especificacao/front/referencia-canvas/`:
  - [ ] Fase 1, inventário: rotas, `routerLink`, componentes e o que falta; grava `diasabatico/1-inventario.md`
  - [ ] Fase 2, visual (Opus): `styles.css` (`.switch`, `.chip`, `.wordmark`, `.input-field`), `telas.design.json` (`accent-800`), `*.content.json` novos, logo `.png` e imagem da apresentação em `frontend/public/`, `index.html`; apresentação e login; shell; empresas; cliente; avisos; perfil. Dados de exemplo só no componente, sem chamar API
  - [ ] Fase 3, navegação: rota `''`, `**` → `''`, redirecionamento de quem já tem sessão, links de ida e volta entre apresentação, login e painéis
  - [ ] Fase 4, auditoria: telas usadas e disponíveis, estados vazio/erro/sucesso, 375 / 768 / 1280 px, `npm run build` e `npm test -- --watch=false --browsers=ChromeHeadless` em `frontend/`; grava `4-auditoria.md` com as lacunas
  - [ ] Fase 5, entrega: quadro final em `lacunas-front.md` desta pasta
- [ ] Prova: `apresentacao.component.spec.ts` (link Entrar para `/login`; quem tem sessão vai ao painel) e a suíte existente do front passando
- [ ] Atualizar `openspec/specs/auth/spec.md` com AUTH-03 (página pública de apresentação) e a linha de prova em `openspec/specs/README.md`
- [ ] Mover esta pasta para `openspec/archive/aplicar-novo-design-front/`
- [x] Deploy conforme a `horadoshow`: `producao` recebe só `frontend/` e `vercel.json`; `main` recebe os arquivos da tarefa, a spec definitiva e a central
