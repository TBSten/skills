#!/usr/bin/env bash
# schedule 実行を約 2 週に 1 回へ間引く gate。ISO 週番号が偶数の週だけ run=true を出す。
# schedule 以外 (workflow_dispatch 等) は常に run=true。
#
# usage: week-gate.sh <event_name> [date (YYYY-MM-DD、既定: 今日 UTC)]
# stdout: run=true|false (GITHUB_OUTPUT にそのまま追記できる形式)
set -euo pipefail

event="${1:?usage: week-gate.sh <event_name> [YYYY-MM-DD]}"
day="${2:-$(date -u +%F)}"

if date -u -d "$day" +%V >/dev/null 2>&1; then
  week="$(date -u -d "$day" +%V)" # GNU date (ubuntu runner)
else
  week="$(date -u -j -f %F "$day" +%V)" # BSD date (macOS でのローカル確認用)
fi

if [ "$event" != "schedule" ]; then
  echo "week-gate: event=$event なので gate しない" >&2
  echo "run=true"
elif [ $((10#$week % 2)) -eq 0 ]; then
  echo "week-gate: ISO week $week (偶数) なので実行する" >&2
  echo "run=true"
else
  echo "week-gate: ISO week $week (奇数) なのでスキップする" >&2
  echo "run=false"
fi
