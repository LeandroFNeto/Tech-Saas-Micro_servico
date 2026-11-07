# 2. Só o visual: empresa-form (bot-whatsapp-gamb-suporte-teste)

## O que fez

- Textarea "E-mails dos responsáveis da reunião" no mesmo padrão do bloco "ID da agenda": label `for="empresa-responsaveis"`, `input-field shadow-none font-mono`, ajuda em `text-brand-600`.
- `min-h-[96px]` virou `min-h-24` (96 px, utilitário padrão). `resize-y` e `break-all` para e-mail longo não estourar em 375 px.
- Erro: `text-red-700` mantido, porque é o token `campo.erro` que o perfil do cliente já usa (`clientePerfil.campo.erro`); entrou também a borda vermelha do campo (`entradaInvalida`, mesmo token da tela de funcionamento), `aria-invalid` e `aria-describedby` com ajuda, aviso de vazio e erro (`id="empresa-responsaveis-erro"`, `role="alert"`).
- Estados: normal; erro de e-mail inválido (`errors.emailInvalido`, texto do spec mantido); erro de mais de 10 (`errors.excessoEmails`, "Use no máximo 10 e-mails."); vazio (a frase "Lista vazia desliga o agendamento de reunião." vira um aviso destacado em laranja claro quando o campo está vazio); salvando (`[readOnly]="salvando()"` com fundo `brand-50` e cursor bloqueado).
- Ajuda ganhou "Até 10 e-mails." (limite `maxItems: 10` do EmpresaUpdateAdminDTO.yaml).
- Select de ramo: opções ATENDENTE_GAMB e TESTE_MODULOS já estavam certas; sem mudança.
- `telas.design.json`: nova seção `telas.adminEmpresaForm.campo` com as classes do bloco (rótulo, entrada, área de texto, inválida, dica, destaque, erro), pronta para o TS importar como o perfil faz.

## Arquivos tocados

- frontend/src/app/features/admin/empresa-form/empresa-form.component.html
- especificacao/front/telas.design.json

## O que o próximo precisa saber

- Fase 3 (TS): o validador `emailsValidos` só devolve `emailInvalido`. Falta devolver `{ excessoEmails: true }` com mais de 10 e-mails; o HTML já mostra a mensagem quando essa chave existir.
- Opcional na fase 3: importar `telas.design.json` e trocar as classes literais por `design.campo.*` (como `client/perfil`). Hoje o HTML usa as mesmas classes em texto.
- `form.invalid` bloqueia o salvar mesmo com o campo escondido (empresa de outro ramo); só acontece se vier valor inválido do backend. Anotado, não mexido.
- Verificação: `npm run build` ok; `ng test` do empresa-form 16/16 ok; Tailwind gerou as classes novas. Não abri no navegador: a tela de edição precisa de login ADMIN e da API de pé. Layout sem largura fixa (`min-w-0`, `w-full`), sem risco de rolagem lateral em 375/768/1280, mas não visto ao vivo.

## Lacunas

- Admin sem `*.content.json`: textos do bloco seguem no HTML, como o "ID da agenda".
- Textos novos: "Até 10 e-mails." e "Use no máximo 10 e-mails." (pedido da tarefa, baseado no contrato).

Estado: aprovado
