#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
cd "$(dirname "$0")/.."
if ! command -v node >/dev/null 2>&1; then
  echo "Node.js não encontrado. Instale com: pkg install nodejs-lts"
  exit 1
fi
node -e 'const [major,minor]=process.versions.node.split(".").map(Number);if(major<20||(major===20&&minor<6)){console.error("Jarvis EDU exige Node.js 20.6 ou superior.");process.exit(1)}'
if [ ! -f .env ]; then cp .env.example .env; echo "Arquivo .env criado. Opcional: edite AI_API_KEY com uma chave sua."; fi
exec node --env-file=.env server.mjs
