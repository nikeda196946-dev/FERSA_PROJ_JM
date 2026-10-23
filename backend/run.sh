#!/usr/bin/env bash
# Carrega .env e sobe o backend. Não precisa ter o Maven instalado:
# o ./mvnw baixa o Maven sozinho na primeira vez. Precisa só do Java 21.
# Uso: ./run.sh
set -e
cd "$(dirname "$0")"

if [ -f .env ]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
else
  echo "Aviso: .env não encontrado. Copie .env.example para .env e preencha os valores."
  exit 1
fi

if ! command -v java >/dev/null 2>&1 && [ -z "$JAVA_HOME" ]; then
  echo "Erro: Java não encontrado. Instale o Java 21 (https://adoptium.net)."
  exit 1
fi

exec ./mvnw spring-boot:run
