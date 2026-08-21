# lib.sh — shared helpers for pr-fix-loop scripts. Source this; do not execute.
#
# Contract for every pr-fix-loop script:
#   - stdout: exactly one line of JSON (the machine-readable result)
#   - stderr: human-readable progress / error explanation
#   - exit non-zero on failure, with "what / why / how to fix" on stderr
#   - GH_CMD env var substitutes the `gh` binary (used by tests to inject a stub)
# Dependencies: bash, git, jq, gh (authenticated).

GH_CMD="${GH_CMD:-gh}"

# Handled-marker convention for issue-level comments (review-handling.md).
# The summary text is a project choice; keep it consistent between the
# fetch filter (fetch-pr-state.sh) and the writer (mark-comment-handled.sh).
RESOLVED_SUMMARY="${PR_FIX_LOOP_RESOLVED_SUMMARY:-Resolved}"
HANDLED_PREFIX="<details><summary>${RESOLVED_SUMMARY}"

# die <what-failed> <why> <how-to-fix>
die() {
  printf 'pr-fix-loop: ERROR: %s\n  why: %s\n  fix: %s\n' "$1" "$2" "$3" >&2
  exit 1
}

log() {
  printf '%s\n' "$*" >&2
}

require_cmd() {
  local c
  for c in "$@"; do
    command -v "$c" >/dev/null 2>&1 || die \
      "required command not found: $c" \
      "pr-fix-loop scripts depend on $c" \
      "install $c (e.g. 'brew install $c' / 'apt-get install $c') and re-run"
  done
}

# gh auth is a hard prerequisite (operations.md). Abort the pass on failure.
preflight_gh_auth() {
  "$GH_CMD" auth status >/dev/null 2>&1 || die \
    "gh auth status failed" \
    "GitHub CLI is not authenticated; every pr-fix-loop pass talks to the GitHub API" \
    "run 'gh auth login', confirm with 'gh auth status', then re-run"
}

# Dirty-worktree abort (operations.md): pr-fix-loop checks out several PR
# branches; never clobber the user's in-progress work.
require_clean_worktree() {
  git rev-parse --is-inside-work-tree >/dev/null 2>&1 || die \
    "not inside a git worktree" \
    "pr-fix-loop operates on the local clone of the repository" \
    "cd into the repository clone and re-run"
  if [ -n "$(git status --porcelain)" ]; then
    die "worktree is dirty" \
      "pr-fix-loop switches between PR branches and would clobber in-progress work" \
      "commit or stash your changes (git stash push -u -m 'pr-fix-loop temp save') and re-run"
  fi
}

# Derive owner/repo once (operations.md) into $OWNER / $REPO.
derive_owner_repo() {
  local json
  if ! json="$("$GH_CMD" repo view --json owner,name 2>/dev/null)"; then
    die "could not derive owner/repo" \
      "'gh repo view' failed — the current directory is not a clone with a GitHub remote" \
      "run from the repository clone (with an 'origin' pointing at GitHub) and re-run"
  fi
  OWNER="$(printf '%s' "$json" | jq -r '.owner.login // empty')"
  REPO="$(printf '%s' "$json" | jq -r '.name // empty')"
  if [ -z "$OWNER" ] || [ -z "$REPO" ]; then
    die "owner/repo derivation returned empty" \
      "'gh repo view --json owner,name' did not contain owner.login / name" \
      "check 'gh repo view' output and the origin remote"
  fi
}

require_pr_number() {
  case "$1" in
    ''|*[!0-9]*) die "invalid PR number: '$1'" \
      "PR arguments must be plain numbers (e.g. 179 180 181)" \
      "pass space-separated PR numbers" ;;
  esac
}
