# shellcheck shell=bash
# skill-eval script 共通処理。各 script から `source` して使う (単体では実行しない)。

SE_REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck disable=SC2034 # 読み込み元の script が使う
SE_DEFAULT_HISTORY_DIR="$SE_REPO_ROOT/.github/skill-eval/history"

# die <何が> <なぜ> <どう直すか>
die() {
  {
    echo "ERROR: $1"
    [ -n "${2:-}" ] && echo "  why: $2"
    [ -n "${3:-}" ] && echo "  fix: $3"
  } >&2
  exit 1
}

warn() { echo "WARN: $*" >&2; }

require_cmd() {
  local cmd
  for cmd in "$@"; do
    command -v "$cmd" >/dev/null 2>&1 ||
      die "command '$cmd' が見つからない" \
        "skill-eval の script は $* を使う" \
        "'$cmd' をインストールする (claude は 'npm install -g @anthropic-ai/claude-code'。yq は mikefarah/yq v4)"
  done
}

# skill 名の検証と skill ディレクトリの解決
skill_dir() {
  [[ "$1" =~ ^[a-z0-9][a-z0-9-]*$ ]] || die "skill 名 '$1' が不正" "kebab-case である必要がある" "例: pr-fix-loop"
  local d="$SE_REPO_ROOT/skills/$1"
  [ -f "$d/SKILL.md" ] || die "skills/$1/SKILL.md が無い" "skill-eval は skills/<name>/SKILL.md を持つ skill だけを扱う" "skill 名を確認する"
  echo "$d"
}

# SKILL.md frontmatter の値を出す (無ければ空)
skill_meta() {
  yq --front-matter=extract "$2 // \"\"" "$1/SKILL.md"
}

# evals/<case>/prompt.md を持つテストケースの数
case_count() {
  find "$1/evals" -mindepth 2 -maxdepth 2 -name prompt.md 2>/dev/null | wc -l | tr -d ' '
}
