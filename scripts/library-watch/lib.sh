# shellcheck shell=bash
# library-watch script 共通処理。各 script から `source` して使う (単体では実行しない)。

LW_REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
# shellcheck disable=SC2034 # 読み込み元の script が使う
LW_DEFAULT_LIBRARIES="$LW_REPO_ROOT/.github/library-watch/libraries.yml"

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
        "library-watch の script は $* を使う" \
        "'$cmd' をインストールする (GitHub の ubuntu runner には yq / jq / gh / git が入っている。yq は mikefarah/yq v4)"
  done
}

require_file() {
  [ -f "$1" ] || die "ファイル '$1' が無い" "${2:-入力として必要}" "パスを確認するか --libraries で正しい一覧を渡す"
}

# libraries.yml を JSON で出す
libraries_json() {
  require_file "$1" "ライブラリ一覧 (SSoT)"
  yq -o=json '.' "$1"
}

# owner/name 形式の検証
require_repo_slug() {
  [[ "$1" =~ ^[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+$ ]] ||
    die "repo '$1' の形式が不正" "owner/name 形式である必要がある" "例: TBSten/cream"
}
