#!/usr/bin/env bash
# check-upstream.sh — fetch the cat4 upstream release sources and diff them
# against the project's pinned versions (references/categories.md cat4).
#
# Usage:
#   check-upstream.sh [--project-root <dir>] [--toml <libs.versions.toml>]
#
# Sources (faithful to references/categories.md):
#   kotlin                — GitHub Releases API (JetBrains/kotlin)
#   compose-multiplatform — GitHub Releases API (JetBrains/compose-multiplatform)
#   gradle                — https://services.gradle.org/versions/current
#   agp                   — Google Maven (com.android.tools.build:gradle metadata)
#   androidx-core         — Google Maven (androidx.core:core metadata; the
#                           machine-readable proxy for the AndroidX versions page)
#
# Prints a JSON array on stdout: [{"tool","latest","project","drift"}] where
# drift ∈ none|patch|minor|major|ahead|unknown|error. Rate-limit / fetch
# failures set drift="error" (+ an "error" message field) and the scan
# continues — a partial result is still useful.
#
# Env:
#   CURL_CMD     — override the fetch command (default: curl -fsSL --max-time 20).
#                  Receives the URL as its last argument (mock-friendly).
#   GITHUB_TOKEN — optional; adds an Authorization header for api.github.com.
#
# Dependencies: bash (3.2+), curl (or CURL_CMD), jq. macOS / Linux compatible.
set -euo pipefail

die() {
  printf 'ERROR: %s\n' "$1" >&2
  if [ -n "${2:-}" ]; then printf '  why: %s\n' "$2" >&2; fi
  if [ -n "${3:-}" ]; then printf '  fix: %s\n' "$3" >&2; fi
  exit 1
}

usage() { sed -n '2,26p' "$0" | sed 's/^# \{0,1\}//'; }

ROOT="." TOML=""
while [ $# -gt 0 ]; do
  case "$1" in
    --project-root)
      [ $# -ge 2 ] || die "--project-root requires a value" "" "pass --project-root <dir>"
      ROOT=$2; shift 2 ;;
    --toml)
      [ $# -ge 2 ] || die "--toml requires a value" "" "pass --toml <path>"
      TOML=$2; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "unknown argument: $1" "" "run with --help" ;;
  esac
done

command -v jq >/dev/null 2>&1 || die "jq not found" \
  "GitHub / Gradle API responses are JSON" "install jq (brew install jq / apt-get install jq)"
if [ -z "${CURL_CMD:-}" ]; then
  command -v curl >/dev/null 2>&1 || die "curl not found" \
    "upstream sources are fetched over HTTPS" "install curl or set CURL_CMD"
fi

[ -n "$TOML" ] || TOML="$ROOT/gradle/libs.versions.toml"
WRAPPER="$ROOT/gradle/wrapper/gradle-wrapper.properties"
if [ ! -f "$TOML" ]; then
  printf 'WARN: version catalog not found: %s (project versions will be "unknown")\n' "$TOML" >&2
fi

fetch() { # args... url  → body on stdout, non-zero on failure
  ${CURL_CMD:-curl -fsSL --max-time 20} "$@"
}
gh_fetch() { # url
  if [ -n "${GITHUB_TOKEN:-}" ]; then
    fetch -H "Authorization: Bearer $GITHUB_TOKEN" "$1"
  else
    fetch "$1"
  fi
}

toml_version() { # key → value or empty
  if [ ! -f "$TOML" ]; then return 0; fi
  awk -v key="$1" '
    /^\[/ { in_v = ($0 ~ /^\[versions\]/); next }
    in_v {
      line = $0
      sub(/#.*/, "", line)
      if (match(line, "^[ \t]*" key "[ \t]*=")) {
        if (match(line, /"[^"]*"/)) { print substr(line, RSTART + 1, RLENGTH - 2); exit }
      }
    }
  ' "$TOML"
}

first_toml_version() { # key... → first hit
  k=""
  for k in "$@"; do
    v=$(toml_version "$k")
    if [ -n "$v" ]; then printf '%s' "$v"; return 0; fi
  done
  return 0
}

wrapper_gradle_version() {
  if [ ! -f "$WRAPPER" ]; then return 0; fi
  sed -n 's/^distributionUrl=.*gradle-\([0-9][0-9.]*\)-.*/\1/p' "$WRAPPER"
}

maven_metadata_latest() { # body on stdin unfriendly in bash3; pass as $1
  b=$1
  v=$(printf '%s\n' "$b" | sed -n 's/.*<release>\([^<]*\)<\/release>.*/\1/p' | sed -n 1p)
  if [ -z "$v" ]; then
    v=$(printf '%s\n' "$b" | sed -n 's/.*<latest>\([^<]*\)<\/latest>.*/\1/p' | sed -n 1p)
  fi
  if [ -z "$v" ]; then
    v=$(printf '%s\n' "$b" | sed -n 's/.*<version>\([^<]*\)<\/version>.*/\1/p' | tail -n 1)
  fi
  printf '%s' "$v"
}

ver_part() { # version index(1..3) → numeric component (0 if absent)
  core=$(printf '%s' "$1" | sed 's/[^0-9.].*//')
  part=$(printf '%s' "$core" | cut -d. -f"$2")
  case "$part" in
    ''|*[!0-9]*) printf '0' ;;
    *) printf '%s' "$part" ;;
  esac
}

drift_of() { # latest project → drift keyword
  latest=$1 project=$2
  if [ -z "$latest" ]; then printf 'error'; return 0; fi
  if [ -z "$project" ]; then printf 'unknown'; return 0; fi
  if [ "$latest" = "$project" ]; then printf 'none'; return 0; fi
  l1=$(ver_part "$latest" 1) l2=$(ver_part "$latest" 2) l3=$(ver_part "$latest" 3)
  r1=$(ver_part "$project" 1) r2=$(ver_part "$project" 2) r3=$(ver_part "$project" 3)
  if   [ "$l1" -gt "$r1" ]; then printf 'major'
  elif [ "$l1" -lt "$r1" ]; then printf 'ahead'
  elif [ "$l2" -gt "$r2" ]; then printf 'minor'
  elif [ "$l2" -lt "$r2" ]; then printf 'ahead'
  elif [ "$l3" -gt "$r3" ]; then printf 'patch'
  elif [ "$l3" -lt "$r3" ]; then printf 'ahead'
  else
    # numerically equal but strings differ → project is on a pre-release of latest
    printf 'patch'
  fi
}

ENTRIES=""
emit() { # tool latest project drift [error]
  ENTRIES="$ENTRIES
$(jq -cn --arg tool "$1" --arg latest "$2" --arg project "$3" --arg drift "$4" --arg err "${5:-}" \
    '{tool: $tool,
      latest: (if $latest == "" then null else $latest end),
      project: (if $project == "" then null else $project end),
      drift: $drift}
     + (if $err == "" then {} else {error: $err} end)')"
}

check_github() { # tool repo project-version
  tool=$1 repo=$2 project=$3
  if body=$(gh_fetch "https://api.github.com/repos/$repo/releases/latest" 2>/dev/null); then
    tag=$(printf '%s' "$body" | jq -r '.tag_name // empty' 2>/dev/null || true)
    latest=${tag#v}
    if [ -n "$latest" ]; then
      emit "$tool" "$latest" "$project" "$(drift_of "$latest" "$project")"
    else
      emit "$tool" "" "$project" "error" "unparseable response from GitHub Releases API ($repo) — possibly rate-limited; set GITHUB_TOKEN"
    fi
  else
    emit "$tool" "" "$project" "error" "fetch failed: https://api.github.com/repos/$repo/releases/latest (network / rate limit)"
  fi
}

check_google_maven() { # tool metadata-url project-version
  tool=$1 url=$2 project=$3
  if body=$(fetch "$url" 2>/dev/null); then
    latest=$(maven_metadata_latest "$body")
    if [ -n "$latest" ]; then
      emit "$tool" "$latest" "$project" "$(drift_of "$latest" "$project")"
    else
      emit "$tool" "" "$project" "error" "unparseable maven-metadata.xml: $url"
    fi
  else
    emit "$tool" "" "$project" "error" "fetch failed: $url"
  fi
}

# 1. Kotlin — GitHub Releases
check_github kotlin JetBrains/kotlin "$(first_toml_version kotlin)"

# 2. Compose Multiplatform — GitHub Releases
check_github compose-multiplatform JetBrains/compose-multiplatform \
  "$(first_toml_version compose-multiplatform composeMultiplatform compose)"

# 3. Gradle — services.gradle.org
proj_gradle=$(wrapper_gradle_version)
if [ -z "$proj_gradle" ]; then proj_gradle=$(first_toml_version gradle); fi
if body=$(fetch "https://services.gradle.org/versions/current" 2>/dev/null); then
  latest=$(printf '%s' "$body" | jq -r '.version // empty' 2>/dev/null || true)
  if [ -n "$latest" ]; then
    emit gradle "$latest" "$proj_gradle" "$(drift_of "$latest" "$proj_gradle")"
  else
    emit gradle "" "$proj_gradle" "error" "unparseable response from services.gradle.org/versions/current"
  fi
else
  emit gradle "" "$proj_gradle" "error" "fetch failed: https://services.gradle.org/versions/current"
fi

# 4. AGP — Google Maven
check_google_maven agp \
  "https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml" \
  "$(first_toml_version agp android-gradle-plugin androidGradlePlugin)"

# 5. AndroidX (representative: androidx.core:core) — Google Maven
check_google_maven androidx-core \
  "https://dl.google.com/dl/android/maven2/androidx/core/core/maven-metadata.xml" \
  "$(first_toml_version androidx-core androidxCore androidx-core-ktx core-ktx coreKtx)"

printf '%s\n' "$ENTRIES" | jq -s .
