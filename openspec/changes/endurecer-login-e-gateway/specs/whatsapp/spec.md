# Spec Delta

## ADDED Requirements

### Requirement: WPP-22 Erro do gateway sem o objeto bruto
O gateway WPPConnect em uso SHALL registrar erros somente por tipo, código e status HTTP, com caracteres e tamanho limitados, e o `deviceController` SHALL responder sem o objeto do erro.

Prova: testes em `wppconnect-server/src/tests`

#### Scenario: Erro com texto arbitrário no nome ou no código
- **WHEN** um erro traz em `name` ou `code` um texto longo, com caracteres de controle ou com número de telefone
- **THEN** o log SHALL trazer `erro` no lugar do nome e SHALL omitir o código inválido

#### Scenario: Falha em rota de dispositivo
- **WHEN** uma rota do `deviceController` falha
- **THEN** o log SHALL trazer só a descrição segura e a resposta SHALL não trazer os campos `error` nem `log`
