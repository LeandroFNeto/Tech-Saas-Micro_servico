# Graph Report - corrigir-reserva-automatica  (2026-10-27)

## Corpus Check
- 3 files · ~1,620 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 19 nodes · 16 edges · 4 communities
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `45d5e77f`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- Design
- corrigir-reserva-automatica
- O que muda
- Tarefas

## God Nodes (most connected - your core abstractions)
1. `Design` - 5 edges
2. `corrigir-reserva-automatica` - 5 edges
3. `O que muda` - 4 edges
4. `Tarefas` - 3 edges
5. `Decisão` - 1 edges
6. `Arquivos` - 1 edges
7. `Alinhamento` - 1 edges
8. `Riscos` - 1 edges
9. `Problema` - 1 edges
10. `Entra` - 1 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities (4 total, 0 thin omitted)

### Community 0 - "Design"
Cohesion: 0.33
Nodes (5): Alinhamento, Arquivos, Decisão, Design, Riscos

### Community 1 - "corrigir-reserva-automatica"
Cohesion: 0.40
Nodes (4): corrigir-reserva-automatica, Domínios, Fora de escopo, Problema

### Community 2 - "O que muda"
Cohesion: 0.50
Nodes (4): Altera, Entra, O que muda, Sai

### Community 3 - "Tarefas"
Cohesion: 0.50
Nodes (3): Fechamento (não é da programação), Programação, Tarefas

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `corrigir-reserva-automatica` connect `corrigir-reserva-automatica` to `O que muda`?**
  _High betweenness centrality (0.144) - this node is a cross-community bridge._
- **Why does `O que muda` connect `O que muda` to `corrigir-reserva-automatica`?**
  _High betweenness centrality (0.118) - this node is a cross-community bridge._