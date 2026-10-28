#!/usr/bin/env bash
# detect.sh — その環境に「フィードバック置き場・Sink 設定・feedback mode の配線」があるかを調べ、実践（loop）と構築（build）のどちらに進むかを提案する。
# 使い方: bash scripts/detect.sh [リポジトリのルート（既定: カレント）]
# 読み取りだけ（何も変更しない）。出力の末尾の VERDICT= を SKILL.md の判定表に当てはめる。
# -e は付けない: grep の「見つからない」(終了コード 1) を正常系として扱うため。失敗は各所で明示的に判定する
set -uo pipefail
ROOT="${1:-.}"
cd "$ROOT" || { echo "no such dir: $ROOT" >&2; exit 2; }
EXCL=(--exclude-dir=node_modules --exclude-dir=.git --exclude-dir=dist --exclude-dir=build --exclude-dir=out --exclude-dir=.next --exclude-dir=Pods --exclude-dir=.dart_tool --exclude-dir=.gradle --exclude-dir=tmp --exclude-dir=.local)
REC='^[0-9]{6}-[0-9]{2,}\.md$'

echo "== 1. フィードバック置き場（Inbox）の候補: yymmdd-xx.md の形の記録がある・archived/ がある ディレクトリ"
inbox_found=0
for d in feedback feedbacks .feedback docs/feedback .local/feedback "${FEEDBACK_DIR:-}"; do
  [ -n "$d" ] && [ -d "$d" ] || continue
  open=$(ls "$d" 2>/dev/null | grep -Ec "$REC"); arch=$(ls "$d/archived" 2>/dev/null | grep -Ec "$REC")
  zero=$(find "$d" -maxdepth 1 -type f -name '*.md' -size 0 2>/dev/null | wc -l | tr -d ' ')
  hasarch=no; [ -d "$d/archived" ] && hasarch=yes
  echo "  $d: 未対応 $open 件 / 対応済み $arch 件 / 0 バイト $zero 件 / archived/ $hasarch"
  if [ "$open" -gt 0 ] || [ "$arch" -gt 0 ] || [ "$hasarch" = yes ]; then inbox_found=1; fi
done
[ $inbox_found -eq 0 ] && echo "  （見つからない）"

echo "== 2. Sink・置き場の設定の手がかり（.env*・設定ファイル・docs の feedback.dir / FEEDBACK_*）"
sink_hits=$( { grep -rIlE 'FEEDBACK_(ENDPOINT|DIR|TOKEN|HOST|REPO_ROOT)|feedback\.dir|feedback:\s*$' "${EXCL[@]}" --include='.env*' --include='*.yaml' --include='*.yml' --include='*.json' --include='*.toml' --include='*.md' . 2>/dev/null | head -8; } )
[ -n "$sink_hits" ] && echo "$sink_hits" | sed 's/^/  /' || echo "  （見つからない）"

echo "== 3. feedback mode の配線（画面側・受け口側のコード）"
wiring=0
for p in 'installFeedbackMode' 'registerFeedback' 'feedback-core' 'FeedbackProvider' 'FeedbackRoot' 'FbTarget' 'app:feedback' '__feedback' 'data-fb'; do
  n=$(grep -rIlE -e "$p" "${EXCL[@]}" --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' --include='*.mjs' --include='*.vue' --include='*.svelte' --include='*.dart' --include='*.swift' --include='*.kt' --include='*.rs' --include='*.html' . 2>/dev/null | wc -l | tr -d ' ')
  [ "$n" -gt 0 ] && { echo "  $p: $n ファイル"; wiring=$((wiring + n)); }
done
[ $wiring -eq 0 ] && echo "  （見つからない）"

echo "== 4. 実践用の skill（feedback-loop 相当）"
loopskill=0
for d in .claude/skills/feedback-loop .claude/skills/feedback-mode "$HOME/.claude/skills/feedback-loop" "$HOME/.claude/skills/feedback-mode"; do
  [ -d "$d" ] && { echo "  $d"; loopskill=1; }
done
[ $loopskill -eq 0 ] && echo "  （見つからない）"

echo "== 5. 既存のデバッグ基盤（情報。判定は変えない。build 手順 1b で使う）"
dbg=$(grep -rIlE -e 'DebugMenu|debugMenu|debug menu|DevMenu|DevSettings|dev ?panel|DebugDrawer|Hyperion|FLEX|devtools|shakeToDebug|onShake' "${EXCL[@]}" --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' --include='*.mjs' --include='*.vue' --include='*.dart' --include='*.swift' --include='*.kt' --include='*.java' --include='*.rs' . 2>/dev/null | head -8)
[ -n "$dbg" ] && echo "$dbg" | sed 's/^/  /' || echo "  （見つからない）"
echo "  ビルド種別の判定（__DEV__ / kDebugMode / BuildConfig.DEBUG / #if DEBUG / import.meta.env.DEV / isPackaged）を使うファイル数:"
for p in '__DEV__' 'kDebugMode' 'BuildConfig\.DEBUG' '#if DEBUG' 'import\.meta\.env\.DEV' 'isPackaged' 'debug_assertions'; do
  n=$(grep -rIlE -e "$p" "${EXCL[@]}" --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' --include='*.mjs' --include='*.dart' --include='*.swift' --include='*.kt' --include='*.java' --include='*.rs' . 2>/dev/null | wc -l | tr -d ' ')
  [ "$n" -gt 0 ] && echo "    $p: $n"
done

echo "== 6. 既存のログ基盤（情報。判定は変えない。build 手順 1c で使う）"
lg=$(grep -rIlE -e 'electron-log|winston|pino|loglevel|Timber|CocoaLumberjack|OSLog|os_log|Sentry\.|addBreadcrumb|LogBuffer|ring ?buffer|Logger\.root|createLogger|new Logger|logger\.(info|warn|error|debug)\(|\blog\.(info|warn|error|debug)\(' "${EXCL[@]}" --include='*.ts' --include='*.tsx' --include='*.js' --include='*.jsx' --include='*.mjs' --include='*.dart' --include='*.swift' --include='*.kt' --include='*.java' --include='*.rs' . 2>/dev/null | head -8)
[ -n "$lg" ] && echo "$lg" | sed 's/^/  /' || echo "  （見つからない）"

echo "== 判定"
if [ $inbox_found -eq 1 ] && [ $wiring -gt 0 ]; then v=loop; r="置き場も配線もある"
elif [ $inbox_found -eq 1 ]; then v=loop; r="置き場はあるが配線は見つからない（別アプリ・手書きメモ・他の取り込み元）。構築は頼まれたときだけ"
elif [ $wiring -gt 0 ]; then v=partial; r="配線はあるが置き場・記録が無い（部分的）。動作確認を build の手順 10〜12 で行ってから loop"
else v=build; r="置き場も配線も見つからない"; fi
echo "VERDICT=$v  ($r)"
[ $loopskill -eq 1 ] && [ "$v" != build ] && echo "NOTE: 既存の feedback-loop skill があるなら、それを正として使い、loop/ は差分の確認に使う"
echo "NOTE: ユーザーの明示（「作って」= build / 「対応して」= loop / 両方）が判定より優先"
