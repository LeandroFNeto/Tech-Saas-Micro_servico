# Relatório de Segurança - lembrar-confirmacao-reserva

## Auditoria de Segurança e Proteção de Dados

1. **Criptografia em Repouso**:
   - Telefones de clientes e nomes armazenados com criptografia simétrica AES-256-GCM através do conversor `CriptografiaCampo`.
   - Índices determinísticos de busca baseados em HMAC-SHA256 (`calcularIndice`), impedindo armazenamento de números em texto claro ou hashes reversíveis.

2. **Isolamento e Controle de Acesso**:
   - Rotas em `ControllerClienteAutomacao` protegidas por `ServicoAcessoEmpresa.validarAcessoExclusivoCliente`: Administradores e clientes de outras empresas recebem HTTP 403 Forbidden. Apenas o cliente autenticado na própria sessão tem acesso aos dados.

3. **Rate Limiting e Prevenção de Raspagem**:
   - Endpoint de revelação de telefone completo (`/contatos/{id}/numero`) protegido por limite estrito de 10 consultas por hora por empresa, respondendo HTTP 429 Too Many Requests quando excedido.

4. **Sanitização de Logs e Ausência de Vazamentos**:
   - Nenhum número de telefone em texto claro, credencial ou token do WhatsApp é exposto nos logs. Todos os arquivos respeitam `MascaraLog`.

## Parecer
- **Aprovado** para avanço à etapa de Revisão.
