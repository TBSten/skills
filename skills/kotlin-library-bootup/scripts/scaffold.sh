#!/usr/bin/env bash
#
# scaffold.sh — kotlin-library-bootup skill の example/ (Gradle 一式) と assets/project/ (README /
# CLAUDE.md / .claude / docs 等) を対象ディレクトリへ決定的に展開する。
# コピー / ターゲット別の条件分岐 / プレースホルダー置換 / rename / 置換漏れ検証の SSoT はこの script。
#
# Usage:
#   scaffold.sh --dest <dir> --name <kebab-case> --package <pkg>
#               [--group-id <id>] [--owner <owner>] [--repo <repo>] [--description <text>]
#               [--developer-id <id>] [--developer-name <name>]
#               [--targets standard|full|jvm] [--module-name <kebab-case>]
#               [--kotlin-version <v>]
#               [--skip-integration-test] [--skip-ai-skills] [--skip-docs-site] [--skip-ci]
#               [--dry-run] [--force]
#
# 引数:
#   --dest            生成先ディレクトリ (無ければ作る)。既存ファイルがあれば --force が無い限り止まる
#   --name            プロジェクト名 (kebab-case, 例: my-lib)。rootProject.name / convention plugin id
#                     (<name>.kmp 等) / version catalog キー / artifactId の接頭辞になる
#   --package         ルートパッケージ (例: io.github.me.mylib)
#   --group-id        Maven groupId (既定: --package)
#   --owner / --repo  GitHub の owner / repo (既定: --dest の git remote origin から推定)
#   --description     POM の description (既定: "<name> is a Kotlin Multiplatform library." / jvm は "... a Kotlin library.")
#   --developer-id    POM の developer id (既定: --owner)
#   --developer-name  POM の developer name / LICENSE の著作権者 (既定: --developer-id)
#   --targets         standard (既定): android / jvm / js / wasmJs / iosArm64 / iosSimulatorArm64
#                     full          : standard + macos / watchos / tvos / linux / mingw / androidNative / wasmWasi
#                     jvm           : kotlin("jvm") のみ。Android / Apple 関連 (AGP, CI の android / ios job) は生成しない
#   --module-name     最初のライブラリモジュール名 (既定: <name>-core)。単一モジュールで終わる予定なら <name> も可
#   --kotlin-version  gradle/libs.versions.toml の kotlin を上書きする (既定: example の値)
#   --skip-integration-test  integrationTest/ (独立 Gradle ビルド) と CI の integration-test job を生成しない
#   --skip-ai-skills  assets/project/.claude/skills/ を生成しない
#   --skip-docs-site  assets/project/docs/ と .github/workflows/docs.yml / CI の docs job を生成しない
#                     (Dokka の出力先は docs/public/api-docs ではなく build/api-docs になる)
#   --skip-ci         .github/ を生成しない
#   --dry-run         配置予定の一覧だけを出す (置換・条件分岐・置換漏れ検証は一時ディレクトリで実際に行う)
#   --force           既存ファイルを上書きする
#
# 置換仕様 (長いキー優先の単一パス置換なので、置換結果が再置換されることはない):
#   com.example.kotlinlibrarybootup  -> --package (パス形 com/example/kotlinlibrarybootup も)
#   example-lib-core / example.lib.core -> --module-name (kebab / catalog accessor のドット形)
#   example-lib / example.lib        -> --name (kebab / catalog accessor のドット形)
#   ExampleLib                       -> --name の PascalCase (my-lib -> MyLib)
#   <group-id> <owner> <repo> <year> <description> <developer-id> <developer-name>
#   ファイル / ディレクトリ名にも同じ置換を適用する。
#
# 条件分岐ディレクティブ (example/ と assets/project/ 共通。コメント行として書く: //, #, <!-- -->):
#   @bootup:if <cond>  ...  @bootup:end   cond が偽ならブロックを削除 (入れ子可)。マーカー行自体は常に削除
#   @bootup:file-if <cond>                cond が偽ならファイルごと生成しない (行自体は削除)
#   ブロック内で行頭 (インデント後) が `//~ ` / `#~ ` の行は、ブロックが残る時にコメントを外す
#   (example を standard のままコンパイル可能に保ちつつ、jvm / full 用の行を持つため)
#   cond: カンマ区切りの OR。`!` で否定。
#     standard | full | jvm | kmp (= standard or full) | integration-test | docs-site | ci | ai-skills
#
# ディレクトリ再マッピング:
#   example/**            -> <dest>/** (--targets jvm の時だけ <module>/src/commonMain|commonTest -> src/main|test)
#   assets/project/**     -> <dest>/** (存在すれば。example と同じパスがあればエラー)
#   gradle wrapper は properties のみ同梱 (jar / gradlew は verify.sh --bootstrap-wrapper か `gradle wrapper` で生成)
#
# 出力: 配置ファイル一覧 + 次の手順 + 1 行 JSON {"ok":true,"files":N,"dest":"...",...}

set -euo pipefail

SCRIPT_DIR=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)
SKILL_DIR=$(dirname "$SCRIPT_DIR")
EXAMPLE_DIR="$SKILL_DIR/example"
ASSETS_DIR="$SKILL_DIR/assets/project"

usage() {
    sed -n '/^# scaffold\.sh/,/^# 出力/p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

die() {
    # die "<何が>" "<なぜ>" "<どう直すか>"
    {
        echo "ERROR: $1"
        [ $# -ge 2 ] && echo "  why: $2"
        [ $# -ge 3 ] && echo "  fix: $3"
    } >&2
    exit 1
}

# ---------------------------------------------------------------- 引数パース
DEST="" NAME="" PKG="" GROUP_ID="" OWNER="" REPO="" DESCRIPTION=""
DEV_ID="" DEV_NAME="" TARGETS="standard" MODULE="" KOTLIN_VERSION=""
SKIP_IT=false SKIP_AI=false SKIP_DOCS=false SKIP_CI=false DRY_RUN=false FORCE=false

need_value() {
    [ $# -ge 2 ] || die "オプション $1 に値がない" \
        "$1 は値を取るオプション" "例: $1 <value> の形で渡す"
}

while [ $# -gt 0 ]; do
    case "$1" in
        --dest)                  need_value "$@"; DEST=$2; shift 2 ;;
        --name)                  need_value "$@"; NAME=$2; shift 2 ;;
        --package)               need_value "$@"; PKG=$2; shift 2 ;;
        --group-id)              need_value "$@"; GROUP_ID=$2; shift 2 ;;
        --owner)                 need_value "$@"; OWNER=$2; shift 2 ;;
        --repo)                  need_value "$@"; REPO=$2; shift 2 ;;
        --description)           need_value "$@"; DESCRIPTION=$2; shift 2 ;;
        --developer-id)          need_value "$@"; DEV_ID=$2; shift 2 ;;
        --developer-name)        need_value "$@"; DEV_NAME=$2; shift 2 ;;
        --targets)               need_value "$@"; TARGETS=$2; shift 2 ;;
        --module-name)           need_value "$@"; MODULE=$2; shift 2 ;;
        --kotlin-version)        need_value "$@"; KOTLIN_VERSION=$2; shift 2 ;;
        --skip-integration-test) SKIP_IT=true; shift ;;
        --skip-ai-skills)        SKIP_AI=true; shift ;;
        --skip-docs-site)        SKIP_DOCS=true; shift ;;
        --skip-ci)               SKIP_CI=true; shift ;;
        --dry-run)               DRY_RUN=true; shift ;;
        --force)                 FORCE=true; shift ;;
        -h|--help)               usage; exit 0 ;;
        *) die "不明なオプション: $1" "このオプションは定義されていない" "scaffold.sh --help で使い方を確認する" ;;
    esac
done

# ---------------------------------------------------------------- preflight
[ -n "$DEST" ] || die "--dest がない" "生成先ディレクトリは必須" "--dest <dir> を渡す"
[ -n "$NAME" ] || die "--name がない" "プロジェクト名 (kebab-case) は必須" "--name my-lib のように渡す"
[ -n "$PKG" ]  || die "--package がない" "ルートパッケージは必須" "--package io.github.me.mylib のように渡す"

KEBAB_RE='^[a-z][a-z0-9]*(-[a-z0-9]+)*$'
echo "$NAME" | grep -Eq "$KEBAB_RE" \
    || die "--name '$NAME' が kebab-case でない" \
        "rootProject.name / convention plugin id / version catalog キー / artifactId に使うため 'my-lib' 形式が必要" \
        "小文字英数字とハイフンのみの名前にする (例: my-lib)"
[ -n "$MODULE" ] || MODULE="$NAME-core"
echo "$MODULE" | grep -Eq "$KEBAB_RE" \
    || die "--module-name '$MODULE' が kebab-case でない" \
        "Gradle project 名と artifactId に使う" "小文字英数字とハイフンのみの名前にする (例: my-lib-core)"
echo "$PKG" | grep -Eq '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$' \
    || die "--package '$PKG' がパッケージ名として不正" \
        "Kotlin の package 宣言とディレクトリパスに使う" \
        "小文字ドット区切りで 2 階層以上にする (例: io.github.me.mylib)"
case "$TARGETS" in
    standard|full|jvm) ;;
    *) die "--targets '$TARGETS' が不正" "standard / full / jvm のいずれかで生成内容を切り替える" \
        "--targets standard (既定) / full / jvm のどれかを渡す" ;;
esac
if [ -n "$KOTLIN_VERSION" ]; then
    echo "$KOTLIN_VERSION" | grep -Eq '^[0-9]+\.[0-9]+\.[0-9]+([-.][A-Za-z0-9.-]+)?$' \
        || die "--kotlin-version '$KOTLIN_VERSION' がバージョン形式でない" \
            "gradle/libs.versions.toml の kotlin にそのまま書き込む" "2.4.10 のような形式で渡す"
fi

[ -d "$EXAMPLE_DIR" ] || die "example/ が見つからない: $EXAMPLE_DIR" \
    "skill のコピー元一式が無いと何も生成できない" \
    "skill を丸ごと (example/ を含めて) 取得しているか確認する。prompt 経由なら sparse clone で skills/kotlin-library-bootup 全体を取得する"
command -v perl >/dev/null 2>&1 || die "perl が見つからない" \
    "条件分岐とプレースホルダー置換に perl を使う (macOS / Linux 両対応のため)" "perl をインストールする"

# ---------------------------------------------------------------- 派生値
pascal_of() {
    # kebab-case -> PascalCase (my-lib -> MyLib)
    local out="" part
    local IFS='-'
    for part in $1; do
        out+=$(printf '%s' "${part:0:1}" | tr '[:lower:]' '[:upper:]')${part:1}
    done
    printf '%s' "$out"
}

PASCAL=$(pascal_of "$NAME")
NAME_DOTTED=${NAME//-/.}
MODULE_DOTTED=${MODULE//-/.}
PKG_PATH=${PKG//./\/}
YEAR=$(date +%Y)
[ -n "$GROUP_ID" ] || GROUP_ID=$PKG
echo "$GROUP_ID" | grep -Eq '^[A-Za-z0-9_-]+(\.[A-Za-z0-9_-]+)*$' \
    || die "--group-id '$GROUP_ID' が不正" "Maven groupId として POM と座標に埋め込む" "英数字・ハイフン・アンダースコアのドット区切りにする (例: io.github.me)"

# owner/repo: 未指定なら dest の git remote origin から推定
if [ -z "$OWNER" ] || [ -z "$REPO" ]; then
    remote_url=""
    if [ -d "$DEST" ]; then
        remote_url=$(git -C "$DEST" remote get-url origin 2>/dev/null || true)
    fi
    case "$remote_url" in
        *github.com*)
            rest=${remote_url#*github.com}
            rest=${rest#[:/]}
            rest=${rest%.git}
            guess_owner=${rest%%/*}
            guess_repo=${rest#*/}
            guess_repo=${guess_repo%%/*}
            [ -n "$OWNER" ] || OWNER=$guess_owner
            [ -n "$REPO" ]  || REPO=$guess_repo
            ;;
    esac
fi
if [ -z "$OWNER" ] || [ -z "$REPO" ]; then
    die "GitHub の owner/repo を決定できない" \
        "POM の url/scm・Dokka のソースリンク・LICENSE に <owner>/<repo> を埋め込むが、--owner/--repo が無く、--dest の git remote origin からも推定できなかった" \
        "--owner <owner> --repo <repo> を渡す (または dest を GitHub リポジトリの clone にする)"
fi
[ -n "$DEV_ID" ]   || DEV_ID=$OWNER
[ -n "$DEV_NAME" ] || DEV_NAME=$DEV_ID
if [ -z "$DESCRIPTION" ]; then
    if [ "$TARGETS" = jvm ]; then
        DESCRIPTION="$NAME is a Kotlin library."
    else
        DESCRIPTION="$NAME is a Kotlin Multiplatform library."
    fi
fi
for pair in "description:$DESCRIPTION" "developer-name:$DEV_NAME" "developer-id:$DEV_ID" "owner:$OWNER" "repo:$REPO"; do
    key=${pair%%:*}
    value=${pair#*:}
    case "$value" in
        *\"*|*\\*|*\$*|*$'\n'*)
            die "--$key に使えない文字 (\" \\ \$ 改行) が含まれる: $value" \
                "Kotlin / YAML の文字列リテラルにそのまま埋め込むため" "該当の文字を除いた値を渡す" ;;
    esac
done

# ---------------------------------------------------------------- 条件 (ディレクティブ) の真偽
ATOMS="$TARGETS"
[ "$TARGETS" = jvm ] || ATOMS+=",kmp"
$SKIP_IT   || ATOMS+=",integration-test"
$SKIP_DOCS || ATOMS+=",docs-site"
$SKIP_CI   || ATOMS+=",ci"
$SKIP_AI   || ATOMS+=",ai-skills"
KNOWN_ATOMS="standard,full,jvm,kmp,integration-test,docs-site,ci,ai-skills"

eval_cond() {
    # eval_cond "<cond>" -> exit 0 (真) / 1 (偽)。未知の atom は die
    local cond=$1 term neg atom
    local IFS=','
    for term in $cond; do
        term=$(printf '%s' "$term" | tr -d '[:space:]')
        neg=false
        case "$term" in !*) neg=true; term=${term#!} ;; esac
        case ",$KNOWN_ATOMS," in
            *",$term,"*) ;;
            *) die "未知の条件 '$term' (cond: $cond)" "ディレクティブの条件は決まった atom だけを受け付ける" "atom を $KNOWN_ATOMS のどれかにする" ;;
        esac
        atom=false
        case ",$ATOMS," in *",$term,"*) atom=true ;; esac
        if $neg; then
            if $atom; then atom=false; else atom=true; fi
        fi
        $atom && return 0
    done
    return 1
}

# ---------------------------------------------------------------- 変換
export B_ATOMS=$ATOMS B_KNOWN=$KNOWN_ATOMS
export K_PKG=$PKG K_PKG_PATH=$PKG_PATH K_NAME=$NAME K_NAME_DOTTED=$NAME_DOTTED \
    K_MODULE=$MODULE K_MODULE_DOTTED=$MODULE_DOTTED K_PASCAL=$PASCAL \
    K_GROUP_ID=$GROUP_ID K_OWNER=$OWNER K_REPO=$REPO K_YEAR=$YEAR \
    K_DESCRIPTION=$DESCRIPTION K_DEV_ID=$DEV_ID K_DEV_NAME=$DEV_NAME

# @bootup:if / :end / :file-if を解釈する。入れ子可。未閉じ・未知 atom は非 0 終了
transform_directives() {
    perl -e '
        my %on = map { $_ => 1 } split /,/, $ENV{B_ATOMS};
        my %known = map { $_ => 1 } split /,/, $ENV{B_KNOWN};
        my $file = $ARGV[0];
        sub cond_true {
            my ($c) = @_;
            for my $t (split /\s*,\s*/, $c) {
                $t =~ s/^\s+|\s+$//g;
                my $neg = ($t =~ s/^!//);
                die "$file: unknown condition atom \"$t\"\n" unless $known{$t};
                my $v = $on{$t} ? 1 : 0;
                $v = !$v if $neg;
                return 1 if $v;
            }
            return 0;
        }
        my $mark = qr{^\s*(?://|\#|<!--)\s*\@bootup:};
        my @stack;
        my $n = 0;
        open my $in, "<", $file or die "$file: $!\n";
        while (my $line = <$in>) {
            $n++;
            if ($line =~ m{${mark}file-if\s+(.+?)\s*(?:-->)?\s*$}) { next; }
            if ($line =~ m{${mark}if\s+(.+?)\s*(?:-->)?\s*$}) { push @stack, [cond_true($1), $n]; next; }
            if ($line =~ m{${mark}end\b}) {
                die "$file:$n: \@bootup:end without \@bootup:if\n" unless @stack;
                pop @stack;
                next;
            }
            die "$file:$n: unknown \@bootup directive\n" if $line =~ m{$mark};
            next if grep { !$_->[0] } @stack;
            $line =~ s{^(\s*)(?://|\#)~ ?}{$1} if @stack;
            print $line;
        }
        die "$file:$stack[-1][1]: unclosed \@bootup:if\n" if @stack;
    ' "$1"
}

# 全置換を長いキー優先の単一パスで行う。s///g は置換結果を再走査しないので、
# 置換後の文字列に別のキーが含まれていても壊れない。
transform_full() {
    perl -0777 -pe '
        BEGIN {
            %m = (
                "com.example.kotlinlibrarybootup" => $ENV{K_PKG},
                "com/example/kotlinlibrarybootup" => $ENV{K_PKG_PATH},
                "example-lib-core"                => $ENV{K_MODULE},
                "example.lib.core"                => $ENV{K_MODULE_DOTTED},
                "example-lib"                     => $ENV{K_NAME},
                "example.lib"                     => $ENV{K_NAME_DOTTED},
                "ExampleLib"                      => $ENV{K_PASCAL},
                "<group-id>"                      => $ENV{K_GROUP_ID},
                "<owner>"                         => $ENV{K_OWNER},
                "<repo>"                          => $ENV{K_REPO},
                "<year>"                          => $ENV{K_YEAR},
                "<description>"                   => $ENV{K_DESCRIPTION},
                "<developer-id>"                  => $ENV{K_DEV_ID},
                "<developer-name>"                => $ENV{K_DEV_NAME},
            );
            $re = join "|", map { quotemeta } sort { length($b) <=> length($a) } keys %m;
        }
        s/($re)/$m{$1}/g;
    '
}

transform_path() {
    local p
    p=$(printf '%s' "$1" | transform_full)
    if [ "$TARGETS" = jvm ]; then
        # KMP の source set 名を JVM の標準レイアウトへ
        p=$(printf '%s' "$p" | perl -pe 's{/src/commonMain/}{/src/main/}; s{/src/commonTest/}{/src/test/}')
    fi
    printf '%s' "$p"
}

apply_versions_toml() {
    if [ -n "$KOTLIN_VERSION" ]; then
        K_V=$KOTLIN_VERSION perl -pe 's/^kotlin = "[^"]*"/kotlin = "$ENV{K_V}"/'
    else
        cat
    fi
}

is_binary() {
    case "$1" in
        *.png|*.jpg|*.jpeg|*.gif|*.ico|*.webp|*.avif|*.woff|*.woff2|*.ttf|*.otf|*.jar|*.zip|*.pdf) return 0 ;;
        *) return 1 ;;
    esac
}

# ---------------------------------------------------------------- 配置計画
PLAN_SRC=()
PLAN_DST=()

file_condition_ok() {
    # @bootup:file-if <cond> があれば評価する (無ければ常に真)
    local cond
    is_binary "$1" && return 0
    cond=$(perl -ne 'if (m{^\s*(?://|\#|<!--)\s*\@bootup:file-if\s+(.+?)\s*(?:-->)?\s*$}) { print $1; exit }' "$1")
    [ -z "$cond" ] && return 0
    eval_cond "$cond"
}

add_plan() {
    local src=$1 dstrel=$2 i
    for i in "${!PLAN_DST[@]}"; do
        if [ "${PLAN_DST[$i]}" = "$dstrel" ]; then
            die "配置先が重複している: $dstrel" \
                "${PLAN_SRC[$i]#"$SKILL_DIR"/} と ${src#"$SKILL_DIR"/} が同じパスに展開される" \
                "skill 側 (example/ と assets/project/) のどちらかのファイルを削除・改名する"
        fi
    done
    PLAN_SRC+=("$src")
    PLAN_DST+=("$dstrel")
}

while IFS= read -r src; do
    rel=${src#"$EXAMPLE_DIR"/}
    case "$rel" in
        .github/*)        $SKIP_CI && continue ;;
        integrationTest/*) $SKIP_IT && continue ;;
    esac
    file_condition_ok "$src" || continue
    add_plan "$src" "$(transform_path "$rel")"
done < <(find "$EXAMPLE_DIR" -type f ! -name '.DS_Store' | LC_ALL=C sort)

ASSETS_FOUND=false
if [ -d "$ASSETS_DIR" ]; then
    ASSETS_FOUND=true
    while IFS= read -r src; do
        rel=${src#"$ASSETS_DIR"/}
        case "$rel" in
            .claude/skills/*) $SKIP_AI && continue ;;
            docs/*)           $SKIP_DOCS && continue ;;
            .github/*)        $SKIP_CI && continue ;;
            integrationTest/*) $SKIP_IT && continue ;;
        esac
        # 依存物・生成物は持ち込まない
        case "/$rel" in
            */node_modules/*|*/dist/*|*/.astro/*|*/build/*|*/.gradle/*) continue ;;
        esac
        file_condition_ok "$src" || continue
        add_plan "$src" "$(transform_path "$rel")"
    done < <(find "$ASSETS_DIR" -type f ! -name '.DS_Store' | LC_ALL=C sort)
fi

TOTAL=${#PLAN_SRC[@]}
[ "$TOTAL" -gt 0 ] || die "配置対象が 0 件" "example/ が空か、skip オプションで全て除外された" "example/ の中身と --skip-* の組み合わせを確認する"

# ---------------------------------------------------------------- 冪等性チェック
conflicts=()
for i in $(seq 0 $((TOTAL - 1))); do
    [ -e "$DEST/${PLAN_DST[$i]}" ] && conflicts+=("${PLAN_DST[$i]}")
done

if [ ${#conflicts[@]} -gt 0 ] && ! $FORCE && ! $DRY_RUN; then
    {
        echo "ERROR: 生成先に既存ファイルが ${#conflicts[@]} 件ある (上書きしない)"
        echo "  why: 冪等性のため、明示しない限り既存ファイルを壊さない"
        echo "  fix: 内容を確認して --force で上書きするか、別の --dest を使う (--dry-run で [exists] を確認できる)"
        echo "  conflicts:"
        n=0
        for c in "${conflicts[@]}"; do
            echo "    - $c"
            n=$((n + 1))
            if [ "$n" -ge 20 ]; then
                echo "    ... ほか $((${#conflicts[@]} - 20)) 件"
                break
            fi
        done
    } >&2
    exit 1
fi

# ---------------------------------------------------------------- 一時ディレクトリへ生成 (dry-run でも行う)
STAGE=$(mktemp -d "${TMPDIR:-/tmp}/kotlin-library-bootup.XXXXXX")
trap 'rm -rf "$STAGE"' EXIT

for i in $(seq 0 $((TOTAL - 1))); do
    src=${PLAN_SRC[$i]}
    dstrel=${PLAN_DST[$i]}
    out="$STAGE/$dstrel"
    mkdir -p "$(dirname "$out")"
    if is_binary "$src"; then
        cp "$src" "$out"
        continue
    fi
    if ! transform_directives "$src" > "$out.directives" 2> "$STAGE/.directive-error"; then
        die "条件分岐ディレクティブの解釈に失敗: ${src#"$SKILL_DIR"/}" \
            "$(sed "s|$SKILL_DIR/||" "$STAGE/.directive-error")" \
            "@bootup:if と @bootup:end の対応・条件の atom を直す (TBSten/skills に issue 報告する)"
    fi
    transform_full < "$out.directives" > "$out"
    rm -f "$out.directives"
    if [ "$dstrel" = "gradle/libs.versions.toml" ]; then
        apply_versions_toml < "$out" > "$out.tmp" && mv "$out.tmp" "$out"
    fi
    [ -x "$src" ] && chmod +x "$out"
done
rm -f "$STAGE/.directive-error"

# ---------------------------------------------------------------- 置換漏れ検証
# ユーザー指定値そのものに含まれる文字列はチェック対象から外す (誤検知防止)
USER_VALUES="$PKG|$PKG_PATH|$NAME|$MODULE|$NAME_DOTTED|$MODULE_DOTTED|$PASCAL|$GROUP_ID|$OWNER|$REPO|$DESCRIPTION|$DEV_ID|$DEV_NAME"
LEFT_PATTERNS=()
add_left_pattern() {
    # add_left_pattern <literal> <regex>
    case "$USER_VALUES" in *"$1"*) return 0 ;; esac
    LEFT_PATTERNS+=("$2")
}
add_left_pattern "kotlinlibrarybootup" 'kotlinlibrarybootup'
add_left_pattern "example-lib" 'example-lib'
add_left_pattern "example.lib" 'example\.lib'
add_left_pattern "ExampleLib" 'ExampleLib'
for token in group-id owner repo year description developer-id developer-name; do
    add_left_pattern "<$token>" "<$token>"
done
LEFT_PATTERNS+=('@bootup:' '^[[:space:]]*(//|#)~')
LEFT_RE=$(IFS='|'; printf '%s' "${LEFT_PATTERNS[*]}")

LEFTOVER=""
for i in $(seq 0 $((TOTAL - 1))); do
    is_binary "${PLAN_SRC[$i]}" && continue
    hits=$(grep -nE "$LEFT_RE" "$STAGE/${PLAN_DST[$i]}" 2>/dev/null || true)
    [ -n "$hits" ] && LEFTOVER+=$(printf '%s\n' "$hits" | sed "s|^|  ${PLAN_DST[$i]}:|")$'\n'
done
if [ -n "$LEFTOVER" ]; then
    printf 'ERROR: 置換漏れ (プレースホルダー / ディレクティブの残り) を検出した:\n%s' "$LEFTOVER" >&2
    die "scaffold 結果に未置換のプレースホルダーが残っている (生成先には何も書き込んでいない)" \
        "置換ルールかディレクティブが example/ / assets/project/ を網羅できていない" \
        "上記ファイルの元テンプレートを直す (TBSten/skills に issue 報告する)"
fi

# ---------------------------------------------------------------- dry-run
resolve_dest() {
    if [ -d "$DEST" ]; then (cd "$DEST" && pwd); else printf '%s' "$DEST"; fi
}

SUMMARY_JSON="\"targets\":\"$TARGETS\",\"name\":\"$NAME\",\"module\":\"$MODULE\",\"package\":\"$PKG\",\"groupId\":\"$GROUP_ID\",\"owner\":\"$OWNER\",\"repo\":\"$REPO\",\"assets\":$ASSETS_FOUND"

if $DRY_RUN; then
    echo "## DRY RUN: 配置予定 ($TOTAL files, targets=$TARGETS) — 書き込みは行わない"
    for i in $(seq 0 $((TOTAL - 1))); do
        marker=""
        [ -e "$DEST/${PLAN_DST[$i]}" ] && marker="  [exists]"
        echo "  ${PLAN_DST[$i]}  <=  ${PLAN_SRC[$i]#"$SKILL_DIR"/}$marker"
    done
    $ASSETS_FOUND || echo "NOTE: assets/project/ が無いので README / CLAUDE.md / docs 等は生成されない"
    [ ${#conflicts[@]} -gt 0 ] && echo "NOTE: [exists] の ${#conflicts[@]} 件は実行時に --force が必要"
    echo "{\"ok\":true,\"dryRun\":true,\"files\":$TOTAL,\"dest\":\"$(resolve_dest)\",$SUMMARY_JSON}"
    exit 0
fi

# ---------------------------------------------------------------- 書き込み
mkdir -p "$DEST"
DEST_ABS=$(resolve_dest)
for i in $(seq 0 $((TOTAL - 1))); do
    dst="$DEST_ABS/${PLAN_DST[$i]}"
    mkdir -p "$(dirname "$dst")"
    cp -p "$STAGE/${PLAN_DST[$i]}" "$dst"
done

# ---------------------------------------------------------------- 結果出力
echo "## 配置ファイル ($TOTAL files, targets=$TARGETS) -> $DEST_ABS"
for i in $(seq 0 $((TOTAL - 1))); do
    echo "  ${PLAN_DST[$i]}"
done
$SKIP_CI   && echo "  (skip: .github/)"
$SKIP_IT   && echo "  (skip: integrationTest/)"
$SKIP_DOCS && echo "  (skip: docs/ と docs.yml)"
$SKIP_AI   && echo "  (skip: .claude/skills/)"
$ASSETS_FOUND || echo "  (assets/project/ が無いので README / CLAUDE.md / docs 等は未生成)"
cat <<EOF
## 次の手順
  1. gradle wrapper を用意する (jar / gradlew は同梱しない):
       bash $SCRIPT_DIR/verify.sh --project-dir $DEST_ABS --bootstrap-wrapper --only wrapper
     (または Gradle がある環境で: gradle wrapper --gradle-version <gradle/wrapper/gradle-wrapper.properties の版>)
  2. ビルド確認 (初回は API dump を作る): bash $SCRIPT_DIR/verify.sh --project-dir $DEST_ABS --fresh
  3. 生成された api/ ディレクトリ (binary-compatibility-validator の dump) をコミットする
EOF
echo "{\"ok\":true,\"files\":$TOTAL,\"dest\":\"$DEST_ABS\",$SUMMARY_JSON}"
