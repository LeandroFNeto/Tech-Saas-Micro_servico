#!/usr/bin/env bash
set -euo pipefail

ARQUIVO="${1:-.env}"
ALTERADAS=()

if [ ! -f "$ARQUIVO" ]; then
  echo "Arquivo $ARQUIVO não encontrado."
  exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl não encontrado."
  exit 1
fi

ler() {
  grep -E "^$1=" "$ARQUIVO" | tail -n 1 | cut -d= -f2- | tr -d '\r' || true
}

definir() {
  local nome="$1"
  local valor="$2"
  local temporario
  temporario="$(mktemp)"
  if grep -qE "^$nome=" "$ARQUIVO"; then
    NOME="$nome" VALOR="$valor" awk 'BEGIN{n=ENVIRON["NOME"]; v=ENVIRON["VALOR"]} index($0, n"=")==1 {print n"="v; next} {print}' "$ARQUIVO" > "$temporario"
  else
    cat "$ARQUIVO" > "$temporario"
    [ -n "$(tail -c 1 "$temporario")" ] && echo >> "$temporario"
    printf '%s=%s\n' "$nome" "$valor" >> "$temporario"
  fi
  cat "$temporario" > "$ARQUIVO"
  rm -f "$temporario"
  ALTERADAS+=("$nome")
}

gerar() {
  local tamanho="$1"
  local valor=""
  while [ "${#valor}" -lt "$tamanho" ]; do
    valor="$valor$(openssl rand -base64 64 | tr -dc 'A-Za-z0-9')"
  done
  printf '%s' "${valor:0:$tamanho}"
}

CRIPTO_ATUAL="$(ler CONTATOS_CHAVE_CRIPTO)"
INDICE_ATUAL="$(ler CONTATOS_CHAVE_INDICE)"
CRIPTO_ANTIGA="$(ler CONTATOS_CHAVE_CRIPTO_ANTIGA)"
INDICE_ANTIGA="$(ler CONTATOS_CHAVE_INDICE_ANTIGA)"

if [ -n "$CRIPTO_ANTIGA" ] || [ -n "$INDICE_ANTIGA" ]; then
  echo "O $ARQUIVO já tem CONTATOS_CHAVE_CRIPTO_ANTIGA ou CONTATOS_CHAVE_INDICE_ANTIGA preenchida."
  echo "Suba a aplicação para concluir a rotação anterior e apague essas variáveis antes de rodar de novo."
  exit 1
fi
if [ -z "$CRIPTO_ATUAL" ] || [ -z "$INDICE_ATUAL" ]; then
  echo "O $ARQUIVO não tem as duas chaves atuais (CONTATOS_CHAVE_CRIPTO e CONTATOS_CHAVE_INDICE)."
  exit 1
fi

umask 077
COPIA="$ARQUIVO.bak-$(date +%Y%m%d%H%M%S)"
cp "$ARQUIVO" "$COPIA"
chmod 600 "$COPIA"
echo "Cópia de segurança: $COPIA"

definir CONTATOS_CHAVE_CRIPTO_ANTIGA "$CRIPTO_ATUAL"
definir CONTATOS_CHAVE_INDICE_ANTIGA "$INDICE_ATUAL"
definir CONTATOS_CHAVE_CRIPTO "$(openssl rand -base64 32 | tr -d '\r\n')"
definir CONTATOS_CHAVE_INDICE "$(gerar 48)"

chmod 600 "$ARQUIVO"

echo
echo "Variáveis atualizadas (valores não exibidos):"
printf '  - %s\n' "${ALTERADAS[@]}"
echo
echo "Próximos passos:"
echo "  1. Confirme que o dump do banco anterior à rotação está guardado fora do servidor, cifrado."
echo "  2. docker compose up -d --force-recreate"
echo "  3. Confira no log da API as quantidades de contatos e reservas rotacionados."
echo "  4. Confira a lista de contatos no painel."
echo "  5. Apague CONTATOS_CHAVE_CRIPTO_ANTIGA e CONTATOS_CHAVE_INDICE_ANTIGA do $ARQUIVO, as cópias $ARQUIVO.bak-* e o dump anterior."
