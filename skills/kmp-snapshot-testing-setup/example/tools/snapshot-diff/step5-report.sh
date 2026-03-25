#!/usr/bin/env bash
# Step 5: Gradle タスク snapshotReport を呼び出してレポートを生成
#
# 概要:
#   snapshotReport Gradle タスクに必要なプロパティを渡して
#   result.json と result.md を生成する。
#
# Input (環境変数):
#   SNAPSHOT_DST       — スナップショットディレクトリ (build/snapshots/)
#   BASELINE_MANIFEST  — baseline ファイル一覧のパス (NUL 区切りテンポラリファイル)
#   BEFORE             — ベースラインの ref
#   AFTER              — 比較対象の ref (空文字 = "current working tree")
#   PBT_ITERATION      — PBT イテレーション数 (空文字の場合あり)
#   PREFIX             — ログ出力のプレフィックス
#
# Output:
#   build/snapshots/result.json
#   build/snapshots/result.md
#
set -euo pipefail

AFTER_LABEL="${AFTER:-current working tree}"
echo ""
echo "==="
echo "=== [5/5] Generating report ==="
echo "==="

GRADLE_ARGS=(
  snapshotReport
  "-PsnapshotReport.dir=$SNAPSHOT_DST"
  "-PsnapshotReport.baselineManifest=$BASELINE_MANIFEST"
  "-PsnapshotReport.before=$BEFORE"
  "-PsnapshotReport.after=$AFTER_LABEL"
  --console=rich
)
if [ -n "${PBT_ITERATION:-}" ]; then
  GRADLE_ARGS+=("-PsnapshotReport.pbtIteration=$PBT_ITERATION")
fi

./gradlew "${GRADLE_ARGS[@]}" 2>&1 | sed "s/^/${PREFIX}/"
