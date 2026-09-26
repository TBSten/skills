#!/usr/bin/env bash
# setup-publish.sh — Kotlin/KMP プロジェクトに Maven Central 公開設定を一括セットアップする。
#
# TBSten/skills skills/kotlin-maven-central-publish に同梱。
# やること:
#   1. gradle/libs.versions.toml に mavenPublish プラグイン (+ 任意で自ライブラリのバージョン) を冪等追記
#      vanniktech のバージョンは Gradle wrapper / Kotlin のバージョンから互換なものを自動選択
#   2. <convention-dir>/build.gradle.kts / settings.gradle.kts の生成 (既存ありなら ACTION_REQUIRED)
#      convention-dir は buildSrc (デフォルト) か build-logic 等の included build
#   3. <convention-dir>/src/main/kotlin/publish-convention.gradle.kts をプレースホルダー置換して生成
#   4. .github/workflows/publish.yml / publish-check.yml の生成
#      (runner は Apple ターゲットの有無で macos-latest / ubuntu-latest を自動選択)
# やらないこと (AI / ユーザーの責務):
#   - 公開対象モジュールへの id("publish-convention") 適用と group/version 設定
#   - Dokka / languageVersion 床などの任意設定 (references/convention-options.md)
#   - GPG 鍵・GitHub Secrets のセットアップ (scripts/setup-secrets.sh を使う)
#
# 冪等: 再実行しても二重追記しない。既存ファイルは --force なしでは上書きしない。
# stdout の末尾 1 行に結果 JSON を出力する。ログ・エラーは stderr。
set -euo pipefail

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
EXAMPLE_DIR="$SCRIPT_DIR/../example"
RAW_EXAMPLE_BASE="https://raw.githubusercontent.com/TBSten/skills/refs/heads/main/skills/kotlin-maven-central-publish/example"
# vanniktech のバージョンと最低要件 (CHANGELOG.md で確認)
#   0.37.0: Gradle 9.0 / KGP 2.2.0 / JDK 17
#   0.35.0: Gradle 8.13 / KGP 1.9.20 / JDK 11
#   0.34.0: Gradle 8.5  / KGP 1.9.20 / JDK 11 (SonatypeHost が DSL から削除された最初の版)
DEFAULT_MAVEN_PUBLISH_VERSION="0.37.0"

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
  --maven-publish-version <v> Vanniktech Maven Publish のバージョン
                             (catalog に既にあればそれを使う。無ければ Gradle>=9 かつ Kotlin>=2.2 で 0.37.0、Gradle>=8.13 で 0.35.0、それ以外 0.34.0)
  --version-ref <key>        自ライブラリのバージョンを持つ version catalog [versions] のキー (例: mylib)。
                             publish.yml の tag 一致チェックが読む SSoT。省略時は gradle.properties の VERSION_NAME を使う
  --version <v>              --version-ref のキーが catalog に無い時に追記する初期バージョン (例: 0.1.0)
  --convention-dir <dir>     convention plugin を置くディレクトリ (デフォルト: buildSrc。build-logic 等の included build も可)
  --skip-publish-check       PR で publishToMavenLocal を回す .github/workflows/publish-check.yml を生成しない
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
mp_version=""
version_ref=""
lib_version=""
convention_dir="buildSrc"
skip_publish_check=0
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
    --version-ref) need_arg "$1" $#; version_ref=$2; shift 2 ;;
    --version) need_arg "$1" $#; lib_version=$2; shift 2 ;;
    --convention-dir) need_arg "$1" $#; convention_dir=${2%/}; shift 2 ;;
    --skip-publish-check) skip_publish_check=1; shift ;;
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
case $convention_dir in
  ""|/*|*..*) die "--convention-dir が不正: $convention_dir" "プロジェクトルートからの相対パスである必要がある" "--convention-dir buildSrc や --convention-dir build-logic のように指定する" ;;
esac

# ---------- バージョン SSoT (publish.yml の tag 一致チェックが読む) ----------
if [ -n "$version_ref" ]; then
  case $version_ref in
    *[!A-Za-z0-9_.-]*) die "--version-ref が不正: $version_ref" "version catalog のキーに使えない文字を含む" "--version-ref mylib のように英数字と - _ . で指定する" ;;
  esac
  if ! grep -Eq "^[[:space:]]*${version_ref}[[:space:]]*=[[:space:]]*\"" "$catalog" && [ -z "$lib_version" ]; then
    die "version catalog に $version_ref のバージョンが無い" "--version-ref のキーが [versions] に未定義で、追記する初期値も渡されていない" "--version <初期バージョン (例: 0.1.0)> を併せて指定するか、[versions] に $version_ref を追加する"
  fi
  read_version_cmd="grep -E '^[[:space:]]*${version_ref}[[:space:]]*=' gradle/libs.versions.toml | head -n 1 | cut -d'\"' -f2"
  version_source="gradle/libs.versions.toml [versions] $version_ref"
elif [ -f "$project_root/gradle.properties" ] && grep -Eq '^[[:space:]]*VERSION_NAME[[:space:]]*=' "$project_root/gradle.properties"; then
  read_version_cmd="grep -E '^[[:space:]]*VERSION_NAME[[:space:]]*=' gradle.properties | head -n 1 | cut -d= -f2 | tr -d '[:space:]'"
  version_source="gradle.properties VERSION_NAME"
else
  die "自ライブラリのバージョンの SSoT を特定できない" "publish.yml は tag とバージョンの一致を検証するため、バージョンの置き場所が必要" "--version-ref <catalog のキー> --version <初期バージョン> を指定するか、gradle.properties に VERSION_NAME=<版> を書く"
fi
log "バージョンの SSoT: $version_source"

# ---------- vanniktech バージョンの自動選択 ----------
version_ge() {
  # version_ge <a> <b>: a >= b (major.minor の数値比較) なら 0
  local a_major a_minor b_major b_minor
  a_major=${1%%.*}; a_minor=${1#*.}; a_minor=${a_minor%%[!0-9]*}
  b_major=${2%%.*}; b_minor=${2#*.}; b_minor=${b_minor%%[!0-9]*}
  a_major=${a_major%%[!0-9]*}
  [ -z "$a_minor" ] && a_minor=0
  [ "$a_major" -gt "$b_major" ] || { [ "$a_major" -eq "$b_major" ] && [ "$a_minor" -ge "$b_minor" ]; }
}
if [ -z "$mp_version" ] && ! grep -q 'com\.vanniktech\.maven\.publish' "$catalog"; then
  gradle_version=""
  wrapper_props="$project_root/gradle/wrapper/gradle-wrapper.properties"
  if [ -f "$wrapper_props" ]; then
    gradle_version=$(sed -n 's/^distributionUrl=.*gradle-\([0-9][0-9.]*\)-.*/\1/p' "$wrapper_props" | head -n 1)
  fi
  kotlin_version=$(grep -E '^[[:space:]]*kotlin[[:space:]]*=[[:space:]]*"' "$catalog" | head -n 1 | cut -d'"' -f2 || true)
  mp_version=$DEFAULT_MAVEN_PUBLISH_VERSION
  if [ -n "$gradle_version" ] && ! version_ge "$gradle_version" 9.0; then
    if version_ge "$gradle_version" 8.13; then mp_version="0.35.0"
    elif version_ge "$gradle_version" 8.5; then mp_version="0.34.0"
    else die "Gradle $gradle_version は古すぎる" "Central Portal に対応した vanniktech (0.34.0+) は Gradle 8.5 以上が必要" "./gradlew wrapper --gradle-version <8.5 以上> で wrapper を上げてから再実行する"
    fi
  elif [ -n "$kotlin_version" ] && ! version_ge "$kotlin_version" 2.2; then
    mp_version="0.35.0"
  fi
  log "vanniktech maven-publish のバージョンを選択: $mp_version (Gradle=${gradle_version:-不明}, Kotlin=${kotlin_version:-不明})"
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

catalog_changed=0
# 既に vanniktech のプラグインエントリがあればそのエイリアスを使う (例: maven-publish → accessor maven.publish)
mp_alias=$(grep -E '^[[:space:]]*[A-Za-z0-9_.-]+[[:space:]]*=.*com\.vanniktech\.maven\.publish' "$catalog" | head -n 1 | sed -E 's/^[[:space:]]*([A-Za-z0-9_.-]+).*/\1/' || true)
if [ -n "$mp_alias" ]; then
  add_skipped "gradle/libs.versions.toml (vanniktech プラグイン $mp_alias は定義済み。バージョンは既存の値を使う)"
else
  mp_alias="mavenPublish"
  if ! grep -Eq '^[[:space:]]*mavenPublish[[:space:]]*=[[:space:]]*"' "$catalog"; then
    toml_insert versions "mavenPublish = \"$mp_version\""
  fi
  toml_insert plugins 'mavenPublish = { id = "com.vanniktech.maven.publish", version.ref = "mavenPublish" }'
  catalog_changed=1
fi
# Gradle の catalog accessor は - _ . を区切りとして扱う
mp_accessor=$(printf '%s' "$mp_alias" | tr '_-' '..')
if [ -n "$version_ref" ] && ! grep -Eq "^[[:space:]]*${version_ref}[[:space:]]*=[[:space:]]*\"" "$catalog"; then
  toml_insert versions "$version_ref = \"$lib_version\""
  catalog_changed=1
fi
if [ "$catalog_changed" = 1 ]; then
  add_changed "gradle/libs.versions.toml"
fi

# ---------- 2. convention plugin 用ビルド (buildSrc / build-logic) ----------
render() {
  # render <テンプレートパス> <出力パス> <PLACEHOLDER=値>...  (値は加工せずそのまま埋め込む)
  local src=$1 dst=$2 content kv key val leftover
  shift 2
  content=$(cat "$src")
  for kv in "$@"; do
    key=${kv%%=*}
    val=${kv#*=}
    content=${content//"<$key>"/"$val"}
  done
  printf '%s\n' "$content" > "$dst"
  if grep -Eq '<[A-Z_]+>' "$dst"; then
    leftover=$(grep -Eo '<[A-Z_]+>' "$dst" | sort -u | tr '\n' ' ')
    die "プレースホルダーが置換されずに残った ($(basename "$src")): $leftover" "テンプレートと script の置換リストがずれている" "TBSten/skills の skills/kotlin-maven-central-publish に issue 報告するか、生成後のファイルを手動修正する"
  fi
}

catalog_aliases_for() {
  # catalog_aliases_for <plugin-id の ERE>: [plugins] でその id を持つエイリアスの catalog accessor を 1 行ずつ出す
  grep -E "^[[:space:]]*[A-Za-z0-9_.-]+[[:space:]]*=.*id[[:space:]]*=[[:space:]]*\"($1)\"" "$catalog" \
    | sed -E 's/^[[:space:]]*([A-Za-z0-9_.-]+).*/\1/' | tr '_-' '..' || true
}
# vanniktech は KGP / AGP のクラスに触るため、同じ classpath (= convention ビルド) に載せる必要がある
KGP_IDS='org\.jetbrains\.kotlin\.(multiplatform|jvm|android)'
AGP_IDS='com\.android\.(kotlin\.multiplatform\.library|library|application)'
kgp_accessor=""
for id in 'org\.jetbrains\.kotlin\.multiplatform' 'org\.jetbrains\.kotlin\.jvm' 'org\.jetbrains\.kotlin\.android'; do
  kgp_accessor=$(catalog_aliases_for "$id" | head -n 1)
  if [ -n "$kgp_accessor" ]; then break; fi
done
if [ -z "$kgp_accessor" ]; then
  die "version catalog に Kotlin Gradle plugin のエイリアスが無い" "vanniktech は KGP を convention ビルドと同じ classpath に要求するため、catalog 経由で KGP を依存に載せる必要がある" '[plugins] に kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" } 等を追加して再実行する'
fi
agp_accessor=""
for id in 'com\.android\.kotlin\.multiplatform\.library' 'com\.android\.library' 'com\.android\.application'; do
  agp_accessor=$(catalog_aliases_for "$id" | head -n 1)
  if [ -n "$agp_accessor" ]; then break; fi
done
plugin_deps="    implementation(plugin(libs.plugins.$mp_accessor))
    implementation(plugin(libs.plugins.$kgp_accessor))"
if [ -n "$agp_accessor" ]; then
  plugin_deps="$plugin_deps
    implementation(plugin(libs.plugins.$agp_accessor))"
fi

cd_build="$project_root/$convention_dir/build.gradle.kts"
if [ -f "$cd_build" ]; then
  missing=""
  for acc in "$mp_accessor" "$kgp_accessor" $agp_accessor; do
    if ! grep -Eq "libs\.plugins\.$acc([^A-Za-z0-9.]|$)" "$cd_build"; then missing="$missing libs.plugins.$acc"; fi
  done
  if [ -z "$missing" ]; then
    add_skipped "$convention_dir/build.gradle.kts (必要なプラグイン依存が既にある)"
  else
    add_action "$convention_dir/build.gradle.kts が既に存在する。次の plugin marker 依存が見当たらないので example/buildSrc-build.gradle.kts を参考に dependencies へ手動でマージすること:$missing"
  fi
else
  mkdir -p "$project_root/$convention_dir"
  tpl=$(resolve_template buildSrc-build.gradle.kts)
  render "$tpl" "$cd_build" "PLUGIN_DEPENDENCIES=$plugin_deps"
  add_changed "$convention_dir/build.gradle.kts"
fi

# buildSrc の classpath に載せたプラグインをバージョン付きで要求すると
# "already on the classpath with an unknown version" で失敗するため、該当箇所を列挙する。
# (included build (build-logic) では逆にバージョン付き alias(...) のままで良く、id-only だと解決できない)
versioned_requests=""
[ "$convention_dir" = "buildSrc" ] && versioned_requests=$(
  {
    for acc in $mp_accessor $(catalog_aliases_for "$KGP_IDS") $(catalog_aliases_for "$AGP_IDS"); do
      grep -rEns --include='*.gradle.kts' --exclude-dir=build --exclude-dir=.gradle --exclude-dir="$convention_dir" \
        "alias\([[:space:]]*libs\.plugins\.${acc}[[:space:]]*\)" "$project_root" || true
    done
    grep -rEns --include='*.gradle.kts' --exclude-dir=build --exclude-dir=.gradle --exclude-dir="$convention_dir" \
      "(kotlin\(\"(multiplatform|jvm|android)\"\)|id\(\"($KGP_IDS|$AGP_IDS|com\.vanniktech\.maven\.publish)\"\))[[:space:]]+version" "$project_root" || true
  } | sed "s|^$project_root/||" | sort -u
)
if [ -n "$versioned_requests" ]; then
  while IFS= read -r req; do
    add_action "バージョン付きのプラグイン要求を id(libs.plugins.<alias>.get().pluginId) (ルートの apply false 行は削除でも可) に置き換えること: $req"
  done <<EOF
$versioned_requests
EOF
fi

cd_settings="$project_root/$convention_dir/settings.gradle.kts"
if [ -f "$cd_settings" ]; then
  if grep -q 'libs\.versions\.toml' "$cd_settings"; then
    add_skipped "$convention_dir/settings.gradle.kts (version catalog import 済み)"
  else
    add_action "$convention_dir/settings.gradle.kts が既に存在するが version catalog を import していない。example/buildSrc-settings.gradle.kts の dependencyResolutionManagement を手動でマージすること"
  fi
else
  mkdir -p "$project_root/$convention_dir"
  tpl=$(resolve_template buildSrc-settings.gradle.kts)
  cp "$tpl" "$cd_settings"
  add_changed "$convention_dir/settings.gradle.kts"
fi

# buildSrc 以外は included build としてルートの settings に登録されている必要がある
if [ "$convention_dir" != "buildSrc" ]; then
  root_settings="$project_root/settings.gradle.kts"
  if [ -f "$root_settings" ] && grep -Eq "includeBuild\([[:space:]]*\"(\./)?$convention_dir\"[[:space:]]*\)" "$root_settings"; then
    add_skipped "settings.gradle.kts (includeBuild(\"$convention_dir\") 済み)"
  else
    add_action "settings.gradle.kts の pluginManagement { } に includeBuild(\"$convention_dir\") を追加すること (included build の convention plugin を id(\"publish-convention\") で解決するため)"
  fi
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
render "$tpl" "$TMP_DIR/publish-convention.rendered.gradle.kts" \
  "PROJECT_DESCRIPTION=$(kts_escape "$description")" \
  "GITHUB_URL=$(kts_escape "$github_url")" \
  "INCEPTION_YEAR=$(kts_escape "$start_year")" \
  "LICENSE_NAME=$(kts_escape "$license_name")" \
  "LICENSE_URL=$(kts_escape "$license_url")" \
  "DEVELOPER_ID=$(kts_escape "$developer_id")" \
  "DEVELOPER_NAME=$(kts_escape "$developer_name")" \
  "DEVELOPER_URL=$(kts_escape "$developer_url")" \
  "GITHUB_OWNER=$(kts_escape "$github_owner")" \
  "GITHUB_REPO=$(kts_escape "$github_repo")"
install_file "$TMP_DIR/publish-convention.rendered.gradle.kts" \
  "$project_root/$convention_dir/src/main/kotlin/publish-convention.gradle.kts" \
  "$convention_dir/src/main/kotlin/publish-convention.gradle.kts"

# ---------- 4. workflows ----------
# Apple ターゲットがあれば macOS runner (Apple の klib は macOS でしかビルドできない)。無ければ安価な ubuntu。
if grep -rEqs --include='*.gradle.kts' --exclude-dir=build --exclude-dir=.gradle --exclude-dir="$convention_dir" \
  '(ios|macos|tvos|watchos)(Arm64|X64|SimulatorArm64|DeviceArm64)?[[:space:]]*\(' "$project_root"; then
  runner="macos-latest"
else
  runner="ubuntu-latest"
fi
log "GitHub Actions runner: $runner"

tpl=$(resolve_template publish.yml)
render "$tpl" "$TMP_DIR/publish.rendered.yml" "RUNNER=$runner" "READ_VERSION_CMD=$read_version_cmd"
install_file "$TMP_DIR/publish.rendered.yml" "$project_root/.github/workflows/publish.yml" ".github/workflows/publish.yml"

if [ "$skip_publish_check" = 0 ]; then
  tpl=$(resolve_template publish-check.yml)
  render "$tpl" "$TMP_DIR/publish-check.rendered.yml" "RUNNER=$runner"
  install_file "$TMP_DIR/publish-check.rendered.yml" "$project_root/.github/workflows/publish-check.yml" ".github/workflows/publish-check.yml"
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

status="ok"
if [ -n "$ACTION_REQUIRED" ]; then status="action_required"; fi
next_steps='"公開対象モジュールの build.gradle.kts に id(\"publish-convention\") と group/version を追加する","./gradlew publishToMavenLocal で動作確認する (署名はスキップされる)","scripts/setup-secrets.sh で GPG 鍵と GitHub Secrets を設定する"'
printf '{"status":"%s","changed":%s,"skipped":%s,"action_required":%s,"group_id":"%s","description":"%s","github_url":"%s","github_owner":"%s","github_repo":"%s","license_name":"%s","license_url":"%s","developer_id":"%s","developer_name":"%s","developer_url":"%s","inception_year":"%s","maven_publish_version":"%s","maven_publish_accessor":"%s","convention_dir":"%s","version_source":"%s","runner":"%s","next_steps":[%s]}\n' \
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
  "$(json_escape "$mp_accessor")" \
  "$(json_escape "$convention_dir")" \
  "$(json_escape "$version_source")" \
  "$(json_escape "$runner")" \
  "$next_steps"
