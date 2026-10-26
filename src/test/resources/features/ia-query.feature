# language: pt
# Origem: especificacao/back/paths/ia-query.yaml
Funcionalidade: Consulta em grafo para a IA

  Cenário: POST /api/ia-query sem credencial retorna 401
    Quando eu envio POST /api/ia-query sem credencial
    Então o status HTTP deve ser 401

  Cenário: POST /api/ia-query com JSON malformado retorna 400
    Quando eu envio POST /api/ia-query com JSON malformado e o token administrativo
    Então o status HTTP deve ser 400
