#!/usr/bin/env bash
# kmp-snapshot-testing-setup install script.
# KMP + Compose プロジェクトにスナップショットテスト基盤一式を配置する:
#   (1) gradle/libs.versions.toml へ versions / libraries を冪等追記
#   (2) build-logic へ convention plugin 3 ファイルをコピー + 置換
#   (3) テスト基盤モジュール (build.gradle.kts + Kotlin ソース) を配置 + settings include 追記
#   (4) Kotlin ソースのパッケージ置換 (com.example.snapshot -> --package)
#   (5) tools/ (snapshot-diff.sh 一式) をコピー + chmod +x
#   (6) ルート build.gradle.kts へ cleanSnapshotOutputDir タスクを冪等追記
# AI agents: run this script as-is. Do not read, modify, or reimplement it.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SKILL_DIR="$(cd "${SCRIPT_DIR}/.." && pwd)"
EXAMPLE_DIR="${SKILL_DIR}/example"
SRC_PACKAGE="com.example.snapshot"
SRC_MODULE_GRADLE_PATH=":core:testing:snapshot"
COMPOSE_ONLY_CORE_FILES="testing/snapshot/assertion/ImageSnapshotAssertion.kt testing/snapshot/assertion/SemanticsSnapshotAssertion.kt testing/snapshot/assertion/SemanticsLayoutRenderer.kt"

usage() {
  cat >&2 <<'EOF'
Usage: install.sh --project <dir> --package <pkg> [options]

Required:
  --project <dir>          対象プロジェクトのルートディレクトリ
  --package <pkg>          テスト基盤の Kotlin パッケージ (com.example.snapshot を置換)

Options:
  --module-path <path>     テスト基盤モジュールのパス (default: core/testing/snapshot)
  --ui-module-path <path>  Compose テストモジュールのパス (default: ui/core/testing)
  --skip-compose           Compose 関連 (ui モジュール + Compose 向け assertion + Compose 依存) を省く
  --dry-run                書き込みせずに実行内容を表示する
  --force                  既存ファイルを上書きする (無指定時は conflict として skip)
  -h, --help               このヘルプを表示

出力: 進捗は stderr、最終行に結果 JSON 1 行を stdout に出力する。
EOF
}

die() { # die <what> <why> <fix>
  printf 'ERROR: %s\nWHY: %s\nFIX: %s\n' "$1" "$2" "$3" >&2
  exit 1
}

info() { echo "$*" >&2; }

# --- Parse arguments ---------------------------------------------------------
PROJECT=""
PACKAGE=""
MODULE_PATH="core/testing/snapshot"
UI_MODULE_PATH="ui/core/testing"
SKIP_COMPOSE=false
DRY_RUN=false
FORCE=false

while [ $# -gt 0 ]; do
  case "$1" in
    --project)
      [ $# -ge 2 ] || { usage; die "--project requires a value" "no value was given" "pass e.g. --project /path/to/project"; }
      PROJECT="$2"; shift 2 ;;
    --package)
      [ $# -ge 2 ] || { usage; die "--package requires a value" "no value was given" "pass e.g. --package com.myapp"; }
      PACKAGE="$2"; shift 2 ;;
    --module-path)
      [ $# -ge 2 ] || { usage; die "--module-path requires a value" "no value was given" "pass e.g. --module-path core/testing/snapshot"; }
      MODULE_PATH="$2"; shift 2 ;;
    --ui-module-path)
      [ $# -ge 2 ] || { usage; die "--ui-module-path requires a value" "no value was given" "pass e.g. --ui-module-path ui/core/testing"; }
      UI_MODULE_PATH="$2"; shift 2 ;;
    --skip-compose) SKIP_COMPOSE=true; shift ;;
    --dry-run) DRY_RUN=true; shift ;;
    --force) FORCE=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *)
      usage
      die "unknown argument: $1" "this script only accepts the options shown above" "remove '$1' and re-run" ;;
  esac
done

# --- Preflight ---------------------------------------------------------------
[ -n "$PROJECT" ] || { usage; die "--project is required" "the target project cannot be guessed" "pass e.g. --project /path/to/project"; }
[ -n "$PACKAGE" ] || { usage; die "--package is required" "the target package cannot be guessed" "pass e.g. --package com.myapp"; }

[ -d "$PROJECT" ] || die "project directory not found: $PROJECT" "the path does not exist or is not a directory" "check the path passed to --project"
PROJECT="$(cd "$PROJECT" && pwd)"

if ! printf '%s' "$PACKAGE" | grep -Eq '^[A-Za-z_][A-Za-z0-9_]*(\.[A-Za-z_][A-Za-z0-9_]*)*$'; then
  die "invalid package name: $PACKAGE" "it is not a valid Kotlin package (dot-separated identifiers)" "pass e.g. --package com.myapp"
fi

validate_module_path() { # validate_module_path <label> <path>
  case "$2" in
    /*) die "$1 must be a relative path: $2" "module paths are resolved relative to --project" "pass e.g. --$1 core/testing/snapshot" ;;
    *..*) die "$1 must not contain '..': $2" "path traversal is not allowed" "pass a plain relative path" ;;
    "") die "$1 must not be empty" "an empty module path cannot be resolved" "pass e.g. --$1 core/testing/snapshot" ;;
  esac
}
MODULE_PATH="${MODULE_PATH%/}"
UI_MODULE_PATH="${UI_MODULE_PATH%/}"
validate_module_path "module-path" "$MODULE_PATH"
validate_module_path "ui-module-path" "$UI_MODULE_PATH"

SETTINGS_FILE=""
if [ -f "$PROJECT/settings.gradle.kts" ]; then SETTINGS_FILE="$PROJECT/settings.gradle.kts";
elif [ -f "$PROJECT/settings.gradle" ]; then SETTINGS_FILE="$PROJECT/settings.gradle";
else die "settings.gradle(.kts) not found in $PROJECT" "--project must point at the Gradle project root" "pass the directory that contains settings.gradle.kts"; fi

TOML="$PROJECT/gradle/libs.versions.toml"
[ -f "$TOML" ] || die "gradle/libs.versions.toml not found: $TOML" "this skill records dependencies in the version catalog" "create gradle/libs.versions.toml (empty [versions]/[libraries] sections are fine) and re-run"

BUILD_LOGIC_SRC=""
if [ -d "$PROJECT/build-logic" ]; then BUILD_LOGIC_SRC="$PROJECT/build-logic/src/main/kotlin";
elif [ -d "$PROJECT/buildSrc" ]; then BUILD_LOGIC_SRC="$PROJECT/buildSrc/src/main/kotlin";
else die "neither build-logic/ nor buildSrc/ found in $PROJECT" "the convention plugins need a precompiled-script-plugin build" "set up a build-logic included build (or buildSrc) first, then re-run"; fi

command -v python3 >/dev/null 2>&1 || die "python3 not found" "python3 is needed for portable TOML editing and text replacement" "install python3 and re-run"

for f in convention-kmp-test.gradle.kts convention-kmp-snapshot-testing.gradle.kts SnapshotReportTask.kt; do
  [ -f "$EXAMPLE_DIR/build-logic/$f" ] || die "example file not found: $EXAMPLE_DIR/build-logic/$f" "the skill installation is incomplete" "re-install the skill (gh skill install tbsten/skills kmp-snapshot-testing-setup)"
done
for f in core-testing-snapshot/build.gradle.kts ui-core-testing/build.gradle.kts tools/snapshot-diff.sh; do
  [ -f "$EXAMPLE_DIR/$f" ] || die "example file not found: $EXAMPLE_DIR/$f" "the skill installation is incomplete" "re-install the skill (gh skill install tbsten/skills kmp-snapshot-testing-setup)"
done

# --- Computed values ---------------------------------------------------------
PKG_PATH="$(printf '%s' "$PACKAGE" | tr '.' '/')"
MODULE_GRADLE_PATH=":$(printf '%s' "$MODULE_PATH" | tr '/' ':')"
UI_MODULE_GRADLE_PATH=":$(printf '%s' "$UI_MODULE_PATH" | tr '/' ':')"

TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/kmp-snapshot-install-XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

INSTALLED_COUNT=0
SKIPPED_COUNT=0
CONFLICTS=()
WARNINGS=()
ADDED_VERSIONS=()
ADDED_LIBRARIES=()
CATALOG_SKIPPED=()
INCLUDES_ADDED=()
FOLLOWUPS=()
ROOT_BUILD_UPDATED=false

warn() { echo "WARN: $*" >&2; WARNINGS+=("$*"); }

# --- Helpers -----------------------------------------------------------------
replace_fixed() { # replace_fixed <file> <from> <to>  (fixed-string, in-place)
  RF_FROM="$2" RF_TO="$3" python3 - "$1" <<'PY'
import os, sys
path = sys.argv[1]
with open(path, encoding="utf-8") as fp:
    text = fp.read()
with open(path, "w", encoding="utf-8") as fp:
    fp.write(text.replace(os.environ["RF_FROM"], os.environ["RF_TO"]))
PY
}

strip_compose_blocks() { # strip_compose_blocks <file>  (marker 行含めて削除)
  python3 - "$1" <<'PY'
import sys
path = sys.argv[1]
out, skipping = [], False
with open(path, encoding="utf-8") as fp:
    for line in fp:
        if "compose-deps:start" in line:
            skipping = True
            continue
        if "compose-deps:end" in line:
            skipping = False
            continue
        if not skipping:
            out.append(line)
with open(path, "w", encoding="utf-8") as fp:
    fp.writelines(out)
PY
}

append_with_newline() { # append_with_newline <file> <text>  (末尾改行を保証して追記)
  AW_TEXT="$2" python3 - "$1" <<'PY'
import os, sys
path = sys.argv[1]
with open(path, encoding="utf-8") as fp:
    text = fp.read()
if text and not text.endswith("\n"):
    text += "\n"
text += os.environ["AW_TEXT"] + "\n"
with open(path, "w", encoding="utf-8") as fp:
    fp.write(text)
PY
}

install_file() { # install_file <rendered-src> <dest>
  local src="$1" dest="$2" rel="${2#"$PROJECT"/}"
  if [ -e "$dest" ]; then
    if cmp -s "$src" "$dest"; then
      SKIPPED_COUNT=$((SKIPPED_COUNT + 1))
      info "Up-to-date: $rel"
    elif [ "$FORCE" = true ]; then
      if [ "$DRY_RUN" = true ]; then
        info "DRY-RUN: would overwrite $rel"
      else
        cp "$src" "$dest"
        info "Overwrote: $rel"
      fi
      INSTALLED_COUNT=$((INSTALLED_COUNT + 1))
    else
      CONFLICTS+=("$rel")
      info "CONFLICT (kept existing, use --force to overwrite): $rel"
    fi
  else
    if [ "$DRY_RUN" = true ]; then
      info "DRY-RUN: would install $rel"
    else
      mkdir -p "$(dirname "$dest")"
      cp "$src" "$dest"
      info "Installed: $rel"
    fi
    INSTALLED_COUNT=$((INSTALLED_COUNT + 1))
  fi
}

add_settings_include() { # add_settings_include <gradle-path>
  local gpath="$1"
  if grep -Eq "[\"']${gpath}[\"']" "$SETTINGS_FILE"; then
    info "Include exists: $gpath"
    return
  fi
  if [ "$DRY_RUN" = true ]; then
    info "DRY-RUN: would append include(\"$gpath\") to ${SETTINGS_FILE#"$PROJECT"/}"
  else
    append_with_newline "$SETTINGS_FILE" "include(\"$gpath\")"
    info "Appended include(\"$gpath\") to ${SETTINGS_FILE#"$PROJECT"/}"
  fi
  INCLUDES_ADDED+=("$gpath")
}

# --- Step 1: version catalog -------------------------------------------------
info "== Step 1: gradle/libs.versions.toml =="
CATALOG_OUT="$TMP_DIR/catalog.out"
TOML_PATH="$TOML" DRY="$([ "$DRY_RUN" = true ] && echo 1 || echo 0)" python3 - >"$CATALOG_OUT" <<'PY'
import os, re

path = os.environ["TOML_PATH"]
dry = os.environ["DRY"] == "1"
with open(path, encoding="utf-8") as fp:
    text = fp.read()
lines = text.split("\n")

def section_span(name):
    start = None
    for i, ln in enumerate(lines):
        s = ln.strip()
        if s == f"[{name}]":
            start = i
            continue
        if start is not None and re.match(r"^\[[^\]]+\]$", s):
            return (start, i)
    return (start, len(lines)) if start is not None else None

def key_exists(name, key):
    span = section_span(name)
    if span is None:
        return False
    pat = re.compile(r"^\s*" + re.escape(key) + r"\s*=")
    return any(pat.match(lines[i]) for i in range(span[0] + 1, span[1]))

inserts = {}  # section -> [line, ...]

def ensure(section, key, line, kind, module=None):
    if key_exists(section, key):
        print(f"{kind}_EXISTS {key}")
        return
    if module is not None and (f'"{module}"' in text or f"'{module}'" in text):
        print(f"L_SKIP_MODULE {key} {module}")
        return
    inserts.setdefault(section, []).append(line)
    print(f"{kind}_ADDED {key}")

ensure("versions", "kotest", 'kotest = "6.0.0.M1"  # or latest', "V")
ensure("versions", "turbine", 'turbine = "1.2.0"  # or latest', "V")
ensure("versions", "kotlinx-serialization",
       'kotlinx-serialization = "1.7.3"  # TODO: プロジェクトの kotlinx.serialization version に合わせる', "V")
if not key_exists("versions", "kotlinx-coroutines"):
    inserts.setdefault("versions", []).append(
        'kotlinx-coroutines = "1.10.2"  # TODO: プロジェクトの coroutines version に合わせる')
    print("V_ADDED kotlinx-coroutines")
    print("WARN [versions] に kotlinx-coroutines が未定義だったため 1.10.2 を追記しました。"
          "プロジェクトの coroutines version に合わせて修正してください")
else:
    print("V_EXISTS kotlinx-coroutines")

libraries = [
    ("kotestFrameworkEngine", "io.kotest:kotest-framework-engine", "kotest"),
    ("kotestAssertionsCore", "io.kotest:kotest-assertions-core", "kotest"),
    ("kotestRunnerJunit5", "io.kotest:kotest-runner-junit5", "kotest"),
    ("kotestProperty", "io.kotest:kotest-property", "kotest"),
    ("kotestExtensionsHtmlReporter", "io.kotest:kotest-extensions-htmlreporter", "kotest"),
    ("kotestExtensionsJunitXml", "io.kotest:kotest-extensions-junitxml", "kotest"),
    ("turbine", "app.cash.turbine:turbine", "turbine"),
    ("kotlinx-coroutines-test", "org.jetbrains.kotlinx:kotlinx-coroutines-test", "kotlinx-coroutines"),
    ("kotlinxSerializationCore", "org.jetbrains.kotlinx:kotlinx-serialization-core", "kotlinx-serialization"),
]
for alias, module, ref in libraries:
    ensure("libraries", alias,
           f'{alias} = {{ module = "{module}", version.ref = "{ref}" }}', "L", module=module)

if inserts and not dry:
    # 下から順に挿入して行番号のズレを防ぐ。無いセクションは末尾に新設する。
    spans = {}
    missing = []
    for section in inserts:
        span = section_span(section)
        if span is None:
            missing.append(section)
        else:
            spans[section] = span
    for section, span in sorted(spans.items(), key=lambda kv: kv[1][0], reverse=True):
        # セクション末尾の空行の手前に挿入する
        insert_at = span[1]
        while insert_at > span[0] + 1 and lines[insert_at - 1].strip() == "":
            insert_at -= 1
        lines[insert_at:insert_at] = inserts[section]
    for section in ("versions", "libraries"):
        if section in missing:
            if lines and lines[-1].strip() != "":
                lines.append("")
            lines.append(f"[{section}]")
            lines.extend(inserts[section])
    out = "\n".join(lines)
    if not out.endswith("\n"):
        out += "\n"
    with open(path, "w", encoding="utf-8") as fp:
        fp.write(out)
PY
while IFS= read -r line; do
  case "$line" in
    V_ADDED\ *)  ADDED_VERSIONS+=("${line#V_ADDED }"); info "Catalog: added version ${line#V_ADDED }" ;;
    V_EXISTS\ *) info "Catalog: version exists ${line#V_EXISTS }" ;;
    L_ADDED\ *)  ADDED_LIBRARIES+=("${line#L_ADDED }"); info "Catalog: added library ${line#L_ADDED }" ;;
    L_EXISTS\ *) info "Catalog: library exists ${line#L_EXISTS }" ;;
    L_SKIP_MODULE\ *)
      rest="${line#L_SKIP_MODULE }"
      alias_name="${rest%% *}"
      module_name="${rest#* }"
      CATALOG_SKIPPED+=("$alias_name")
      warn "catalog: $module_name は別 alias で既に宣言済みのため $alias_name を追加しませんでした。テンプレートは libs.$alias_name を参照するので、モジュール build.gradle.kts を既存 alias に合わせて修正してください" ;;
    WARN\ *) warn "${line#WARN }" ;;
  esac
done <"$CATALOG_OUT"
if [ "$DRY_RUN" = true ] && { [ "${#ADDED_VERSIONS[@]}" -gt 0 ] || [ "${#ADDED_LIBRARIES[@]}" -gt 0 ]; }; then
  info "DRY-RUN: catalog entries above would be appended (not written)"
fi

# --- Step 2: build-logic convention plugins ---------------------------------
info "== Step 2: build-logic convention plugins ($BUILD_LOGIC_SRC) =="
for f in convention-kmp-test.gradle.kts SnapshotReportTask.kt; do
  cp "$EXAMPLE_DIR/build-logic/$f" "$TMP_DIR/$f"
  install_file "$TMP_DIR/$f" "$BUILD_LOGIC_SRC/$f"
done
cp "$EXAMPLE_DIR/build-logic/convention-kmp-snapshot-testing.gradle.kts" "$TMP_DIR/convention-kmp-snapshot-testing.gradle.kts"
replace_fixed "$TMP_DIR/convention-kmp-snapshot-testing.gradle.kts" "<your-package>" "$PACKAGE"
replace_fixed "$TMP_DIR/convention-kmp-snapshot-testing.gradle.kts" "$SRC_MODULE_GRADLE_PATH" "$MODULE_GRADLE_PATH"
install_file "$TMP_DIR/convention-kmp-snapshot-testing.gradle.kts" "$BUILD_LOGIC_SRC/convention-kmp-snapshot-testing.gradle.kts"

# --- Step 3: core testing module ---------------------------------------------
info "== Step 3: core testing module ($MODULE_PATH) =="
cp "$EXAMPLE_DIR/core-testing-snapshot/build.gradle.kts" "$TMP_DIR/core-build.gradle.kts"
if [ "$SKIP_COMPOSE" = true ]; then
  strip_compose_blocks "$TMP_DIR/core-build.gradle.kts"
fi
install_file "$TMP_DIR/core-build.gradle.kts" "$PROJECT/$MODULE_PATH/build.gradle.kts"

CORE_SRC_ROOT="$PROJECT/$MODULE_PATH/src/jvmMain/kotlin/$PKG_PATH"
CORE_KT_LIST="$TMP_DIR/core-kt-list.txt"
(cd "$EXAMPLE_DIR/core-testing-snapshot" && find . -name '*.kt' | sed 's|^\./||' | sort) >"$CORE_KT_LIST"
while IFS= read -r rel; do
  if [ "$SKIP_COMPOSE" = true ]; then
    skip=false
    for cf in $COMPOSE_ONLY_CORE_FILES; do
      [ "$rel" = "$cf" ] && skip=true
    done
    if [ "$skip" = true ]; then
      info "Skip (compose-only): $rel"
      continue
    fi
  fi
  mkdir -p "$TMP_DIR/render/$(dirname "$rel")"
  cp "$EXAMPLE_DIR/core-testing-snapshot/$rel" "$TMP_DIR/render/$rel"
  replace_fixed "$TMP_DIR/render/$rel" "$SRC_PACKAGE" "$PACKAGE"
  install_file "$TMP_DIR/render/$rel" "$CORE_SRC_ROOT/$rel"
done <"$CORE_KT_LIST"
add_settings_include "$MODULE_GRADLE_PATH"

# --- Step 4: Compose testing module ------------------------------------------
if [ "$SKIP_COMPOSE" = true ]; then
  info "== Step 4: Compose testing module: skipped (--skip-compose) =="
else
  info "== Step 4: Compose testing module ($UI_MODULE_PATH) =="
  cp "$EXAMPLE_DIR/ui-core-testing/build.gradle.kts" "$TMP_DIR/ui-build.gradle.kts"
  replace_fixed "$TMP_DIR/ui-build.gradle.kts" "$SRC_MODULE_GRADLE_PATH" "$MODULE_GRADLE_PATH"
  install_file "$TMP_DIR/ui-build.gradle.kts" "$PROJECT/$UI_MODULE_PATH/build.gradle.kts"

  UI_SRC_ROOT="$PROJECT/$UI_MODULE_PATH/src/jvmMain/kotlin/$PKG_PATH/ui/testing"
  for f in ComposeSnapshot.kt ComposeSnapshotPbtSpec.kt; do
    cp "$EXAMPLE_DIR/ui-core-testing/$f" "$TMP_DIR/$f"
    replace_fixed "$TMP_DIR/$f" "$SRC_PACKAGE" "$PACKAGE"
    install_file "$TMP_DIR/$f" "$UI_SRC_ROOT/$f"
  done
  add_settings_include "$UI_MODULE_GRADLE_PATH"
fi

# --- Step 5: shell scripts (tools/) ------------------------------------------
info "== Step 5: tools/ (snapshot-diff scripts) =="
TOOLS_LIST="$TMP_DIR/tools-list.txt"
(cd "$EXAMPLE_DIR/tools" && find . -type f | sed 's|^\./||' | sort) >"$TOOLS_LIST"
while IFS= read -r rel; do
  install_file "$EXAMPLE_DIR/tools/$rel" "$PROJECT/tools/$rel"
done <"$TOOLS_LIST"
if [ "$DRY_RUN" != true ]; then
  find "$PROJECT/tools" -name '*.sh' -exec chmod +x {} + 2>/dev/null || true
fi

# --- Step 6: root build.gradle.kts -------------------------------------------
info "== Step 6: root build.gradle.kts (cleanSnapshotOutputDir) =="
ROOT_BUILD="$PROJECT/build.gradle.kts"
CLEAN_TASK_BLOCK='
// kmp-snapshot-testing-setup: jvmSnapshotTestRecord が依存する snapshot 出力削除タスク
tasks.register<Delete>("cleanSnapshotOutputDir") {
    group = "verification"
    description = "Deletes build/snapshots directory"
    delete(layout.projectDirectory.dir("build/snapshots"))
}'
if [ -f "$ROOT_BUILD" ]; then
  if grep -q 'cleanSnapshotOutputDir' "$ROOT_BUILD"; then
    info "cleanSnapshotOutputDir already registered in build.gradle.kts"
  elif [ "$DRY_RUN" = true ]; then
    info "DRY-RUN: would append cleanSnapshotOutputDir task to build.gradle.kts"
    ROOT_BUILD_UPDATED=true
  else
    append_with_newline "$ROOT_BUILD" "$CLEAN_TASK_BLOCK"
    info "Appended cleanSnapshotOutputDir task to build.gradle.kts"
    ROOT_BUILD_UPDATED=true
  fi
elif [ -f "$PROJECT/build.gradle" ]; then
  warn "root build.gradle は Groovy DSL のため cleanSnapshotOutputDir タスクを自動追記しませんでした。手動で登録してください"
  FOLLOWUPS+=("root build.gradle (Groovy) に cleanSnapshotOutputDir (Delete タスク, build/snapshots 削除) を手動で登録する")
else
  warn "root build.gradle.kts が見つからないため cleanSnapshotOutputDir タスクを追記しませんでした"
  FOLLOWUPS+=("root build.gradle.kts を作成し cleanSnapshotOutputDir (Delete タスク, build/snapshots 削除) を登録する")
fi

# --- Post-check: no source package left --------------------------------------
if [ "$DRY_RUN" != true ]; then
  LEFTOVER=""
  for d in "$PROJECT/$MODULE_PATH" "$PROJECT/$UI_MODULE_PATH" "$BUILD_LOGIC_SRC/convention-kmp-snapshot-testing.gradle.kts"; do
    [ -e "$d" ] || continue
    if grep -rF "$SRC_PACKAGE" "$d" >/dev/null 2>&1; then
      LEFTOVER="$LEFTOVER $d"
    fi
  done
  if [ -n "$LEFTOVER" ]; then
    if [ "${#CONFLICTS[@]}" -gt 0 ]; then
      warn "置換前パッケージ $SRC_PACKAGE が残っています:$LEFTOVER (conflict で古いファイルが残っている可能性。--force で再実行を検討)"
    else
      die "package replacement left '$SRC_PACKAGE' in:$LEFTOVER" "the in-place replacement did not complete" "delete the copied files and re-run (report this as a skill bug if it persists)"
    fi
  fi
fi

# --- Manual follow-ups (AI の責務) -------------------------------------------
FOLLOWUPS+=("build-logic の build classpath に kotlin serialization plugin (org.jetbrains.kotlin:kotlin-serialization) を追加する (convention-kmp-snapshot-testing が適用するため)")
FOLLOWUPS+=("convention plugin コメント内の 'convention-kmp' はプロジェクトの KMP 共通 convention に読み替える (無ければモジュール build.gradle.kts が KMP 設定を自前で行うのでそのままで可)")
FOLLOWUPS+=("convention plugin の libs.findLibrary が解決できない場合は build-logic に VersionCatalog アクセサを用意する")
if [ "$SKIP_COMPOSE" != true ]; then
  FOLLOWUPS+=("ComposeSnapshot.kt の WithTestGraph / AppTheme 参照と $UI_MODULE_PATH/build.gradle.kts の TODO 依存をプロジェクトの theme / DI に合わせて調整する")
fi
FOLLOWUPS+=("スナップショットテストを書くモジュールの build.gradle.kts に id(\"convention-kmp-snapshot-testing\") を適用する")
FOLLOWUPS+=("./gradlew compileKotlinJvm でビルド確認する")

# --- Result JSON (1 line, stdout) --------------------------------------------
join_lines() { local IFS=$'\n'; printf '%s' "${*-}"; }
J_CONFLICTS="$(join_lines ${CONFLICTS[@]+"${CONFLICTS[@]}"})"
J_WARNINGS="$(join_lines ${WARNINGS[@]+"${WARNINGS[@]}"})"
J_ADDED_VERSIONS="$(join_lines ${ADDED_VERSIONS[@]+"${ADDED_VERSIONS[@]}"})"
J_ADDED_LIBRARIES="$(join_lines ${ADDED_LIBRARIES[@]+"${ADDED_LIBRARIES[@]}"})"
J_CATALOG_SKIPPED="$(join_lines ${CATALOG_SKIPPED[@]+"${CATALOG_SKIPPED[@]}"})"
J_INCLUDES_ADDED="$(join_lines ${INCLUDES_ADDED[@]+"${INCLUDES_ADDED[@]}"})"
J_FOLLOWUPS="$(join_lines ${FOLLOWUPS[@]+"${FOLLOWUPS[@]}"})"

OK_FLAG=true
[ "${#CONFLICTS[@]}" -gt 0 ] && OK_FLAG=false

RESULT_OK="$OK_FLAG" RESULT_DRY="$DRY_RUN" RESULT_SKIP_COMPOSE="$SKIP_COMPOSE" \
RESULT_PROJECT="$PROJECT" RESULT_PACKAGE="$PACKAGE" \
RESULT_MODULE_PATH="$MODULE_PATH" RESULT_UI_MODULE_PATH="$UI_MODULE_PATH" \
RESULT_BUILD_LOGIC="$BUILD_LOGIC_SRC" \
RESULT_INSTALLED="$INSTALLED_COUNT" RESULT_SKIPPED="$SKIPPED_COUNT" \
RESULT_ROOT_BUILD_UPDATED="$ROOT_BUILD_UPDATED" \
J_CONFLICTS="$J_CONFLICTS" J_WARNINGS="$J_WARNINGS" \
J_ADDED_VERSIONS="$J_ADDED_VERSIONS" J_ADDED_LIBRARIES="$J_ADDED_LIBRARIES" \
J_CATALOG_SKIPPED="$J_CATALOG_SKIPPED" J_INCLUDES_ADDED="$J_INCLUDES_ADDED" \
J_FOLLOWUPS="$J_FOLLOWUPS" \
python3 - <<'PY'
import json, os

def lines(name):
    raw = os.environ.get(name, "")
    return [x for x in raw.split("\n") if x != ""]

result = {
    "ok": os.environ["RESULT_OK"] == "true",
    "skill": "kmp-snapshot-testing-setup",
    "dry_run": os.environ["RESULT_DRY"] == "true",
    "skip_compose": os.environ["RESULT_SKIP_COMPOSE"] == "true",
    "project": os.environ["RESULT_PROJECT"],
    "package": os.environ["RESULT_PACKAGE"],
    "module_path": os.environ["RESULT_MODULE_PATH"],
    "ui_module_path": os.environ["RESULT_UI_MODULE_PATH"],
    "build_logic_dir": os.environ["RESULT_BUILD_LOGIC"],
    "installed": int(os.environ["RESULT_INSTALLED"]),
    "skipped_identical": int(os.environ["RESULT_SKIPPED"]),
    "conflicts": lines("J_CONFLICTS"),
    "catalog": {
        "added_versions": lines("J_ADDED_VERSIONS"),
        "added_libraries": lines("J_ADDED_LIBRARIES"),
        "skipped_aliases": lines("J_CATALOG_SKIPPED"),
    },
    "settings_includes_added": lines("J_INCLUDES_ADDED"),
    "root_build_updated": os.environ["RESULT_ROOT_BUILD_UPDATED"] == "true",
    "warnings": lines("J_WARNINGS"),
    "manual_followups": lines("J_FOLLOWUPS"),
}
print(json.dumps(result, ensure_ascii=False))
PY
