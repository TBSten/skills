#!/usr/bin/env bash
# check-android-release.sh — Android の release の APK に、フィードバックモードの痕跡（コード・URL・cleartext の許可・debug だけの権限）が無いことを確かめる。
#
# 使い方: scripts/check-android-release.sh <app-release.apk> [<app-debug.apk>]
#   1. check-no-feedback-in-prod.sh で dex・resources・manifest を文字列 grep
#   2. aapt2 で release の manifest に usesCleartextTraffic="true" / networkSecurityConfig が無いこと
#   3. debug の APK も渡すと、debug にだけある権限（INTERNET など）が release に無いことを比べる（main に元からある権限は両方にあるので出ない）
# 終了コード: 0 = 痕跡なし / 1 = 見つかった / 2 = 使い方の誤り・aapt2 が無い
# aapt2 は $ANDROID_HOME（無ければ ~/Library/Android/sdk）の build-tools の最新を使う。
set -uo pipefail
[ $# -ge 1 ] && [ -f "$1" ] || { echo "usage: $0 <app-release.apk> [<app-debug.apk>]" >&2; exit 2; }
here=$(cd "$(dirname "$0")" && pwd)
sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/Library/Android/sdk}}
aapt2=$(ls -d "$sdk"/build-tools/*/aapt2 2>/dev/null | sort -V | tail -1)
[ -x "$aapt2" ] || { echo "aapt2 が見つかりません（ANDROID_HOME を設定する）: $sdk/build-tools" >&2; exit 2; }
status=0
bash "$here/check-no-feedback-in-prod.sh" "$1" || status=1
manifest=$("$aapt2" dump xmltree --file AndroidManifest.xml "$1") || { echo "aapt2 dump に失敗: $1" >&2; exit 2; }
if echo "$manifest" | grep -qE 'usesCleartextTraffic.*=(true|-1|0xffffffff)'; then echo "FOUND usesCleartextTraffic=true in release manifest"; status=1; fi
if echo "$manifest" | grep -q 'networkSecurityConfig'; then echo "FOUND networkSecurityConfig in release manifest（debug だけに置いたか確かめる）"; status=1; fi
if [ $# -ge 2 ]; then
  perms() { "$aapt2" dump permissions "$1" | grep '^uses-permission' | sed "s/name='[^']*\.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'/DYNAMIC/" | sort -u; }
  only_debug=$(comm -23 <(perms "$2") <(perms "$1"))
  only_release_has=$(comm -12 <(perms "$2") <(perms "$1"))
  echo "debug だけの権限（release に無いのが正しい）:"; echo "${only_debug:-  なし}" | sed 's/^/  /'
  echo "両方にある権限（main 由来。feedback のためだけに足したものが無いか目で見る）:"; echo "${only_release_has:-  なし}" | sed 's/^/  /'
fi
[ $status -eq 0 ] && echo "OK: release の APK に cleartext の許可・networkSecurityConfig は無い"
exit $status
