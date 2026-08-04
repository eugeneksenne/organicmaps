#!/usr/bin/env bash
# Starts the local Supabase PostgreSQL database and applies all tracked migrations.
# Requires Docker and the Supabase CLI; neither is installed by this repository.
set -euo pipefail

if ! command -v supabase >/dev/null 2>&1; then
  echo "Supabase CLI is required. Install it before running this script." >&2
  exit 1
fi
if ! command -v docker >/dev/null 2>&1; then
  echo "Docker is required to run the local Supabase database." >&2
  exit 1
fi

cd "$(dirname "$0")/.."
supabase start
supabase db reset
printf '\nLocal database is ready. Connection details: supabase status\n'
