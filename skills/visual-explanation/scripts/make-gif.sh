#!/usr/bin/env bash
# 連番の PNG (capture.mjs で撮ったコマ) を 1 本の GIF にする。
# 画面収録できない環境で、実際のログを 1 コマずつ HTML に描いて動きを見せるときに使う。
#
# 使い方:
#   bash make-gif.sh [--delay=800] [--width=840] <out.gif> <frame.png>...
#
#   --delay  1 コマの表示時間 (ms、既定 800)。最後のコマは 3 倍表示する
#   --width  GIF の幅 (px、既定 840)。コマの大きさが違っても、この幅に合わせて下を余白で揃える
#
# 出力: 1 行目に OK、続けて GIF のパスとサイズ。
set -euo pipefail

fail() { echo "ERROR: $*" >&2; exit 1; }

delay=800
width=840
args=()
for a in "$@"; do
  case "$a" in
    --delay=*) delay="${a#--delay=}" ;;
    --width=*) width="${a#--width=}" ;;
    --*) fail "不明なオプション: $a (使えるのは --delay= / --width=)" ;;
    *) args+=("$a") ;;
  esac
done
[ "${#args[@]}" -ge 3 ] || fail "出力先と 2 枚以上のコマを渡す。使い方: bash make-gif.sh [--delay=800] [--width=840] <out.gif> <frame.png>..."
case "$delay" in ''|*[!0-9]*) fail "--delay は正の整数 (ms)" ;; esac
case "$width" in ''|*[!0-9]*) fail "--width は正の整数 (px)" ;; esac
command -v ffmpeg >/dev/null 2>&1 || fail "ffmpeg が無い。brew install ffmpeg / apt-get install ffmpeg で入れてから再実行する"
command -v ffprobe >/dev/null 2>&1 || fail "ffprobe が無い。ffmpeg と一緒に入れる"

out="${args[0]}"
frames=("${args[@]:1}")
case "$out" in *.gif) ;; *) fail "出力先は .gif にする: $out" ;; esac
for f in "${frames[@]}"; do [ -f "$f" ] || fail "コマが見つからない: $f"; done

# 幅を揃えたときの最大の高さを求める (全コマを同じキャンバスに置くため)
height=0
for f in "${frames[@]}"; do
  dims="$(ffprobe -v error -select_streams v:0 -show_entries stream=width,height -of csv=p=0:s=x "$f")" \
    || fail "画像として読めない: $f"
  w="${dims%x*}"; h="${dims#*x}"
  sh=$(( (h * width + w - 1) / w ))
  if [ "$sh" -gt "$height" ]; then height="$sh"; fi
done
height=$(( (height + 1) / 2 * 2 ))

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
list="$work/list.txt"
sec="$(awk -v d="$delay" 'BEGIN { printf "%.3f", d / 1000 }')"
last="$(awk -v d="$delay" 'BEGIN { printf "%.3f", d * 3 / 1000 }')"
n=${#frames[@]}
# コマの大きさが違うと ffmpeg のフィルタが途中で作り直されてコマが落ちるので、先に同じキャンバスへ揃える
i=0
for f in "${frames[@]}"; do
  i=$((i + 1))
  norm="$work/$(printf '%04d' "$i").png"
  ffmpeg -v error -y -i "$f" -vf "scale=${width}:-2:flags=lanczos,pad=${width}:${height}:0:0:color=white" "$norm" \
    || fail "コマを揃えられなかった: $f"
  printf "file '%s'\n" "$norm" >> "$list"
  if [ "$i" -eq "$n" ]; then printf 'duration %s\n' "$last" >> "$list"; else printf 'duration %s\n' "$sec" >> "$list"; fi
done
# concat demuxer は最後のコマの duration を無視するので、もう一度並べる
printf "file '%s'\n" "$norm" >> "$list"

filter="split[a][b];[a]palettegen=stats_mode=diff[p];[b][p]paletteuse=dither=bayer:bayer_scale=4"
ffmpeg -v error -y -f concat -safe 0 -i "$list" -vf "$filter" -fps_mode vfr -loop 0 "$out" \
  || fail "ffmpeg で GIF を作れなかった。上のエラーを見て、コマの PNG が壊れていないか確かめる"

bytes="$(wc -c < "$out" | tr -d ' ')"
echo "OK"
echo "gif: $out"
echo "size: $((bytes / 1024)) KB (${width}x${height}, ${n} frames)"
if [ "$bytes" -gt 512000 ]; then
  echo "WARN: 500 KB を超えている。--width を小さくするか、コマを減らす" >&2
fi
