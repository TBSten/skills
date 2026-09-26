#!/usr/bin/env bash
# Collect the raw material for a release note into .local/v<version>-release-note/.
#
#   commits.txt  `git log` of <prev-tag>..<ref> (merge commits included)
#   prs.json     merged PRs since <prev-tag> (title / body / url / labels), if `gh` is available
#   range.txt    the range, commit count and compare URL
#
# Usage:
#   collect-changes.sh <new-version> [--prev <tag>] [--ref <ref>]
#
#   --prev  Previous release tag. Default: the latest `v*` tag reachable from <ref>.
#   --ref   Where the release is cut from. Default: origin/main.
#
# Run from the repository root. Safe to re-run: the output files are overwritten.
set -euo pipefail

die() {
    echo "ERROR: $*" >&2
    exit 1
}

version=""
prev=""
ref="origin/main"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --prev) prev="${2:?--prev needs a tag}"; shift 2 ;;
        --ref) ref="${2:?--ref needs a ref}"; shift 2 ;;
        -h|--help) sed -n '2,15p' "$0"; exit 0 ;;
        -*) die "unknown option '$1' (see --help)" ;;
        *) [[ -z "$version" ]] || die "more than one version given"; version="$1"; shift ;;
    esac
done
[[ -n "$version" ]] || die "no version given. Usage: collect-changes.sh <new-version> [--prev <tag>]"
version="${version#v}"

git fetch --quiet origin --tags || echo "WARN git fetch failed; using local refs" >&2
git rev-parse --verify --quiet "$ref" > /dev/null || die "ref '$ref' not found. Pass --ref <branch>."

if [[ -z "$prev" ]]; then
    prev="$(git describe --tags --abbrev=0 --match 'v*' "$ref" 2>/dev/null || true)"
fi

out=".local/v${version}-release-note"
mkdir -p "$out"

if [[ -n "$prev" ]]; then
    range="${prev}..${ref}"
    since="$(git log -1 --format=%cI "$prev")"
else
    echo "WARN no previous v* tag found; collecting the whole history (first release?)" >&2
    range="$ref"
    since=""
fi

git log --pretty='%h %ad %s' --date=short "$range" > "$out/commits.txt"
commit_count="$(wc -l < "$out/commits.txt" | tr -d ' ')"

remote_url="$(git remote get-url origin 2>/dev/null || true)"
repo_slug="$(echo "$remote_url" | sed -E 's#^(git@github\.com:|https://github\.com/)##; s#\.git$##')"

{
    echo "version: v${version}"
    echo "previous: ${prev:-<none>}"
    echo "range: ${range}"
    echo "commits: ${commit_count}"
    if [[ -n "$prev" && -n "$repo_slug" ]]; then
        echo "compare: https://github.com/${repo_slug}/compare/${prev}...v${version}"
    fi
} > "$out/range.txt"

if command -v gh > /dev/null 2>&1; then
    search="is:merged"
    [[ -n "$since" ]] && search="is:merged merged:>=${since%%T*}"
    if gh pr list --state merged --limit 200 --search "$search" \
        --json number,title,body,url,labels,mergedAt,author > "$out/prs.json"; then
        pr_count="$(python3 -c 'import json,sys; print(len(json.load(open(sys.argv[1]))))' "$out/prs.json")"
        echo "OK prs.json: ${pr_count} merged PR(s) (by merge date; double-check against commits.txt)"
    else
        rm -f "$out/prs.json"
        echo "WARN gh pr list failed (not authenticated / not a GitHub repo?); prs.json was not created." >&2
    fi
else
    echo "WARN gh not found; prs.json was not created. Read PRs from commits.txt instead." >&2
fi

echo "OK commits.txt: ${commit_count} commit(s) in ${range}"
echo "OK output: ${out}/"
