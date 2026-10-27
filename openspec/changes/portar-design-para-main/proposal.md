# portar-design-para-main

## Problema

O dono aprovou o redesenho do front (apresentação pública, login novo, painéis responsivos mobile first, logo nova). A equipe o aplicou na branch `producao` (commit `cbc6fdd`), que a Vercel publica só como Preview. O site no ar vem da `main`, e a `main` tem um front mais novo que a `producao`: perfil no celular, Cloudinary, editor do WhatsApp e status ao vivo no admin. As duas branches divergem desde `449d852`, então empurrar a `producao` para a `main` apagaria o trabalho novo.

Efeito para o dono hoje: quem abre o site cai direto no login, sem ninguém explicar o que é o gamb. Além disso, no painel "Clientes cadastrados" o status aparece "DISCONNECTED" e não atualiza. A causa está no dashboard novo da `producao`, que perdeu a consulta de status que a `main` faz a cada 5 s (`WhatsappConnectionService.consultarStatus`, sinal `statusPorSessao`, método `statusDe`).

## O que muda
### Entra
- Apresentação pública "Sobre" em `/`, com botão **Entrar** para `/login`.
- Login novo, shell novo (abas embaixo no celular, barra lateral no desktop), Empresas, Editar cliente, Avisos e Meu perfil no padrão do canvas, logo `logo-gamb.png` e `conversa-whatsapp.jpg`.
- `apresentacao.component.spec.ts` e o requisito AUTH-03 (página pública de apresentação).
- Status ao vivo no cartão (celular) e na linha (desktop) da lista de clientes, em chip com a cor do desenho.

### Altera
- Rota `''` passa a abrir `ApresentacaoComponent`; `**` redireciona para `''`.
- Os arquivos que a `main` mexeu (`dashboard`, `empresa-form`, `perfil`, `styles.css`, `app.routes.ts`, `tailwind.config.js`) recebem o design por cima do código da `main`, não o contrário. Perfil no celular, Cloudinary, editor do WhatsApp e polling de status da `main` continuam.
- `openspec/central-tarefas.md` da `main` recebe a linha desta change, sem apagar as existentes.

### Sai
- `logo-gamb.jpg` (vira `docs/logo-gamb-antiga.jpg`, como na `producao`).
- A linha "[CONTATO DA GAMB]" fica fora do build até o dono informar o contato.

## Fora de escopo
- Mudança de API, DTO, Java ou YAML.
- Busca e filtro na lista de empresas; desenho próprio da tela "criar cliente".
- Merge da branch `producao` na `main`. O porte é feito por arquivo, a `producao` fica como está.
- Subir para a `main` (publica o site no ar): só com confirmação explícita do dono, depois do trabalho pronto e provado na branch de trabalho.

## Domínios
- spec: `openspec/specs/auth/spec.md` (AUTH-03)
