---
tags: [skill-eval]
max_turns: 15
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Bash]
---

さっき直した `CartViewModel` の合計金額計算のバグ、確認は ViewModel の unit test でやりました。
`CartRepository` は MockK で mock してて、テストケースは「商品 2 個の通常ケース」「クーポン適用」「在庫切れでエラー」の 3 つ。
assertEquals で期待値を比べてて、GitHub Actions の CI で PR ごとに自動で回ってます。

この確認方法の検証レベルを出してもらえますか？弱いところと次にやるべきことも一言で。
