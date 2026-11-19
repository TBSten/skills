---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

Android Studio / IntelliJ 向けに、Navigation のルート定義を解析して tool window にグラフ表示するプラグインを作り始めたいです。Jewel (Compose) の tool window と、Analysis API を使ったテストが回る土台をまず用意してください。

- 生成先: ./route-viewer
- package: io.github.hoge.routeviewer
- plugin id: io.github.hoge.routeviewer
- plugin 名: Route Viewer
- vendor: hoge

この環境は IDE のダウンロードができないので、Gradle のビルド確認はしなくていいです。生成できたら、次に自分が何をすればいいかも教えてください。
