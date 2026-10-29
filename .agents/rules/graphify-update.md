# Graphify update

Depois de criar ou alterar qualquer arquivo do projeto, quando `graphify-out/graph.json` existir, execute uma vez no fim do turno:

```bash
graphify update .
```

Vale para código, teste, spec, YAML, JSON e regras. Vários arquivos no mesmo turno pedem um único update. O comando é de análise AST local e não consome API.
Não execute caso `graphify-out/graph.json` ainda não exista.
