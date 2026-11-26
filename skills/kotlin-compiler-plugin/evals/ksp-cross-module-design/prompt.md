---
tags: [skill-eval]
max_turns: 15
timeout_seconds: 600
allowed_tools: [Read, Glob, Grep, Skill]
---

マルチモジュールの Android アプリ (:app, :feature:home, :feature:settings, :feature:profile ...) で、各 feature モジュールにある `@Feature` 付きクラスを全部集めて、:app で `val allFeatures: List<KClass<*>>` として使いたいです。

最初 KSP で書いてみたんですが、:app で processor を回しても :feature:* 側の `@Feature` クラスが `resolver.getSymbolsWithAnnotation` に出てこなくて詰まっています。Kotlin 2.3 (K2) を使っています。

これ、どういう設計にすれば実現できますか？ まだコードは書かなくていいので、仕組みと方針を説明してください。
