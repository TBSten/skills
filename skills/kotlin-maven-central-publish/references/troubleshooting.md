# トラブルシューティング

| 症状 | 原因 | 対処 |
|---|---|---|
| `Make sure the Kotlin version 2.2.0 or newer is applied. ... was not able to access Kotlin plugin classes` | vanniktech と KGP が別 classloader にある | convention ビルド (buildSrc / build-logic) の dependencies に KGP の plugin marker を載せる (`references/convention-options.md` §1) |
| `The request for this plugin could not be satisfied because the plugin is already on the classpath with an unknown version` | buildSrc の classpath に載っているプラグインをバージョン付き (`alias(...)` / `version "x"`) で要求した | `id(libs.plugins.<alias>.get().pluginId)` に置き換える。ルートの `alias(...) apply false` は削除してよい |
| `Plugin [id: 'org.jetbrains.kotlin...'] was not found ... (plugin dependency must include a version number for this source)` | included build (build-logic) 構成で KGP を id のみで要求した | build-logic 構成では `alias(...)` (バージョン付き) のままにする |
| `Unresolved reference: SonatypeHost` | vanniktech 0.34.0 で `SonatypeHost` が削除された | `publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)` → `publishToMavenCentral()` |
| `The value for extension 'mavenPublishing' property '...' is final and cannot be changed` | `publishToMavenCentral()` / `signAllPublications()` を二重に呼んだ (例: `gradle.properties` の `mavenCentralPublishing=true` と DSL の両方)、またはモジュール側で `coordinates(...)` を再設定した | どちらか一方にする。モジュール側では `coordinates` を呼ばず pom だけ上書きする |
| `publishToMavenLocal` で `no configured signatory` / `Cannot perform signing task ... because it has no configured signatory` | 鍵が無いのに署名が有効 | convention の署名条件を確認 (`references/convention-options.md` §2)。path 付きタスク名は末尾セグメントで判定する |
| publish で `401 Unauthorized` | `MAVEN_CENTRAL_USERNAME` / `MAVEN_CENTRAL_PASSWORD` が Central Portal の User Token と一致しない (アカウントのログイン情報ではなく Token を使う) | Central Portal で User Token を再発行し Secrets を更新 |
| 署名で `BAD_PASSPHRASE` / `Could not read PGP secret key` | `SIGNING_PASSWORD` が鍵のパスフレーズと不一致、または `GPG_KEY_CONTENTS` が armor 全体 (BEGIN〜END 行) でない | `scripts/setup-secrets.sh --key-id <鍵>` で再登録 |
| Central の検証で `Invalid signature` / `Could not find a public key` | 公開鍵がキーサーバーに届いていない | `gpg --keyserver keyserver.ubuntu.com --send-keys <FINGERPRINT>` (反映に数分〜) |
| Central の検証で `Javadocs must be provided` / `Sources must be provided` | javadoc / sources jar が無い | vanniktech の既定 (`configureBasedOnAppliedPlugins`) を上書きしていないか確認。Dokka を使うなら `dokkaGeneratePublicationJavadoc` を渡す (`dokkaGenerate` は空 jar) |
| Kotlin の古い版の利用者で全シンボルが `Unresolved reference` | 成果物の metadata が新しすぎる | `languageVersion` / `apiVersion` / `coreLibrariesVersion` の床を下げる (`references/convention-options.md` §6) |
| iOS ターゲットで `xcrun ... non-zero exit code: 72` | Command Line Tools のみで Xcode 本体が無い | Xcode をインストールし `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`。CI は macOS runner を使う |
| publish.yml が pre-release で起動しない | `types: [released]` や `[prereleased]` を使っている | `types: [published]` にする |
| `Could not resolve all files for configuration ':classpath'` (CI) | setup-gradle のキャッシュ不整合 | Re-run all jobs |
