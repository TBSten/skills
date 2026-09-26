#!/usr/bin/env bash
# Bump the example-lib version.
#
# The SSoT is `example-lib = "X.Y.Z"` in gradle/libs.versions.toml. This script
#   1. rewrites the SSoT,
#   2. rewrites every Maven coordinate `<group-id>:example-lib*:<any version>` in the
#      allowlisted files (so stale versions left by earlier bumps are fixed too),
#   3. greps the whole repository for the old version and prints the hits it did NOT
#      touch as `REVIEW` lines, for a human / AI to judge (defensive sweep).
#
# It never commits. Review `git diff` and commit yourself.
#
# Usage:
#   bump-version.sh <new-version> [--dry-run] [--allow-downgrade]
#
# Run from the repository root.
set -euo pipefail

CATALOG="gradle/libs.versions.toml"
CATALOG_KEY="example-lib"
GROUP_ID="<group-id>"
ARTIFACT_PREFIX="example-lib"

# Files whose Maven coordinates are rewritten. Add a file here when it starts to
# contain a literal version of this library (e.g. a sample project's build script).
allowlist() {
    local f
    for f in README.md README.ja.md CLAUDE.md; do
        [[ -f "$f" ]] && echo "$f"
    done
    if [[ -d docs/src/content/docs ]]; then
        find docs/src/content/docs -type f \( -name '*.md' -o -name '*.mdx' \) | sort
    fi
}

die() {
    echo "ERROR: $*" >&2
    exit 1
}

new_version=""
dry_run=false
allow_downgrade=false
for arg in "$@"; do
    case "$arg" in
        --dry-run) dry_run=true ;;
        --allow-downgrade) allow_downgrade=true ;;
        -h|--help) sed -n '2,17p' "$0"; exit 0 ;;
        -*) die "unknown option: $arg (see --help)" ;;
        *)
            [[ -z "$new_version" ]] || die "more than one version given: '$new_version' and '$arg'"
            new_version="$arg"
            ;;
    esac
done

[[ -n "$new_version" ]] || die "no version given. Usage: bump-version.sh <new-version> [--dry-run]"
[[ "$new_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+(-[0-9A-Za-z.]+)?$ ]] \
    || die "'$new_version' is not a version like 1.2.3 or 1.2.3-alpha01. Pass the exact version to release."
[[ -f "$CATALOG" ]] || die "$CATALOG not found. Run this script from the repository root."

current_line="$(grep -E "^${CATALOG_KEY}[[:space:]]*=" "$CATALOG" || true)"
[[ -n "$current_line" ]] \
    || die "no '${CATALOG_KEY} = \"...\"' entry in $CATALOG. The version SSoT must live there under [versions]."
current_version="$(echo "$current_line" | sed -E 's/^[^"]*"([^"]*)".*$/\1/')"

echo "current: $current_version"
echo "target:  $new_version"

if [[ "$current_version" == "$new_version" ]]; then
    echo "OK already at $new_version (nothing to do)"
    exit 0
fi

# Compare the numeric cores only: `sort -V` orders 1.0.0 before 1.0.0-alpha01, which is
# the opposite of SemVer, so pre-release suffixes are handled separately.
current_core="${current_version%%-*}"
new_core="${new_version%%-*}"
is_downgrade=false
if [[ "$current_core" == "$new_core" ]]; then
    # Same core: only "release -> pre-release of the same version" goes backwards.
    [[ "$current_version" != *-* && "$new_version" == *-* ]] && is_downgrade=true
else
    lowest_core="$(printf '%s\n%s\n' "$current_core" "$new_core" | sort -V | head -1)"
    [[ "$lowest_core" == "$new_core" ]] && is_downgrade=true
fi
if [[ "$is_downgrade" == true && "$allow_downgrade" != true ]]; then
    die "target $new_version is lower than current $current_version. Released versions on Maven Central are immutable; pass --allow-downgrade only if you really mean it."
fi

export GROUP_ID ARTIFACT_PREFIX NEW_VERSION="$new_version"
# Matches `<group-id>:example-lib-core:1.2.3` etc. with any (possibly stale) version.
# Single quotes on purpose: `$ENV{...}` is expanded by perl, not by the shell.
# shellcheck disable=SC2016
coordinate_regex='(\Q$ENV{GROUP_ID}\E:\Q$ENV{ARTIFACT_PREFIX}\E[-A-Za-z0-9]*:)[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.]+)?'

echo
echo "== planned changes =="
printf '  %-60s %s\n' "$CATALOG" "$current_version -> $new_version"
touched=("$CATALOG")
while IFS= read -r f; do
    count="$(perl -ne "\$c += () = /$coordinate_regex/g; END { print \$c + 0 }" "$f")"
    if [[ "$count" -gt 0 ]]; then
        printf '  %-60s %s occurrence(s)\n' "$f" "$count"
        touched+=("$f")
    fi
done < <(allowlist)

if [[ "$dry_run" == true ]]; then
    echo
    echo "OK dry-run (no files were changed)"
    exit 0
fi

# Rewrite only the SSoT line, never other keys that happen to share the value.
KEY="$CATALOG_KEY" perl -pi -e 's/^(\Q$ENV{KEY}\E\s*=\s*")[^"]*(")/${1}$ENV{NEW_VERSION}${2}/' "$CATALOG"
for f in "${touched[@]:1}"; do
    perl -pi -e "s/$coordinate_regex/\${1}\$ENV{NEW_VERSION}/g" "$f"
done

echo
echo "== defensive sweep: remaining '$current_version' outside the allowlist =="
# Lockfiles, API dumps and generated output legitimately contain unrelated versions.
review_count=0
while IFS= read -r hit; do
    echo "REVIEW $hit"
    review_count=$((review_count + 1))
done < <(git grep --untracked -n -F "$current_version" -- . \
    ':!*.lock' ':!*pnpm-lock.yaml' ':!*package-lock.json' ':!**/api/*.api' ':!.local/**' \
    ':!docs/public/api-docs/**' ':!**/CHANGELOG*' 2>/dev/null || true)

echo
if [[ "$review_count" -gt 0 ]]; then
    echo "OK bumped to $new_version. $review_count REVIEW line(s) above: decide for each whether it is this library's version (then fix it and add the file to allowlist()) or something unrelated."
else
    echo "OK bumped to $new_version. No stray '$current_version' left."
fi
