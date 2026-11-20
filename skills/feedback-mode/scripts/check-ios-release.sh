#!/usr/bin/env bash
# check-ios-release.sh — iOS の Release の .app / .ipa に、フィードバックモード（assets/templates/adapters/mobile/ios/）の痕跡が無いことを確かめる。
# 使い方: bash check-ios-release.sh [--kmp] <Release の .app か .ipa> [追加の文字列（部品の名前など）...]
#   --kmp: Compose Multiplatform の framework を含むアプリ。sharedUI の FeedbackHooks（no-op の fbTarget）は Release にも残る設計なので fbTarget を見ない
#   例: xcodebuild -scheme MyApp -configuration Release -sdk iphonesimulator -derivedDataPath build CODE_SIGNING_ALLOWED=NO build
#       bash check-ios-release.sh build/Build/Products/Release-iphonesimulator/MyApp.app "ホーム/保存ボタン"
# 中身: (1) check-no-feedback-in-prod.sh（共通のパターン） (2) Info.plist に送信先・ATS の例外・ローカルネットワークのキーが無いこと
#       (3) 実行ファイルに iOS ひな形の型名・modifier 名・受け口のパスが無いこと（シンボル表も含む。大文字小文字を区別しない）
# 終了コード: 0 = 見つからない / 1 = 見つかった / 2 = 使い方の誤り
set -uo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
[ $# -ge 1 ] || { echo "usage: $0 <Release .app | .ipa> [extra-pattern ...]" >&2; exit 2; }
kmp=0; if [ "${1:-}" = "--kmp" ]; then kmp=1; shift; fi
[ $# -ge 1 ] || { echo "usage: $0 [--kmp] <Release .app | .ipa> [extra-pattern ...]" >&2; exit 2; }
target="$1"; shift
[ -e "$target" ] || { echo "not found: $target" >&2; exit 2; }
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
app="$target"
if [[ "$target" == *.ipa ]]; then
  unzip -qq -o "$target" -d "$work" || { echo "unzip failed: $target" >&2; exit 2; }
  app=$(find "$work/Payload" -maxdepth 1 -name '*.app' | head -1)
  [ -n "$app" ] || { echo "Payload/*.app が見つかりません: $target" >&2; exit 2; }
fi
status=0

bash "$HERE/check-no-feedback-in-prod.sh" "$app" || status=1

plist="$app/Info.plist"
for key in FeedbackEndpoint FeedbackToken NSAppTransportSecurity NSLocalNetworkUsageDescription NSBonjourServices; do
  if /usr/libexec/PlistBuddy -c "Print :$key" "$plist" >/dev/null 2>&1; then echo "FOUND Info.plist key: $key"; status=1; fi
done

PATTERNS=('FeedbackMode' 'FeedbackOverlay' 'FeedbackShot' 'FeedbackTransport' 'FeedbackEntryButton' 'FbRegistry' 'FbTargetModifier' '__feedback' 'FEEDBACK_ENDPOINT' "$@")
# KMP は sharedUI の FeedbackHooks（no-op の Modifier.fbTarget）が Release にも残る設計なので、SwiftUI の modifier 名は見ない
[ $kmp -eq 1 ] || PATTERNS+=('fbTarget' 'fbPath')
machos=0
# LC_ALL=C: バイナリの中の UTF-8（日本語の部品名）も 1 バイトずつ比べる
while IFS= read -r -d '' f; do
  file "$f" | grep -q 'Mach-O' || continue
  machos=$((machos+1))
  for p in "${PATTERNS[@]}"; do
    if LC_ALL=C grep -a -i -q -F -- "$p" "$f"; then echo "FOUND /$p/ in $f"; status=1; fi
  done
done < <(find "$app" -type f -print0)
[ $machos -gt 0 ] || { echo "error: $app に実行ファイル（Mach-O）がありません。ビルドが失敗していないか確かめる" >&2; exit 2; }

[ $status -eq 0 ] && echo "OK: iOS の Release にフィードバックモードの痕跡は見つかりませんでした"
exit $status
