---
name: add-support-kotlin-version
description: >
  Adds or removes a specific Kotlin version from a Kotlin Compiler Plugin project's
  support matrix. Covers two architectures: compat module layer (ServiceLoader dispatch,
  metro-style) and source set separation (Gradle dynamic source directory switching).
  Determines whether an existing compat module covers the target version or a new module
  is needed; updates CI matrix, kctfork version map, version catalog, and README.
  Use when requested: "Kotlin X.Y.Z をサポートしたい", "新しい Kotlin バージョン対応",
  "サポート Kotlin バージョンを追加したい", "Kotlin の新バージョン対応したい",
  "Kotlin X.Y.Z のサポートを外したい", "compat module を追加したい",
  "CI matrix に Kotlin X.Y.Z を追加したい", "kctfork を更新したい",
  "Kotlin RC/Beta を入れたい", "複数バージョン対応プラグインに新バージョンを追加",
  "新しい Kotlin パッチで NoSuchMethodError が出た", "compat layer の version を追加".
---

# Add/Remove Supported Kotlin Version

Kotlin Compiler Plugin のサポート対象 Kotlin バージョンを追加・削除する。

このスキルはプロジェクトに既に複数バージョン対応基盤（compat module layer または source set separation）が存在することを前提とする。基盤の初期セットアップは `kotlin-compiler-plugin-setup` の Step 10 を参照。

---

## Step 0: 要件確認とアーキテクチャ判定

ユーザーの指示から以下を把握する。明確なら聞き直さない。

1. **対象バージョン** — 追加 / 削除 / 置換したい Kotlin バージョン
2. **operation** — 追加 / 削除 / 置換
3. **プロジェクトのアーキテクチャ** — 以下のどちらか:
   - **A: Compat Module Layer** — `compiler-plugin/compat-kXX/` 形式のモジュールがある。ServiceLoader で実行時に実装を選択する
   - **B: Source Set Separation** — `src/v2_0_0/kotlin` / `src/pre_2_0_0/kotlin` 形式のディレクトリがある。Gradle がビルド時にソースセットを切り替える

アーキテクチャが不明な場合は `settings.gradle.kts` と `build.gradle.kts` を読んで判断する。

---

## Step 1A: Compat Module Layer の場合

### 1A-1: 既存 compat module で対応可能か判定

各 compat module には `minVersion` が設定されており、`minVersion ≤ current < 次のモジュールの minVersion` の範囲を担当する。

```
既存モジュール例:
  compat-k2000: minVersion = "2.0.0"  → 2.0.0 – 2.0.1x をカバー
  compat-k2020: minVersion = "2.0.20" → 2.0.20 – 2.1.1x をカバー
  compat-k21:   minVersion = "2.1.20" → 2.1.20 – 2.1.x をカバー
  compat-k23:   minVersion = "2.2.0"  → 2.2.0 – (最新) をカバー
```

**判定**: 対象バージョンが既存 module の範囲に収まれば **既存 module で OK**。

ただし、その後の Step 3 でテストを実行し、`NoSuchMethodError` / `NoClassDefFoundError` / `IncompatibleClassChangeError` が出た場合は API 差異あり → 以下を検討:
- 小さな差異 → 既存 module のソースを修正して回避（reflection 化 等）
- 回避不能 → Step 1A-2 へ（新 compat module 作成）

**削除の場合**: Step 2 のファイル更新のみ実施。compat module は残したままで OK（ServiceLoader が自動スキップ）。

### 1A-2: 新 compat module が必要な場合

新しい API 境界が生じた場合のみ実施。詳細コードは `references/compat-module-setup.md` を参照。

1. **最も近い既存 module をコピー**
   ```bash
   cp -r compiler-plugin/compat-k{元名} compiler-plugin/compat-k{新名}
   ```

2. **パッケージ名をリネーム** (find + sed でソース内の `k{元名}` を `k{新名}` に一括置換)

3. **`build.gradle.kts` を編集**:
   - `compileOnly(libs.compat.embeddable.k{新名})` — 新しい minVersion に対応する `kotlin-compiler-embeddable` を参照
   - `apiVersion` / `languageVersion` を新 minVersion の major.minor に設定
   - `gradle/libs.versions.toml` に `compat-embeddable-k{新名}` を追加

4. **Factory の `minVersion` を更新** — 実装クラスの Factory ネストクラスに記載

5. **`META-INF/services/...IrInjector$Factory`** の FQN を新 module の Factory に書き換え

6. **`settings.gradle.kts`** に `include(":compiler-plugin:compat-k{新名}")` 追加

7. **メインの `compiler-plugin/build.gradle.kts`** に `runtimeOnly(project(":compiler-plugin:compat-k{新名}"))` 追加

---

## Step 1B: Source Set Separation の場合

`src/v{バージョン}/kotlin` または `src/pre_{バージョン}/kotlin` の命名規則で新しいソースディレクトリを追加する。

詳細コードは `references/source-set-separation.md` を参照。

1. **ディレクトリ作成**: `src/v{major}_{minor}_{patch}/kotlin/`
2. **VersionSpecificAPIImpl を実装**: そのバージョン向けの処理を書く
3. **Gradle の動的ソースセット選択ロジック** に新バージョンの条件を追加

---

## Step 2: 共通ファイル更新

アーキテクチャによらず必ず更新する。

### CI Matrix

`.github/workflows/ci.yml` の `matrix.kotlin-version` (または同等のフィールド) に対象バージョンを追加/削除:

```yaml
strategy:
  fail-fast: false
  matrix:
    kotlin-version:
      - "2.3.20"
      - "2.4.0"   # ← 追加
```

削除の場合はリストから除く。

### kctfork バージョンマップ

unit test で kctfork を使っている場合、`build.gradle.kts` の kctfork バージョンマップを更新する:

```kotlin
val kctforkForKotlin: Map<String, String> = mapOf(
    "2.3.20" to "0.12.1",
    "2.4.0"  to "0.XX.X",  // ← ZacSweers/kotlin-compile-testing/releases で確認
)
```

kctfork の最新リリースは https://github.com/ZacSweers/kotlin-compile-testing/releases で確認。
新 minor (例: 2.5.x) 対応には通常新しい kctfork が必要。新 patch は既存バージョンで動く場合が多い。

### Compose Multiplatform マップ (KMP プロジェクトの場合)

integration-test が CMP を使っている場合、`settings.gradle.kts` の Kotlin → Compose バージョンマップを更新する。JetBrains/compose-multiplatform の Release Notes で "Supports Kotlin X.Y" アナウンスを確認。

### README

`README.md` と `README.ja.md` の Supported Kotlin Versions テーブルに行を追加/削除。両方を必ず更新する。

---

## Step 3: テスト実行

全バージョン × 全テストが GREEN になるまで繰り返す。

```bash
# unit test (単一バージョンで確認)
./gradlew :compiler-plugin:test -Ptest.kotlin=X.Y.Z

# smoke test / integration test (全バージョン)
./scripts/smoke-test.sh X.Y.Z        # 追加したバージョン単体
./scripts/test-all.sh                # 全バージョン
```

スクリプトが存在しない場合は CI 相当のコマンドを手動で実行。

失敗した場合:
- `NoSuchMethodError` / `NoClassDefFoundError` → API 差異あり。Step 1A-1 の修正フローへ
- コンパイルエラー → compat module のソース修正
- テスト失敗 → テストロジックの修正

---

## Step 4: ドキュメント更新

テストが全バージョン GREEN になったら:

1. **CHANGELOG / current.md** (プロジェクトに存在する場合): 対象バージョンの status を更新し、変更履歴に追記
2. **commit**: 変更を論理単位で分割してコミット
   - scripts / CI 更新 と compat module 追加 は別 commit にするとレビューしやすい

---

## チェックリスト

- [ ] 対象バージョンの operation と既存アーキテクチャを確認
- [ ] compat module / ソースセット の追加・修正 (必要な場合のみ)
- [ ] CI matrix に対象バージョンを追加/削除
- [ ] kctfork バージョンマップを更新
- [ ] Compose マップを更新 (KMP の場合)
- [ ] README 両言語に反映
- [ ] 全バージョンでテスト GREEN
- [ ] ドキュメント・changelog 更新

---

## 参考

- kctfork リリース: https://github.com/ZacSweers/kotlin-compile-testing/releases
- Compose Multiplatform リリース: https://github.com/JetBrains/compose-multiplatform/releases
- Kotlin リリース: https://github.com/JetBrains/kotlin/releases
- Compat module の詳細コード例: `references/compat-module-setup.md`
- Source set separation の詳細コード例: `references/source-set-separation.md`
- CI matrix の YAML テンプレート: `references/ci-matrix.md`
