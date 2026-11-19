# acmepay-android

AcmePay の Android アプリ。

## ビルド

- `./gradlew assembleDebug`

## リリース手順

リリースのたびに以下を上から順に行う。

1. `main` を最新にし、`release/x.y.z` ブランチを切る
2. `gradle/libs.versions.toml` の `appVersionName` と `appVersionCode` を上げる (versionCode は +1、versionName は semver)
3. `CHANGELOG.md` の `## Unreleased` を `## x.y.z - YYYY-MM-DD` に置き換え、空の `## Unreleased` を先頭に足す
4. `./gradlew :app:bundleRelease` が通ることを確認する
5. PR を作ってマージしたら `vX.Y.Z` タグを打って push する
6. 社内 Wiki (https://wiki.acmepay.internal/mobile/release) のリリース台帳に追記し、Slack の #acme-mobile-release で @tanaka-pm に共有する

よくあるミス: versionCode の上げ忘れ (Play Console が同じ versionCode を拒否する)、CHANGELOG の日付ずれ。
