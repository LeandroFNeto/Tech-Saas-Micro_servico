set -e

if [ -d "$HOME/gamb" ]; then
  cd "$HOME/gamb"
elif [ -d "/gamb" ]; then
  cd /gamb
elif [ -d "/root/gamb" ]; then
  cd /root/gamb
elif [ -d "/app/Tech-Saas-Micro_servico" ]; then
  cd /app/Tech-Saas-Micro_servico
else
  cd ~/Tech-Saas-Micro_servico
fi

git fetch origin main
git reset --hard origin/main

if [ -d "frontend" ]; then
  cd frontend
  npm ci
  npm run build
  cd ..
fi

docker compose down --remove-orphans || true
docker compose up -d --build

docker system prune -f

echo "Deploy finalizado com sucesso no VPS Hostinger (pasta gamb)!"
