#!/bin/sh
# 本番ビルド（dist）に、dev 専用のフィードバックモードの痕跡が残っていないことを確かめる。【実績: Web 参照実装の検査を汎用化】
# 使い方: sh check-no-dev-tools-in-dist.sh <dist のフォルダ> [足すパターン ...]
#   CI の build の後に 1 ステップ足す（例: `- run: sh <dev 専用の場所>/check-no-dev-tools-in-dist.sh dist`）。
# 終了コード: 0 = 見つからない / 1 = 見つかった（ファイルを出す）/ 2 = 使い方の誤り・dist が空（ビルドの失敗を見逃さない）
#
# PATTERNS は protocol.ts の定数と、dev のときだけ付ける属性・依存の名前。定数を変えたらここも直す
# （ずれを防ぐなら、定数とこの一覧を突き合わせるテストを 1 つ置く。Web 参照実装はそうしている）。
#   feedback-mode   APP_ID（toolbar アプリの id・イベント名の接頭辞・UI の要素名）
#   __feedbackLog   LOG_GLOBAL（head-inline のログのバッファ）
#   data-fb-src     Markdown の要素に dev のときだけ付ける行番号の属性（使う場合。references/build/locate.md）
set -eu

PATTERNS='feedback-mode
__feedbackLog
data-fb-src'

if [ "$#" -lt 1 ]; then
	echo "usage: $0 <dist dir> [extra pattern ...]" >&2
	exit 2
fi
dist="$1"
shift
for extra in "$@"; do
	PATTERNS="$PATTERNS
$extra"
done
if [ ! -d "$dist" ]; then
	echo "check-no-dev-tools-in-dist: $dist is not a directory" >&2
	exit 2
fi
# 空の dist は何も見つからずに通ってしまう。html が 1 つはあることを求める。
if [ -z "$(find "$dist" -name '*.html' -print | head -n 1)" ]; then
	echo "check-no-dev-tools-in-dist: $dist has no .html file (was the build run?)" >&2
	exit 2
fi

found=0
# 改行で区切って 1 つずつ見る（パイプにすると found が子シェルに閉じ込められる）。
OLDIFS="$IFS"
IFS='
'
for pat in $PATTERNS; do
	if grep -raFq -- "$pat" "$dist"; then
		echo "FOUND '$pat' in:" >&2
		grep -raFl -- "$pat" "$dist" | head -5 | sed 's/^/  /' >&2
		found=1
	fi
done
IFS="$OLDIFS"
if [ "$found" -ne 0 ]; then
	echo "check-no-dev-tools-in-dist: dev-only code leaked into $dist" >&2
	exit 1
fi
echo "check-no-dev-tools-in-dist: ok ($dist)"
