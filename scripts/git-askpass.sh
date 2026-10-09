#!/usr/bin/env bash
set -euo pipefail

# Do not offer the IVL credential to unrelated dependency hosts.
case "$1" in
    *Username*https://github.com*) printf '%s\n' 'x-access-token' ;;
    *Password*https://x-access-token@github.com*) printf '%s\n' "${IVL_READ_TOKEN:?IVL_READ_TOKEN is required}" ;;
    *) exit 1 ;;
esac
