---
tags: [skill-eval]
max_turns: 30
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

このプロジェクトの CLAUDE.md にある「リリース手順」、毎回やってるワークフローなので TBSten/skills に skill として登録したい。skill 名は `android-release-flow` で、対象は Gradle (version catalog) + CHANGELOG.md を使う Android アプリ全般。group は Kotlin / Android アプリ開発。

TBSten/skills はもう `./skills-repo` に clone してあるので、そこにファイルを作って。この環境は gh にログインしていないので push と PR 作成はしなくていい。代わりに PR のタイトルと本文を `./pr-draft.md` に書いておいて。途中の確認は全部 OK なので、聞き返さずに最後まで進めて。

Input files: CLAUDE.md, skills-repo/README.md, skills-repo/README.ja.md
