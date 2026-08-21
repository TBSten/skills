#!/usr/bin/env bash
# setup-secrets.sh — Maven Central 公開に必要な GPG 鍵と GitHub Secrets を対話セットアップする。
#
# TBSten/skills skills/kotlin-maven-central-publish に同梱。
# やること:
#   1. GPG 鍵の生成 (または --key-id で既存鍵を利用)
#   2. フィンガープリント確認と SIGNING_KEY_ID (末尾 8 桁) の抽出
#   3. 公開鍵のキーサーバー送信 (確認プロンプトあり)
#   4. 秘密鍵の ASCII armor export
#   5. Sonatype Central Portal User Token の入力案内 (唯一のユーザー手作業)
#   6. gh secret set で 5 つの Secrets を登録 (確認プロンプトあり)
#
# 副作用 (キーサーバー送信・Secrets 登録) の前には必ず確認を出す。
# --dry-run で副作用ゼロの実行計画確認ができる。
# stdout の末尾 1 行に結果 JSON を出力する。ログ・エラーは stderr。Secrets の値は JSON に含めない。
# 手動でやる場合のフォールバック手順: references/gpg-setup.md / references/github-secrets.md
set -euo pipefail

log() { printf '[setup-secrets] %s\n' "$*" >&2; }
die() {
  # die <何が> [<なぜ>] [<どう直すか>]
  printf '[setup-secrets] ERROR: %s\n' "$1" >&2
  if [ $# -ge 2 ]; then printf '[setup-secrets]   原因: %s\n' "$2" >&2; fi
  if [ $# -ge 3 ]; then printf '[setup-secrets]   対処: %s\n' "$3" >&2; fi
  exit 1
}

usage() {
  cat >&2 <<'USAGE'
Usage: setup-secrets.sh [options]

Options:
  --repo <owner/repo>   Secrets を登録する GitHub リポジトリ (省略時: gh repo view で現在のリポジトリ)
  --key-id <id>         既存の GPG 鍵を使う (省略時: 新規生成)
  --name <name>         鍵生成時の名前 (省略時: 対話入力)
  --email <email>       鍵生成時のメールアドレス (省略時: 対話入力)
  --passphrase <pass>   GPG パスフレーズ (省略時: 対話入力。履歴に残るため対話入力を推奨)
  --keyservers <csv>    公開鍵の送信先 (デフォルト: keyserver.ubuntu.com,keys.openpgp.org)
  --yes                 確認プロンプトをすべて承認扱いにする
  --dry-run             副作用なしで実行計画だけ表示する
  -h, --help            このヘルプ
USAGE
}

# ---------- 引数パース ----------
repo=""
key_id=""
name=""
email=""
passphrase=""
passphrase_set=0
keyservers="keyserver.ubuntu.com,keys.openpgp.org"
dry_run=0
assume_yes=0

need_arg() {
  if [ "$2" -lt 2 ]; then die "オプション $1 に値がない" "引数が不足している" "$1 <値> の形式で指定する"; fi
}
while [ $# -gt 0 ]; do
  case $1 in
    --repo) need_arg "$1" $#; repo=$2; shift 2 ;;
    --key-id) need_arg "$1" $#; key_id=$2; shift 2 ;;
    --name) need_arg "$1" $#; name=$2; shift 2 ;;
    --email) need_arg "$1" $#; email=$2; shift 2 ;;
    --passphrase) need_arg "$1" $#; passphrase=$2; passphrase_set=1; shift 2 ;;
    --keyservers) need_arg "$1" $#; keyservers=$2; shift 2 ;;
    --yes) assume_yes=1; shift ;;
    --dry-run) dry_run=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) usage; die "不明なオプション: $1" "サポートされていない引数" "--help でオプション一覧を確認する" ;;
  esac
done

is_tty() { [ -t 0 ]; }
confirm() {
  # 副作用の直前に呼ぶ。承認なら 0、拒否なら 1 を返す。
  if [ "$assume_yes" = 1 ]; then return 0; fi
  if ! is_tty; then
    die "確認プロンプトを表示できない (非対話環境)" "「$1」は副作用を伴うため確認が必要" "端末から対話実行するか --yes を付ける (--dry-run で計画のみ確認可)"
  fi
  printf '[setup-secrets] %s [y/N]: ' "$1" >&2
  local ans=""
  read -r ans || ans=""
  case $ans in
    y|Y|yes|Yes|YES) return 0 ;;
    *) return 1 ;;
  esac
}
prompt_value() {
  # $1: プロンプト表示, $2: secret なら 1
  if ! is_tty; then
    die "対話入力が必要 ($1)" "非対話環境では入力できない" "端末から実行するか、対応するオプションで値を渡す (--dry-run で計画のみ確認可)"
  fi
  printf '[setup-secrets] %s: ' "$1" >&2
  local v=""
  if [ "${2:-0}" = 1 ]; then
    read -rs v
    printf '\n' >&2
  else
    read -r v
  fi
  printf '%s' "$v"
}

# ---------- preflight ----------
if ! command -v gpg >/dev/null 2>&1; then
  die "gpg が見つからない" "GPG 鍵の生成・export に必要" "macOS: brew install gnupg / Linux: apt-get install gnupg"
fi
if ! command -v gh >/dev/null 2>&1; then
  die "gh (GitHub CLI) が見つからない" "GitHub Secrets の登録に必要" "https://cli.github.com/ からインストールし gh auth login する"
fi
if ! gh auth status >/dev/null 2>&1; then
  if [ "$dry_run" = 1 ]; then
    log "警告: gh が未認証 (dry-run のため続行)"
  else
    die "gh が GitHub に未認証" "gh secret set には認証が必要" "gh auth login を実行してから再実行する"
  fi
fi
if [ -z "$repo" ]; then
  repo=$(gh repo view --json nameWithOwner -q .nameWithOwner 2>/dev/null || true)
  if [ -z "$repo" ]; then
    if [ "$dry_run" = 1 ]; then
      repo="<owner>/<repo>"
      log "警告: 対象リポジトリを特定できない (dry-run のため placeholder で続行)"
    else
      die "Secrets を登録するリポジトリを特定できない" "カレントディレクトリが GitHub リポジトリでない" "--repo <owner>/<repo> を指定するか、対象リポジトリ内で実行する"
    fi
  fi
fi
log "対象リポジトリ: $repo"

keyservers_sent=""
secrets_set=""

# ---------- 1-2. GPG 鍵の用意とフィンガープリント ----------
fpr=""
if [ -n "$key_id" ]; then
  fpr=$(gpg --list-secret-keys --with-colons "$key_id" 2>/dev/null | awk -F: '/^fpr:/ {print $10; exit}' || true)
  if [ -z "$fpr" ]; then
    die "GPG 鍵 $key_id が見つからない" "gpg --list-secret-keys に存在しない" "gpg --list-secret-keys --keyid-format long で確認し、正しい ID を --key-id に指定する"
  fi
  log "既存の GPG 鍵を使用: $fpr"
  if [ "$passphrase_set" = 0 ] && [ "$dry_run" = 0 ]; then
    passphrase=$(prompt_value "鍵 $key_id のパスフレーズ (SIGNING_PASSWORD として登録)" 1)
    passphrase_set=1
  fi
else
  if [ "$dry_run" = 1 ]; then
    log "[dry-run] GPG 鍵を新規生成する (RSA 4096, 無期限, 名前/メール/パスフレーズは対話入力)"
    fpr="DRYRUN000000000000000000000000000DRYRUN0"
  else
    if [ -z "$name" ]; then name=$(prompt_value "鍵に載せる名前 (公開される)" 0); fi
    if [ -z "$email" ]; then email=$(prompt_value "鍵に載せるメールアドレス (公開される)" 0); fi
    if [ "$passphrase_set" = 0 ]; then
      passphrase=$(prompt_value "新しい鍵のパスフレーズ (SIGNING_PASSWORD として登録)" 1)
      passphrase_set=1
    fi
    if [ -z "$name" ] || [ -z "$email" ]; then
      die "名前またはメールアドレスが空" "GPG 鍵の生成に必須" "--name / --email で指定するか、プロンプトに入力する"
    fi
    if confirm "GPG 鍵を新規生成する (RSA 4096, $name <$email>)"; then
      batch_file=$(mktemp)
      trap 'rm -f "$batch_file"' EXIT
      {
        printf 'Key-Type: RSA\n'
        printf 'Key-Length: 4096\n'
        printf 'Subkey-Type: RSA\n'
        printf 'Subkey-Length: 4096\n'
        printf 'Name-Real: %s\n' "$name"
        printf 'Name-Email: %s\n' "$email"
        printf 'Expire-Date: 0\n'
        if [ -n "$passphrase" ]; then
          printf 'Passphrase: %s\n' "$passphrase"
        else
          printf '%%no-protection\n'
        fi
        printf '%%commit\n'
      } > "$batch_file"
      gen_out=$(gpg --batch --status-fd 1 --generate-key "$batch_file") || die "GPG 鍵の生成に失敗" "gpg --batch --generate-key がエラーを返した" "stderr の gpg エラーを確認する (gpg 2.1+ が必要)"
      rm -f "$batch_file"
      fpr=$(printf '%s\n' "$gen_out" | awk '/KEY_CREATED/ {print $4; exit}')
      if [ -z "$fpr" ]; then
        die "生成した鍵のフィンガープリントを取得できない" "gpg の KEY_CREATED 出力が想定と異なる" "gpg --list-secret-keys --keyid-format long で確認し --key-id で再実行する"
      fi
      log "GPG 鍵を生成した: $fpr"
    else
      die "GPG 鍵の生成がキャンセルされた" "ユーザーが確認プロンプトを拒否した" "既存鍵を使う場合は --key-id <id> を指定して再実行する"
    fi
  fi
fi
short_id=$(printf '%s' "$fpr" | tail -c 8)
log "SIGNING_KEY_ID (末尾 8 桁): $short_id"

# ---------- 3. 公開鍵のキーサーバー送信 (副作用 → 確認) ----------
ks_list=$(printf '%s' "$keyservers" | tr ',' ' ')
if [ "$dry_run" = 1 ]; then
  log "[dry-run] 公開鍵 $fpr を送信する: $ks_list"
else
  if confirm "公開鍵をキーサーバー ($keyservers) に送信する"; then
    for ks in $ks_list; do
      if gpg --keyserver "$ks" --send-keys "$fpr" 2>/dev/null; then
        keyservers_sent="$keyservers_sent$ks"$'\n'
        log "送信成功: $ks"
      else
        log "警告: $ks への送信に失敗 (続行。後で手動: gpg --keyserver $ks --send-keys $fpr)"
      fi
    done
  else
    log "キーサーバー送信をスキップ (後で手動: gpg --keyserver keyserver.ubuntu.com --send-keys $fpr)"
  fi
fi

# ---------- 4. 秘密鍵の export ----------
key_armor=""
if [ "$dry_run" = 1 ]; then
  log "[dry-run] 秘密鍵を ASCII armor 形式で export する (GPG_KEY_CONTENTS)"
else
  key_armor=$(gpg --batch --pinentry-mode loopback --passphrase "$passphrase" --armor --export-secret-keys "$fpr") || die "秘密鍵の export に失敗" "パスフレーズ誤り、または gpg が pinentry loopback 非対応" "パスフレーズを確認する。gpg 2.1+ を使用する"
  if [ -z "$key_armor" ]; then
    die "秘密鍵の export 結果が空" "指定した鍵に秘密鍵が無い可能性" "gpg --list-secret-keys で秘密鍵の存在を確認する"
  fi
fi

# ---------- 5. Central Portal User Token (唯一のユーザー手作業) ----------
central_user=""
central_pass=""
if [ "$dry_run" = 1 ]; then
  log "[dry-run] Sonatype Central Portal の User Token 入力を求める (https://central.sonatype.com/account)"
else
  {
    printf '[setup-secrets] --- ユーザー手作業: Sonatype Central Portal の User Token 発行 ---\n'
    printf '[setup-secrets]  1. https://central.sonatype.com/account を開く (GitHub アカウントでサインイン)\n'
    printf '[setup-secrets]  2. "Generate User Token" をクリック\n'
    printf '[setup-secrets]  3. 表示された Username / Password を下のプロンプトに入力する (一度しか表示されない)\n'
    printf '[setup-secrets]  ※ namespace 未登録の場合は先に Namespaces → Add Namespace で Group ID を検証しておく\n'
  } >&2
  central_user=$(prompt_value "Central Portal Token の Username (MAVEN_CENTRAL_USERNAME)" 0)
  central_pass=$(prompt_value "Central Portal Token の Password (MAVEN_CENTRAL_PASSWORD)" 1)
  if [ -z "$central_user" ] || [ -z "$central_pass" ]; then
    die "Central Portal の User Token が入力されなかった" "MAVEN_CENTRAL_USERNAME / MAVEN_CENTRAL_PASSWORD に必要" "https://central.sonatype.com/account で User Token を発行して再実行する"
  fi
fi

# ---------- 6. gh secret set ×5 (副作用 → 確認) ----------
status="ok"
if [ "$dry_run" = 1 ]; then
  log "[dry-run] gh secret set を実行する: MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD, SIGNING_KEY_ID, SIGNING_PASSWORD, GPG_KEY_CONTENTS → $repo"
else
  if confirm "$repo に 5 つの GitHub Secrets を登録する"; then
    set_secret() {
      # $1: Secret 名, $2: 値
      printf '%s' "$2" | gh secret set "$1" --repo "$repo" >&2 || die "gh secret set $1 に失敗" "権限不足・ネットワーク・リポジトリ名誤りの可能性" "gh auth status と $repo への admin 権限を確認する"
      secrets_set="$secrets_set$1"$'\n'
      log "登録: $1"
    }
    set_secret MAVEN_CENTRAL_USERNAME "$central_user"
    set_secret MAVEN_CENTRAL_PASSWORD "$central_pass"
    set_secret SIGNING_KEY_ID "$short_id"
    set_secret SIGNING_PASSWORD "$passphrase"
    set_secret GPG_KEY_CONTENTS "$key_armor"
  else
    status="incomplete"
    log "Secrets 登録をスキップ (references/github-secrets.md の手順で手動登録すること)"
  fi
fi

# ---------- 結果 JSON (stdout 末尾 1 行) ----------
json_escape() {
  local s=$1
  s=${s//\\/\\\\}
  s=${s//\"/\\\"}
  s=${s//$'\n'/\\n}
  s=${s//$'\t'/\\t}
  printf '%s' "$s"
}
json_array() {
  local out="" first=1 line
  while IFS= read -r line; do
    if [ -z "$line" ]; then continue; fi
    if [ "$first" = 1 ]; then first=0; else out="$out,"; fi
    out="$out\"$(json_escape "$line")\""
  done <<EOF
$1
EOF
  printf '[%s]' "$out"
}

dry_json=false
if [ "$dry_run" = 1 ]; then dry_json=true; fi
printf '{"status":"%s","dry_run":%s,"repo":"%s","gpg_fingerprint":"%s","signing_key_id":"%s","keyservers_sent":%s,"secrets_set":%s,"remaining_manual_steps":["GitHub Release を作成して publish workflow の成功を確認する"]}\n' \
  "$status" \
  "$dry_json" \
  "$(json_escape "$repo")" \
  "$(json_escape "$fpr")" \
  "$(json_escape "$short_id")" \
  "$(json_array "$keyservers_sent")" \
  "$(json_array "$secrets_set")"
