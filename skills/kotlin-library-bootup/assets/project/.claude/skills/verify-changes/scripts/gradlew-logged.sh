#!/usr/bin/env bash
# Run a Gradle command with its full output saved under .local/tmp/, so the log can be
# read afterwards instead of being cut with grep/tail.
#
# Usage:
#   gradlew-logged.sh [--name <label>] [--project <dir>] -- <gradle args...>
#
#   --name     Label used in the log file name and the project cache dir. Give each
#              agent running in parallel its own label so their caches do not collide.
#              Default: "main".
#   --project  Run another Gradle build in this repository (e.g. integrationTest)
#              with the root wrapper (`./gradlew -p <dir>`).
#
# Prints `LOG <path>` and `EXIT <code>` as the last two lines and exits with Gradle's code.
# Run from the repository root.
set -euo pipefail

name="main"
project=""
while [[ $# -gt 0 ]]; do
    case "$1" in
        --name) name="${2:?--name needs a value}"; shift 2 ;;
        --project) project="${2:?--project needs a directory}"; shift 2 ;;
        --) shift; break ;;
        -h|--help) sed -n '2,15p' "$0"; exit 0 ;;
        *) echo "ERROR: unknown argument '$1'. Put Gradle arguments after '--' (see --help)." >&2; exit 2 ;;
    esac
done

[[ $# -gt 0 ]] || { echo "ERROR: no Gradle arguments given. Example: gradlew-logged.sh -- jvmTest" >&2; exit 2; }
[[ -x ./gradlew ]] || { echo "ERROR: ./gradlew not found. Run this from the repository root." >&2; exit 2; }
[[ "$name" =~ ^[A-Za-z0-9._-]+$ ]] || { echo "ERROR: --name must be [A-Za-z0-9._-]+ (got '$name')." >&2; exit 2; }
if [[ -n "$project" && ! -d "$project" ]]; then
    echo "ERROR: --project '$project' is not a directory." >&2
    exit 2
fi

mkdir -p .local/tmp/gradle-cache
label="$(printf '%s' "$*" | tr -c 'A-Za-z0-9._-' '_' | cut -c1-60)"
log=".local/tmp/$(date '+%m%d-%H%M%S')-${name}-${label}.log"

cmd=(./gradlew --console=plain --project-cache-dir ".local/tmp/gradle-cache/${name}")
[[ -n "$project" ]] && cmd+=(-p "$project")
cmd+=("$@")

echo "RUN ${cmd[*]}"
set +e
"${cmd[@]}" > "$log" 2>&1
code=$?
set -e

# Enough context to decide whether to open the log at all.
if [[ $code -ne 0 ]]; then
    grep -nE '^(FAILURE|\* What went wrong|e: |> Task .* FAILED)' "$log" | head -20 || true
fi
echo "LOG $log"
echo "EXIT $code"
exit "$code"
