#!/usr/bin/env bash
# check-no-feedback-in-prod.sh — 本番ビルドの成果物に feedback-mode のコード・UI・送信先・依存が残っていないことを確かめる。【追加】参照実装には無い
#
# 使い方: scripts/check-no-feedback-in-prod.sh <本番ビルドの成果物> [...]
#   成果物: ディレクトリ（dist/・build/・.next/・Electron の out/ や app.asar を展開したもの）、.apk / .aab / .ipa / .app / .asar / .zip
#   例: Web `npm run build && scripts/check-no-feedback-in-prod.sh dist`
#       Electron `scripts/check-no-feedback-in-prod.sh "out/MyApp-darwin-arm64/MyApp.app/Contents/Resources"`（app.asar は npx asar extract で展開してから）
#       Android `./gradlew assembleRelease && scripts/check-no-feedback-in-prod.sh app/build/outputs/apk/release/app-release.apk`
#       iOS（Release の .app / .ipa）、Flutter（build/app/outputs/…、build/ios/…）、React Native（release の JS バンドルを含む apk / ipa）
# 終了コード: 0 = 見つからない / 1 = 見つかった（成果物と文字列を表示）/ 2 = 使い方の誤り
# 限界: 文字列の grep なので、難読化・暗号化・圧縮（asar・jar 内の圧縮は unzip で展開する）されたものは見逃す。
#       権限（スクショ・ネットワーク）は別に確認する（下のコメント）。パターンは templates の識別子・文字列に合わせている。自分のアプリで足す。
# -e は付けない: grep の「見つからない」(終了コード 1) を正常系として扱うため。失敗は各所で明示的に判定する
set -uo pipefail
PATTERNS=(
  'feedback-mode'            # feedback-mode.js / ホットキーのキー名 / localStorage のキー（feedback-mode:options）
  'fb-overlay' 'fb-balloon'  # UI のクラス名
  '__feedback'               # 受け口の URL パス（/__feedback）
  'x-feedback-token'         # 受け口の鍵のヘッダ名
  'FEEDBACK_ENDPOINT' 'FEEDBACK_TOKEN' 'FEEDBACK_HOST'  # 開発用のビルド変数名・値が焼き込まれていないか
  'app:feedback-save'        # Electron の IPC チャネル
  'feedbackApi'              # preload が公開する名前
  'html2canvas'              # スクショの依存
  'FeedbackRoot' 'FbTarget' 'FeedbackProvider'  # RN / Flutter のウィジェット名（難読化されると見逃す）
  'installFeedbackMode' 'getRecentLogs'
  '10\.0\.2\.2:7373'         # Android エミュレータから開発機への URL
  ':7373'                    # 受け口の既定のポート
)
[ $# -ge 1 ] || { echo "usage: $0 <build output> [...]" >&2; exit 2; }
work=$(mktemp -d); trap 'rm -rf "$work"' EXIT
status=0
for target in "$@"; do
  [ -e "$target" ] || { echo "not found: $target" >&2; exit 2; }
  dir="$target"
  case "$target" in
    *.apk|*.aab|*.ipa|*.zip|*.jar|*.war) dir="$work/$(basename "$target")"; mkdir -p "$dir"; unzip -qq -o "$target" -d "$dir" || { echo "unzip failed: $target" >&2; exit 2; } ;;
  esac
  for p in "${PATTERNS[@]}"; do
    # -a: バイナリも文字列として。-I は使わない（.dex・.so・Mach-O・Flutter の libapp.so も見る）。node_modules 等の除外はしない（成果物の中身なので）
    hits=$(grep -r -a -l -E -- "$p" "$dir" 2>/dev/null | head -5)
    if [ -n "$hits" ]; then status=1; echo "FOUND /$p/ in $target"; echo "$hits" | sed 's/^/    /'; fi
  done
done
[ $status -eq 0 ] && echo "OK: feedback-mode の痕跡は見つかりませんでした（文字列の grep のみ。権限・依存の宣言は別に確認）"
# 権限・依存の確認（手で）:
#   Android: `aapt2 dump permissions app-release.apk` / `apkanalyzer manifest print` に、feedback のためだけの権限（INTERNET を debug だけで足している場合は release に無いこと）、
#            usesCleartextTraffic="true"（http の開発機に送るための設定）が release に無いこと。network_security_config の開発機ドメインも debug source set だけ。
#   iOS:     Release の Info.plist に NSAppTransportSecurity の例外（開発機への http）・NSLocalNetworkUsageDescription・NSBonjourServices が無いこと。
#   Web/Electron: package.json の dependencies に html2canvas 等が無く devDependencies にあること（`npm ls --omit=dev html2canvas` が空）。CSP の connect-src に開発機が無いこと。
exit $status
