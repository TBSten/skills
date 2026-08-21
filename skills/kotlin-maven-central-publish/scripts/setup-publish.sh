#!/usr/bin/env bash
# setup-publish.sh — Kotlin/KMP プロジェクトに Maven Central 公開設定を一括セットアップする。
#
# TBSten/skills skills/kotlin-maven-central-publish に同梱。
# やること:
#   1. gradle/libs.versions.toml に mavenPublish プラグインを冪等追記
#   2. buildSrc/build.gradle.kts / buildSrc/settings.gradle.kts の生成 (既存ありなら ACTION_REQUIRED)
#   3. buildSrc/src/main/kotlin/publish-convention.gradle.kts をプレースホルダー置換して生成
#   4. .github/workflows/publish.yml の生成
# やらないこと (AI / ユーザーの責務):
#   - 公開対象モジュールへの id("publish-convention") 適用と group/version 設定
#   - GPG 鍵・GitHub Secrets のセットアップ (scripts/setup-secrets.sh を使う)
#
# 冪等: 再実行しても二重追記しない。既存ファイルは --force なしでは上書きしない。
# stdout の末尾 1 行に結果 JSON を出力する。ログ・エラーは stderr。
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
EXAMPLE_DIR="$SCRIPT_DIR/../example"
RAW_EXAMPLE_BASE="https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/example"
DEFAULT_MAVEN_PUBLISH_VERSION="0.30.0"

log() { printf '[setup-publish] %s\n' "$*" >&2; }
die() {
  # die <何が> [<なぜ>] [<どう直すか>]
  printf '[setup-publish] ERROR: %s\n' "$1" >&2
  if [ $# -ge 2 ]; then printf '[setup-publish]   原因: %s\n' "$2" >&2; fi
  if [ $# -ge 3 ]; then printf '[setup-publish]   対処: %s\n' "$3" >&2; fi
  exit 1
}

usage() {
  cat >&2 <<'USAGE'
Usage: setup-publish.sh --description "<POM description>" [options]

Options:
  --description <text>       POM の description (必須。推定不可のため)
  --group-id <id>            Maven Group ID (結果 JSON に含める参考情報。例: com.example.mylib)
  --github-url <url>         GitHub リポジトリ URL (省略時: git remote get-url origin から推定)
  --license <MIT|Apache-2.0> ライセンス (省略時: LICENSE ファイルから推定)
  --license-name <name>      カスタムライセンス名 (--license-url とセットで --license の代わりに指定)
  --license-url <url>        カスタムライセンス URL
  --developer-id <id>        開発者 ID (省略時: GitHub owner)
  --developer-name <name>    開発者名 (省略時: git config user.name、なければ developer-id)
  --developer-url <url>      開発者 URL (省略時: https://github.com/<developer-id>)
  --start-year <yyyy>        プロジェクト開始年 (省略時: git の最初のコミット年、なければ今年)
  --maven-publish-version <v> Vanniktech Maven Publish のバージョン (デフォルト: 0.30.0)
  --project-root <dir>       プロジェクトルート (デフォルト: カレントディレクトリ)
  --force                    既存ファイルが内容不一致でも上書きする
  -h, --help                 このヘルプ
USAGE
}

# ---------- 引数パース ----------
group_id=""
description=""
github_url=""
license=""
license_name=""
license_url=""
developer_id=""
developer_name=""
developer_url=""
start_year=""
mp_version="$DEFAULT_MAVEN_PUBLISH_VERSION"
project_root="."
force=0

need_arg() {
  if [ "$2" -lt 2 ]; then die "オプション $1 に値がない" "引数が不足している" "$1 <値> の形式で指定する"; fi
}
while [ $# -gt 0 ]; do
  case $1 in
    --group-id) need_arg "$1" $#; group_id=$2; shift 2 ;;
    --description) need_arg "$1" $#; description=$2; shift 2 ;;
    --github-url) need_arg "$1" $#; github_url=$2; shift 2 ;;
    --license) need_arg "$1" $#; license=$2; shift 2 ;;
    --license-name) need_arg "$1" $#; license_name=$2; shift 2 ;;
    --license-url) need_arg "$1" $#; license_url=$2; shift 2 ;;
    --developer-id) need_arg "$1" $#; developer_id=$2; shift 2 ;;
    --developer-name) need_arg "$1" $#; developer_name=$2; shift 2 ;;
    --developer-url) need_arg "$1" $#; developer_url=$2; shift 2 ;;
    --start-year) need_arg "$1" $#; start_year=$2; shift 2 ;;
    --maven-publish-version) need_arg "$1" $#; mp_version=$2; shift 2 ;;
    --project-root) need_arg "$1" $#; project_root=$2; shift 2 ;;
    --force) force=1; shift ;;
    -h|--help) usage; exit 0 ;;
    *) usage; die "不明なオプション: $1" "サポートされていない引数" "--help でオプション一覧を確認する" ;;
  esac
done

# ---------- preflight ----------
if [ ! -d "$project_root" ]; then
  die "プロジェクトルート $project_root が存在しない" "--project-root の指定誤りの可能性" "正しいディレクトリを --project-root で指定する"
fi
catalog="$project_root/gradle/libs.versions.toml"
if [ ! -f "$catalog" ]; then
  die "gradle/libs.versions.toml が見つからない ($catalog)" "このスキルは Gradle version catalog を前提とする" "gradle/libs.versions.toml を作成するか、--project-root で正しいプロジェクトルートを指定する"
fi
if [ -z "$description" ]; then
  die "--description が未指定" "POM の description はプロジェクト固有で推定できない" '--description "<プロジェクトの説明 (英語)>" を指定して再実行する'
fi

TMP_DIR=$(mktemp -d)
trap 'rm -rf "$TMP_DIR"' EXIT

# ---------- 推定 ----------
if [ -z "$github_url" ]; then
  remote_url=$(git -C "$project_root" remote get-url origin 2>/dev/null || true)
  case $remote_url in
    git@github.com:*)
      rest=${remote_url#git@github.com:}; rest=${rest%.git}
      github_url="https://github.com/$rest" ;;
    ssh://git@github.com/*)
      rest=${remote_url#ssh://git@github.com/}; rest=${rest%.git}
      github_url="https://github.com/$rest" ;;
    https://github.com/*)
      rest=${remote_url#https://github.com/}; rest=${rest%.git}
      github_url="https://github.com/$rest" ;;
    "")
      die "--github-url が未指定で、git remote origin からも取得できない" "git リポジトリでないか origin が未設定" "--github-url https://github.com/<owner>/<repo> を指定する" ;;
    *)
      die "git remote origin が GitHub URL ではない: $remote_url" "GitHub 以外のリモートからは owner/repo を推定できない" "--github-url https://github.com/<owner>/<repo> を指定する" ;;
  esac
  log "GitHub URL を git remote origin から推定: $github_url"
fi
gh_path=${github_url#https://github.com/}
gh_path=${gh_path%/}
case $gh_path in
  */*) ;;
  *) die "GitHub URL から owner/repo を解析できない: $github_url" "https://github.com/<owner>/<repo> の形式でない" "--github-url を正しい形式で指定する" ;;
esac
github_owner=${gh_path%%/*}
github_repo=${gh_path#*/}
github_repo=${github_repo%.git}
case $github_repo in
  */*|"") die "GitHub URL から owner/repo を解析できない: $github_url" "https://github.com/<owner>/<repo> の形式でない" "--github-url を正しい形式で指定する" ;;
esac
if [ -z "$github_owner" ]; then
  die "GitHub URL から owner を解析できない: $github_url" "https://github.com/<owner>/<repo> の形式でない" "--github-url を正しい形式で指定する"
fi

if [ -z "$license_name" ] || [ -z "$license_url" ]; then
  if [ -z "$license" ]; then
    lic_file=""
    for f in LICENSE LICENSE.md LICENSE.txt LICENCE LICENCE.md; do
      if [ -f "$project_root/$f" ]; then lic_file="$project_root/$f"; break; fi
    done
    if [ -z "$lic_file" ]; then
      die "ライセンスを特定できない (LICENSE ファイルが見つからない)" "--license 未指定かつ LICENSE / LICENSE.md 等が存在しない" "--license MIT|Apache-2.0 か、--license-name/--license-url のペアを指定する"
    fi
    if grep -qi 'MIT License' "$lic_file"; then
      license="MIT"
    elif grep -qi 'Apache License' "$lic_file" && grep -q 'Version 2.0' "$lic_file"; then
      license="Apache-2.0"
    else
      die "$lic_file からライセンスを判別できない" "MIT / Apache-2.0 のどちらの定型文とも一致しない" "--license MIT|Apache-2.0 か、--license-name/--license-url のペアを指定する"
    fi
    log "ライセンスを $lic_file から推定: $license"
  fi
  case $license in
    MIT|mit)
      license_name="MIT License"
      license_url="https://opensource.org/licenses/MIT" ;;
    Apache-2.0|apache-2.0|Apache2|apache2)
      license_name="The Apache License, Version 2.0"
      license_url="https://www.apache.org/licenses/LICENSE-2.0.txt" ;;
    *)
      die "未対応のライセンス: $license" "組み込みマッピングは MIT / Apache-2.0 のみ" "--license-name と --license-url のペアで直接指定する" ;;
  esac
fi

if [ -z "$developer_id" ]; then
  developer_id=$github_owner
  log "developer-id を GitHub owner から推定: $developer_id"
fi
if [ -z "$developer_name" ]; then
  developer_name=$(git -C "$project_root" config user.name 2>/dev/null || true)
  if [ -z "$developer_name" ]; then developer_name=$developer_id; fi
  log "developer-name を推定: $developer_name"
fi
if [ -z "$developer_url" ]; then
  developer_url="https://github.com/$developer_id"
fi

if [ -z "$start_year" ]; then
  start_year=$(git -C "$project_root" log --reverse --format=%ad --date=format:%Y 2>/dev/null | head -n 1 || true)
  if [ -z "$start_year" ]; then start_year=$(date +%Y); fi
  log "開始年を推定: $start_year"
fi
case $start_year in
  [0-9][0-9][0-9][0-9]) ;;
  *) die "開始年が不正: $start_year" "4 桁の西暦である必要がある" "--start-year 2024 のように指定する" ;;
esac

# ---------- テンプレート解決 (ローカル example/ → GitHub raw フォールバック) ----------
resolve_template() {
  # $1: example/ 配下のファイル名。パスを stdout に返す。
  if [ -f "$EXAMPLE_DIR/$1" ]; then
    printf '%s\n' "$EXAMPLE_DIR/$1"
    return 0
  fi
  if ! command -v curl >/dev/null 2>&1; then
    die "テンプレート $1 がローカルに無く、curl も見つからない" "script 単体実行時はテンプレートを GitHub から取得する必要がある" "curl をインストールするか、skill 一式 (example/ 含む) を配置して実行する"
  fi
  log "テンプレート $1 を GitHub から取得"
  if ! curl -fsSL "$RAW_EXAMPLE_BASE/$1" -o "$TMP_DIR/$1"; then
    die "テンプレート $1 のダウンロードに失敗" "ネットワーク障害または URL 変更の可能性" "接続を確認するか、skill 一式 (example/ 含む) を配置して実行する"
  fi
  printf '%s\n' "$TMP_DIR/$1"
}

# ---------- 結果アキュムレータ ----------
CHANGED=""
SKIPPED=""
ACTION_REQUIRED=""
add_changed() { CHANGED="$CHANGED$1"$'\n'; log "変更: $1"; }
add_skipped() { SKIPPED="$SKIPPED$1"$'\n'; log "スキップ: $1"; }
add_action() { ACTION_REQUIRED="$ACTION_REQUIRED$1"$'\n'; printf '[setup-publish] ACTION_REQUIRED: %s\n' "$1" >&2; }

# ---------- 1. version catalog への冪等追記 ----------
toml_insert() {
  # $1: セクション名 (versions / plugins), $2: 追記する行
  awk -v hdr="[$1]" -v line="$2" '
    {
      print
      if (!ins) {
        t = $0
        gsub(/[[:space:]]/, "", t)
        if (t == hdr) { print line; ins = 1 }
      }
    }
    END { if (!ins) { printf "\n%s\n%s\n", hdr, line } }
  ' "$catalog" > "$TMP_DIR/catalog.toml"
  mv "$TMP_DIR/catalog.toml" "$catalog"
}

need_version=1
need_plugin=1
if grep -Eq '^[[:space:]]*mavenPublish[[:space:]]*=[[:space:]]*"' "$catalog"; then need_version=0; fi
if grep -q 'com\.vanniktech\.maven\.publish' "$catalog"; then need_plugin=0; fi
if [ "$need_version" = 0 ] && [ "$need_plugin" = 0 ]; then
  add_skipped "gradle/libs.versions.toml (mavenPublish 追加済み)"
else
  if [ "$need_version" = 1 ]; then
    toml_insert versions "mavenPublish = \"$mp_version\""
  fi
  if [ "$need_plugin" = 1 ]; then
    toml_insert plugins 'mavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "mavenPublish" }'
  fi
  add_changed "gradle/libs.versions.toml"
fi

# ---------- 2. buildSrc ----------
bs_build="$project_root/buildSrc/build.gradle.kts"
if [ -f "$bs_build" ]; then
  if grep -q 'mavenPublish' "$bs_build"; then
    add_skipped "buildSrc/build.gradle.kts (mavenPublish 依存が既にある)"
  else
    add_action "buildSrc/build.gradle.kts が既に存在する。example/buildSrc-build.gradle.kts の dependencies ブロック (libs.plugins.mavenPublish の implementation) を手動でマージすること"
  fi
else
  mkdir -p "$project_root/buildSrc"
  tpl=$(resolve_template buildSrc-build.gradle.kts)
  cp "$tpl" "$bs_build"
  add_changed "buildSrc/build.gradle.kts"
fi

bs_settings="$project_root/buildSrc/settings.gradle.kts"
if [ -f "$bs_settings" ]; then
  if grep -q 'libs\.versions\.toml' "$bs_settings"; then
    add_skipped "buildSrc/settings.gradle.kts (version catalog import 済み)"
  else
    add_action "buildSrc/settings.gradle.kts が既に存在するが version catalog を import していない。example/buildSrc-settings.gradle.kts の dependencyResolutionManagement を手動でマージすること"
  fi
else
  mkdir -p "$project_root/buildSrc"
  tpl=$(resolve_template buildSrc-settings.gradle.kts)
  cp "$tpl" "$bs_settings"
  add_changed "buildSrc/settings.gradle.kts"
fi

# ---------- 3. publish-convention.gradle.kts (プレースホルダー置換) ----------
install_file() {
  # $1: 生成済みソース, $2: 配置先, $3: 表示ラベル
  if [ -f "$2" ]; then
    if cmp -s "$1" "$2"; then
      add_skipped "$3 (内容一致)"
    elif [ "$force" = 1 ]; then
      cp "$1" "$2"
      add_changed "$3 (--force で上書き)"
    else
      add_action "$3 が既に存在し内容が異なる。--force で上書きするか、手動で差分を取り込むこと"
    fi
  else
    mkdir -p "$(dirname "$2")"
    cp "$1" "$2"
    add_changed "$3"
  fi
}

kts_escape() {
  # Kotlin 文字列リテラル用エスケープ。プレースホルダーは全て "..." の中に
  # 展開されるため、\ / " / $ (文字列テンプレート化を防ぐ) / 改行等を
  # エスケープしないと生成される .gradle.kts が壊れる。
  local s=$1
  s=${s//\\/\\\\}
  s=${s//\"/\\\"}
  s=${s//\$/\\\$}
  s=${s//$'\r'/\\r}
  s=${s//$'\n'/\\n}
  s=${s//$'\t'/\\t}
  printf '%s' "$s"
}

tpl=$(resolve_template publish-convention.gradle.kts)
content=$(cat "$tpl")
esc=$(kts_escape "$description");    content=${content//<PROJECT_DESCRIPTION>/$esc}
esc=$(kts_escape "$github_url");     content=${content//<GITHUB_URL>/$esc}
esc=$(kts_escape "$start_year");     content=${content//<INCEPTION_YEAR>/$esc}
esc=$(kts_escape "$license_name");   content=${content//<LICENSE_NAME>/$esc}
esc=$(kts_escape "$license_url");    content=${content//<LICENSE_URL>/$esc}
esc=$(kts_escape "$developer_id");   content=${content//<DEVELOPER_ID>/$esc}
esc=$(kts_escape "$developer_name"); content=${content//<DEVELOPER_NAME>/$esc}
esc=$(kts_escape "$developer_url");  content=${content//<DEVELOPER_URL>/$esc}
esc=$(kts_escape "$github_owner");   content=${content//<GITHUB_OWNER>/$esc}
esc=$(kts_escape "$github_repo");    content=${content//<GITHUB_REPO>/$esc}
printf '%s\n' "$content" > "$TMP_DIR/publish-convention.rendered.gradle.kts"
if grep -Eq '<[A-Z_]+>' "$TMP_DIR/publish-convention.rendered.gradle.kts"; then
  leftover=$(grep -Eo '<[A-Z_]+>' "$TMP_DIR/publish-convention.rendered.gradle.kts" | sort -u | tr '\n' ' ')
  die "プレースホルダーが置換されずに残った: $leftover" "テンプレートと script の置換リストがずれている" "TBSten/skills の skills/kotlin-maven-central-publish に issue 報告するか、生成後のファイルを手動修正する"
fi
install_file "$TMP_DIR/publish-convention.rendered.gradle.kts" \
  "$project_root/buildSrc/src/main/kotlin/publish-convention.gradle.kts" \
  "buildSrc/src/main/kotlin/publish-convention.gradle.kts"

# ---------- 4. publish.yml ----------
tpl=$(resolve_template publish.yml)
install_file "$tpl" "$project_root/.github/workflows/publish.yml" ".github/workflows/publish.yml"

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

status="ok"
if [ -n "$ACTION_REQUIRED" ]; then status="action_required"; fi
next_steps='"公開対象モジュールの build.gradle.kts に id(\"publish-convention\") と group/version を追加する","./gradlew publishToMavenLocal で動作確認する","scripts/setup-secrets.sh で GPG 鍵と GitHub Secrets を設定する"'
printf '{"status":"%s","changed":%s,"skipped":%s,"action_required":%s,"group_id":"%s","description":"%s","github_url":"%s","github_owner":"%s","github_repo":"%s","license_name":"%s","license_url":"%s","developer_id":"%s","developer_name":"%s","developer_url":"%s","inception_year":"%s","maven_publish_version":"%s","next_steps":[%s]}\n' \
  "$status" \
  "$(json_array "$CHANGED")" \
  "$(json_array "$SKIPPED")" \
  "$(json_array "$ACTION_REQUIRED")" \
  "$(json_escape "$group_id")" \
  "$(json_escape "$description")" \
  "$(json_escape "$github_url")" \
  "$(json_escape "$github_owner")" \
  "$(json_escape "$github_repo")" \
  "$(json_escape "$license_name")" \
  "$(json_escape "$license_url")" \
  "$(json_escape "$developer_id")" \
  "$(json_escape "$developer_name")" \
  "$(json_escape "$developer_url")" \
  "$(json_escape "$start_year")" \
  "$(json_escape "$mp_version")" \
  "$next_steps"
