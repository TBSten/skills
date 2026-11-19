---
tags: [skill-eval]
max_turns: 15
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill, Bash]
---

本番で、Android 14 の一部端末 (ナビゲーションバーがジェスチャーじゃなく 3 ボタンの設定) だけ、設定画面の「ログアウト」ボタンがナビゲーションバーの裏に隠れて押せないという問い合わせが来ました。

リリース前の確認は Robolectric の unit test で、`onView(withId(R.id.logout)).check(matches(isDisplayed()))` を 1 ケースだけ書いてて CI で green でした。
なんでテストで見逃したのか、検証方法の観点から説明してほしいです。
