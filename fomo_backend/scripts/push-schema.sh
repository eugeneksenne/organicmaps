#!/usr/bin/env bash
# Applies only the committed Supabase migrations to a linked remote project.
# Review the migration plan and run this manually in CI/CD for production.
set -euo pipefail

if ! command -v supabase >/dev/null 2>&1; then
  echo "Supabase CLI is required. Install it before running this script." >&2
  exit 1
fi
: "${SUPABASE_PROJECT_REF:?Set SUPABASE_PROJECT_REF in your uncommitted .env}"

cd "$(dirname "$0")/.."
supabase link --project-ref "$SUPABASE_PROJECT_REF"
supabase db push
