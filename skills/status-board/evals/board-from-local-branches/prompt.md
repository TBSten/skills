---
tags: [skill-eval]
max_turns: 25
timeout_seconds: 900
allowed_tools: [Read, Glob, Grep, Skill, Write, Edit, Bash]
---

今の作業状況を 1 枚のボードにまとめて見せて。何が止まってるか一目で分かるようにしたい。

補足: ブランチは feat/login の上に feat/login-ui を積んでる (login-ui は login が入らないと出せない)。feat/payment は決済プロバイダを Stripe にするか PAY.JP にするか僕がまだ決めてないので止まってる。この環境だと gh にログインしてないので GitHub の情報は取れなくていい。

Input files: README.md, .gitignore (git リポジトリ: main / feat/login / feat/login-ui / feat/payment)
