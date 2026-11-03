# Design

## Context

O webhook localiza a empresa pela sessão do WhatsApp e entrega o texto à strategy do `ramoDeAtuacao` por `FactoryModulo` (WPP-01). `AssistenteTutoriaService` já tem o modo Suporte e o modo `TESTE_VIRTUAL`. A change `ajustes-visuais-chat-flutuante-tutoriais` torna o Suporte independente de dados de empresa e o publica por endpoint. Motivação em proposal.md.

## Goals / Non-Goals

**Goals:**
- Reaproveitar o bot de Suporte no número de WhatsApp da Gamb sem duplicar prompt nem escopo.
- Dar aos módulos novos um bot de teste em WhatsApp real, restrito a quem a Gamb autoriza.

**Non-Goals:**
- Não mudar o roteamento das empresas clientes nem as strategies existentes.
- Não criar armazenamento novo: o volume é pequeno e o registro de módulos fica no código.
- Não expor o bot de teste a clientes.

## Decisions

- **Duas empresas internas da Gamb, uma por sessão**: o roteamento atual parte da empresa. A Gamb cadastra duas empresas internas, com ramos reservados `SUPORTE_GAMB` e `TESTE_MODULOS`, e `FactoryModulo` entrega cada ramo à sua strategy. Alternativa: um roteamento paralelo ao das empresas, descartada por duplicar o webhook.
- **`SuporteGambStrategy` chama o método de Suporte do `AssistenteTutoriaService`**: o mesmo prompt e o mesmo escopo do painel. A strategy não monta o prompt e não lê dado de empresa.
- **`TesteModulosStrategy` com lista de números autorizados**: lê os números de uma configuração do admin. Número fora da lista recebe uma resposta fixa e nenhum módulo roda. Alternativa: sem lista, descartada porque o bot executa módulos ainda sem revisão.
- **Registro de módulos experimentais**: interface `ModuloExperimental` com nome, descrição e resposta, e um registro que o Spring monta com os beans. O comando `/modulos` lista e `/modulo <nome> <texto>` executa. O registro vive em memória.
- **Simulador do painel**: a lógica de `responderTesteVirtual` fica no serviço e vira o primeiro módulo experimental. O endpoint autenticado continua aceitando o modo.
- **Segredos e logs**: o número do contato e o conteúdo das mensagens passam pela mesma máscara de log do webhook (`MascaraLog`).

## Risks / Trade-offs

- [Pergunta de cliente sobre os próprios dados no número da Gamb] → o Suporte não tem dado de empresa; a resposta orienta a usar o painel.
- [Módulo experimental com efeito colateral] → o contrato do módulo exige resposta simulada e proíbe escrita em dados de empresas reais.
- [Lista de autorizados vazia] → o bot de teste responde que não há número autorizado e nada executa.
- [Dependência da outra change] → só aplicar depois que o Suporte sem dado de empresa estiver implementado.
