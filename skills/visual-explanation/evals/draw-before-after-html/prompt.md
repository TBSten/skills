---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit]
---

PR #58 (注文同期の API 呼び出しに 503 リトライを入れた) の PR 本文に、レビュアーが最初の 1 枚で変化が分かる図を付けたい。
main とこのブランチで同じ flaky なスタブに対して同期スクリプトを走らせたログが logs/ にあるので、それを元に描いてほしい。
スクショの撮影と画像の push は後で自分でやるので、今回は図の HTML と、PR 本文の下書き body.md まで作って。画像の URL はプレースホルダでいいです。

Input files: src/fetchWithRetry.ts, logs/before-main.log, logs/after-branch.log
