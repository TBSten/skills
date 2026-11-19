---
tags: [skill-eval]
max_turns: 20
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Bash]
---

Android アプリの決済画面で、フォントサイズを最大にすると「支払う」ボタンが画面外に押し出される不具合を直しています。
修正後の確認方法として次の 3 案を考えていて、どれでいくか決めたいです。テスト方針の比較をお願いします。

- A: Paparazzi で決済画面の screenshot test を追加 (fontScale 1.0 / 1.3 / 2.0 の 3 パターン、golden と diff 比較、CI で実行)。Paparazzi はまだプロジェクトに入っていない
- B: エミュレータでフォント最大にして手動で操作し、撮った screenshot を Claude に見せて仕様 (Figma のスクショ) と見比べてもらう
- C: staging ビルドを QA チームに渡して、実機 3 台 (Pixel 8 / Galaxy S23 / AQUOS) で見てもらう。QA の予定は 2 日後
