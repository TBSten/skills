# Review の選択肢

popup の「直さない」「直してもらう」「解決済みにする」の隣に並ぶボタン。1 つ = 見出し 1 つ。
押すと指摘を解決済みにして、note の頭に見出しの文字を書く。agent は `- agent:` に書いたとおりに扱う。
（置き場は <Inbox>/review/_actions.md。形は references/review/review.md §6）

## 後で
- agent: 次のバージョンのチケットに積んでから remove する
- decision: 後で

## このまま
- agent: 意図どおりの設計なので確かめずに remove する
- decision: このまま

## PR #65 待ち
- agent: PR #65 のマージ後に見直し、箇所が消えていれば remove、残っていれば reopen

ここは見出しの直後ではないのでメモとして読み飛ばされる。
