# Graphify Knowledge Graph

Este projeto possui um grafo de conhecimento Graphify em `graphify-out/`.

## Pesquisa no código

Antes de explorar o código com busca de texto cega, quando `graphify-out/graph.json` existir:
- `graphify query "<pergunta>"`: subgrafo focado para qualquer pergunta de código ou arquitetura.
- `graphify path "<A>" "<B>"`: caminho de dependência entre dois símbolos.
- `graphify explain "<conceito>"`: nós relacionados a um conceito.

Leia ou edite diretamente apenas após ter o contexto ou indicação do grafo.
Se `graphify-out/graph.json` não existir, use as ferramentas normais de leitura/busca.
