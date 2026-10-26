# Design

## Decisão

Uma passagem pelos requisitos já numerados (`AUTH`, `EMP`, `WPP`, `AGD`, `AVI`, `IA`). Para cada um: o YAML citado em `Contrato:` é o comportamento; a prova citada em `Prova:` precisa passar. Onde a prova ainda não existe, ela entra com o cenário que o spec já descreve. Onde o Java discorda do YAML, corrige-se o Java.

Nenhuma rota nova. Nenhum `$ref` novo em `especificacao/back/swagger.yaml`.

## Arquivos
- Contrato: os paths que os specs já citam (`auth`, empresas, webhook, agenda, avisos, `especificacao/back/paths/ia-query.yaml`). Sem arquivo novo.
- Java: controllers, serviços e a consulta `ConsultaEmpresaGrafo` / `POST /api/ia-query`, só no ponto em que a prova mostrar divergência.
- Front: nenhum. `especificacao/front/` fica como está.
- Prova: `src/test/resources/features/auth.feature` (`AUTH-02`); cenário HTTP de `EMP-04`; `ControllerAvisoTest` (`AVI-02`); teste HTTP de `IA-02`. WhatsApp e agenda já apontam feature ou JUnit e só entram se a prova falhar.

## Alinhamento
- Stack de `00-architecture.md`
- DTO nomeado; sem entidade JPA e sem `Map` no payload
- YAML em `especificacao/back/` antes do Java; `$ref` novo entra em `swagger.yaml`

## Riscos
- Tratar lacuna de produto como se já estivesse no spec. Comportamento sem requisito fica para um documentar separado, depois que houver contrato e prova.
- Ajustar o YAML para o Java passar. A correção é no Java.
