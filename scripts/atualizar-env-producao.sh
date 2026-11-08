#!/usr/bin/env bash
set -euo pipefail

ARQUIVO="${1:-.env}"
CONTAINER_BANCO="${CONTAINER_BANCO:-postgres_recanto}"
REDE_DOCKER="${REDE_DOCKER:-bot-network}"
ALTERADAS=()
PENDENTES=()

if [ ! -f "$ARQUIVO" ]; then
  echo "Arquivo $ARQUIVO não encontrado."
  exit 1
fi
if ! command -v openssl >/dev/null 2>&1; then
  echo "openssl não encontrado."
  exit 1
fi

COPIA="$ARQUIVO.bak-$(date +%Y%m%d%H%M%S)"
cp "$ARQUIVO" "$COPIA"
chmod 600 "$COPIA"
echo "Cópia de segurança: $COPIA"

gerar() {
  local tamanho="${1:-48}"
  local valor=""
  while [ "${#valor}" -lt "$tamanho" ]; do
    valor="$valor$(openssl rand -base64 64 | tr -dc 'A-Za-z0-9')"
  done
  printf '%s' "${valor:0:$tamanho}"
}

ler() {
  grep -E "^$1=" "$ARQUIVO" | tail -n 1 | cut -d= -f2- || true
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

perguntar() {
  local resposta
  read -r -p "$1 [s/N] " resposta
  [ "$resposta" = "s" ] || [ "$resposta" = "S" ]
}

definir JWT_SECRET_KEY "$(gerar 64)"

SEGREDO_GATEWAY="$(gerar 48)"
definir WPP_SECRET_KEY "$SEGREDO_GATEWAY"
definir WPPCONNECT_SECRET_KEY "$SEGREDO_GATEWAY"
definir WPPCONNECT_WEBHOOK_SECRET "$(gerar 48)"
definir ADMIN_API_KEY "$(gerar 48)"
definir API_AUTH_TOKEN "$(gerar 48)"

SENHA_ADMIN_NOVA=""
if perguntar "O usuário admin já existe no banco (você já entra no painel com ele)?"; then
  definir ADMIN_DEFAULT_PASSWORD ""
else
  SENHA_ADMIN_NOVA="$(gerar 20)"
  definir ADMIN_DEFAULT_PASSWORD "$SENHA_ADMIN_NOVA"
fi

if [ -z "$(ler GAMB_CONTATO_WHATSAPP)" ]; then
  read -r -p "Número comercial da Gamb no WhatsApp, só dígitos com 55 (Enter para deixar sem): " NUMERO_GAMB
  if [ -n "$NUMERO_GAMB" ]; then
    definir GAMB_CONTATO_WHATSAPP "$NUMERO_GAMB"
  else
    PENDENTES+=("GAMB_CONTATO_WHATSAPP vazio: a resposta comercial sai sem o link do WhatsApp")
  fi
fi

FAIXA_REDE=""
if command -v docker >/dev/null 2>&1; then
  ID_REDE="$(docker network ls -q -f "name=$REDE_DOCKER" | head -n 1)"
  if [ -n "$ID_REDE" ]; then
    FAIXA_REDE="$(docker network inspect "$ID_REDE" --format '{{(index .IPAM.Config 0).Subnet}}' 2>/dev/null || true)"
  fi
fi
echo "Faixa da rede Docker $REDE_DOCKER: ${FAIXA_REDE:-não encontrada}"
read -r -p "IP exato do proxy na frente da API (Enter para usar a faixa acima): " PROXY
PROXY="${PROXY:-$FAIXA_REDE}"
if [ -n "$PROXY" ]; then
  definir PROXIES_CONFIAVEIS "$PROXY"
else
  PENDENTES+=("PROXIES_CONFIAVEIS vazio: o limite de login vale pelo IP do proxy")
fi

read -r -s -p "Senha antiga dos clientes legados, para bloquear no próximo boot (Enter para pular): " SENHA_LEGADA
echo
if [ -n "$SENHA_LEGADA" ]; then
  definir SENHA_LEGADA_A_BLOQUEAR "$SENHA_LEGADA"
  PENDENTES+=("Depois do primeiro boot, conferir no log as contas bloqueadas e apagar SENHA_LEGADA_A_BLOQUEAR do $ARQUIVO")
fi

read -r -s -p "Chave nova do Gemini, gerada no Google AI Studio (Enter para manter): " CHAVE_GEMINI
echo
if [ -n "$CHAVE_GEMINI" ]; then
  definir GEMINI_API_KEY "$CHAVE_GEMINI"
else
  PENDENTES+=("Revogar a chave do Gemini no Google AI Studio e trocar GEMINI_API_KEY")
fi

read -r -s -p "Chave nova da OpenWeather (Enter para manter): " CHAVE_TEMPO
echo
if [ -n "$CHAVE_TEMPO" ]; then
  definir OPENWEATHER_API_KEY "$CHAVE_TEMPO"
else
  PENDENTES+=("Gerar outra chave na OpenWeather e trocar OPENWEATHER_API_KEY")
fi

if perguntar "Trocar agora a senha do Postgres (roda ALTER USER no container $CONTAINER_BANCO)?"; then
  USUARIO_BANCO="$(ler MEU_POSTGRES_USER)"
  BANCO="$(ler MEU_POSTGRES_DB)"
  SENHA_BANCO="$(gerar 32)"
  docker exec -i "$CONTAINER_BANCO" psql -v ON_ERROR_STOP=1 -U "$USUARIO_BANCO" -d "$BANCO" -c "ALTER USER \"$USUARIO_BANCO\" WITH PASSWORD '$SENHA_BANCO';" >/dev/null
  definir MEU_POSTGRES_PASSWORD "$SENHA_BANCO"
else
  PENDENTES+=("Trocar a senha do Postgres com ALTER USER e MEU_POSTGRES_PASSWORD juntos")
fi

PENDENTES+=("CONTATOS_CHAVE_CRIPTO e CONTATOS_CHAVE_INDICE mantidas: trocar exige decifrar e cifrar de novo os contatos gravados")

chmod 600 "$ARQUIVO"

echo
echo "Variáveis atualizadas (valores não exibidos):"
printf '  - %s\n' "${ALTERADAS[@]}"
echo
echo "Pendências:"
printf '  - %s\n' "${PENDENTES[@]}"
if [ -n "$SENHA_ADMIN_NOVA" ]; then
  echo
  echo "Senha inicial do admin (anote agora; ela não fica em outro lugar além do $ARQUIVO): $SENHA_ADMIN_NOVA"
fi
echo
echo "Para aplicar: docker compose up -d --force-recreate"
echo "A troca do JWT encerra as sessões abertas do painel."
echo "Quando confirmar que tudo subiu, apague a cópia: rm $COPIA"
