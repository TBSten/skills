---
name: kotlin-tuple
description: >
  Generates type-safe Tuple utilities for Kotlin and Kotlin Multiplatform projects.
  Creates Tuple data classes, tupleOf factories, toList conversion, KSerializer for kotlinx.serialization, type-safe awaitAll, a Result-returning awaitAllCatching, and allNotNullOrNull in one go.
  Use when requested: "add Tuple", "generate tupleOf", "type-safe Tuple", "type-safe awaitAll",
  "add allNotNullOrNull", "await multiple Deferred with type safety",
  "run parallel tasks without one failure cancelling the rest", "awaitAll returning Result",
  "null-check multiple nullable values at once".
  For Kotlin/KMP projects where Pair/Triple is not enough for multi-element grouping.
metadata:
  status: Active
  group: Kotlin / Android アプリ開発
---

# Kotlin Tuple Utility Generation Skill

Generates type-safe Tuple utilities for Kotlin/KMP projects by running the bundled parametric generator script (`scripts/generate.py`).

## Usage

Before generating code, confirm the following with the user in **a single message**.

### Confirmation Items

1. **Target module** (for multi-module projects, which module to generate into)
   - Analyze the project structure and suggest a candidate module
   - Ask the user to confirm or specify a different module
2. **Package name and output directory**
   - Infer from the module's existing package structure and suggest a candidate
   - Ask the user to confirm or specify a different package name
3. **Maximum Tuple size** (default: 20, supported range: 4–99)
   - Ask: "Generate Tuple0–TupleN (default: 20)? Enter a different number to change."
4. **File types to generate** (default: all)
   - Let the user select (multiple choice):
     - [x] `Tuple.kt` + `TupleFactory.kt` — Tuple data classes and tupleOf factories (required, always generated)
     - [x] `TupleToList.kt` — `toList()` extension functions
     - [x] `AbstractTupleSerializer.kt` + `TupleSerializer.kt` — KSerializer for kotlinx.serialization
     - [x] `AwaitAll.kt` — Type-safe awaitAll (fail-fast, takes `Deferred`)
     - [x] `AwaitAllCatching.kt` — `Result`-returning awaitAll: takes `suspend () -> T` blocks so one failure does not cancel the rest
     - [x] `TupleResult.kt` — `allSuccessOrNull()` / `allSuccessOrFailure()` for a Tuple of `Result`
     - [x] `AllNotNullOrNull.kt` — allNotNullOrNull utility

### Skipping Confirmation

When the user's intent is clear, skip the confirmation and proceed directly with default settings:
- "デフォルトで" / "デフォルト設定で" / "with default settings"
- "全部入りで" / "全部生成して" / "generate all"
- Arguments explicitly specify target module, package, and options

### ARGUMENTS の処理

スキルが `ARGUMENTS` パラメータを受け取った場合：
- ARGUMENTS の値は **デフォルト値** として扱う（対象モジュール、パッケージ名、オプション等の推定に使用）
- ユーザメッセージで明示的に指定された値がある場合は、**ユーザメッセージを優先** する
- ARGUMENTS とユーザメッセージが矛盾する場合は、ユーザメッセージの指定に従う

### Output Directory Detection

Detect the source set directory based on the module type:

| Module type | Source directory pattern |
|---|---|
| Kotlin Multiplatform (commonMain) | `<module>/src/commonMain/kotlin/` |
| JVM / Android (main) | `<module>/src/main/kotlin/` or `<module>/src/main/java/` |
| Single-module project | `src/main/kotlin/` or `src/main/java/` |

Detection steps:
1. Check `build.gradle.kts` / `build.gradle` for KMP plugin (`kotlin("multiplatform")`) or JVM/Android plugin
2. Look for existing `src/commonMain/kotlin`, `src/main/kotlin`, or `src/main/java` directories
3. Append the package path (e.g., `com.example.tuple` → `com/example/tuple/`)

### Example Confirmation Message

```
I'll generate Tuple utilities. Let me confirm the following:

1. Target module: **shared** (detected)
   → Enter a different module name if needed

2. Package: `com.example.tuple` (detected)
   Output: `shared/src/commonMain/kotlin/com/example/tuple/`
   → Enter a different package name if needed

3. Max Tuple size: **20** (default)
   → Enter a number to change

4. Files to generate:
   - [x] Tuple.kt + TupleFactory.kt (required)
   - [x] TupleToList.kt (toList() conversion)
   - [x] AbstractTupleSerializer.kt + TupleSerializer.kt (kotlinx.serialization support)
   - [x] AwaitAll.kt (type-safe awaitAll)
   - [x] AwaitAllCatching.kt (Result-returning awaitAll; partial failures stay isolated)
   - [x] TupleResult.kt (allSuccessOrNull / allSuccessOrFailure)
   - [x] AllNotNullOrNull.kt (null-safety utility)
   → Uncheck any you don't need

OK to proceed?
```

## Generation Method: Run the Bundled Generator

This skill ships a parametric generator at `scripts/generate.py` (Python 3, standard library only) and golden example output under `example/src/commonMain/kotlin/com/example/tuple/`.
**Do NOT generate Tuple code from scratch, and do NOT read, modify, or reimplement the script — run it as-is.**
It deterministically produces the same code as the example files for any max Tuple size N (4–99), with the package name already applied (no `cp` / `sed` / manual trimming or extending needed).

### Step-by-step

1. Determine the target output directory `<TARGET_DIR>` = source root + package path (e.g. `shared/src/commonMain/kotlin/com/example/tuple`) — see "Output Directory Detection" above
2. **Run the generator** with the confirmed settings:
   ```bash
   python3 "${CLAUDE_SKILL_DIR}/scripts/generate.py" \
     --package <USER_PACKAGE> \
     --max <N> \
     --out <TARGET_DIR> \
     --parts <PARTS>
   ```
   - `${CLAUDE_SKILL_DIR}` はこのスキルのディレクトリ (SKILL.md があるディレクトリ)
   - `--parts` は確認項目 4 の選択結果をカンマ区切りで渡す（全ファイル選択時は `--parts` ごと省略してよい）:

     | Selected files | parts value |
     |---|---|
     | Tuple.kt + TupleFactory.kt (required) | `tuple,factory` — always generated even if omitted |
     | TupleToList.kt | `tolist` |
     | AbstractTupleSerializer.kt + TupleSerializer.kt | `serializer` |
     | AwaitAll.kt | `awaitall` |
     | AwaitAllCatching.kt | `awaitcatching` |
     | TupleResult.kt | `result` |
     | AllNotNullOrNull.kt | `allnotnull` |
   - The script creates `<TARGET_DIR>`, writes the selected files with the correct package declaration, and prints a single-line JSON result on the last line of stdout
3. **Handle the result** by exit code / JSON `status`:

   | Exit | `status` | What to do |
   |---|---|---|
   | 0 | `generated` | Success. Keep the JSON (`files`, `existing`) for the completion report in Step 6, then continue |
   | 3 | `ACTION_REQUIRED` | Pre-existing Tuple definitions were detected in the module (`existing` in the JSON lists the paths). Ask the user whether to: (a) keep both — rerun with `--ignore-existing`, (b) use a different package / output directory, or (c) cancel |
   | 2 | — | Output files already exist in `<TARGET_DIR>`. Confirm overwrite with the user, then rerun with `--force` (keep `--ignore-existing` if it was needed) |
   | 1 | — | Invalid arguments. Fix the arguments following the `FIX:` line on stderr — do not edit the script |
   - 既存 Tuple 定義の検出（テキスト検索・ディレクトリ検索）は generate.py の preflight が自動で行う。検出結果 (JSON の `existing`) は Step 6 の完了メッセージで使用するため保持しておくこと
4. **Verify and add dependencies** in `build.gradle.kts`:
   依存関係を確認し、不足している場合は追加する。追加した場合はユーザに通知すること。
   - If TupleSerializer.kt is included:
     - `kotlinx-serialization` plugin が未設定 → `build.gradle.kts` に追加（公式ドキュメントを参照して正しい設定方法で追加）
     - `kotlinx-serialization-json` dependency が未設定 → `commonMain.dependencies`（KMP）または `dependencies`（JVM/Android）に追加
   - If AwaitAll.kt or AwaitAllCatching.kt is included:
     - `kotlinx-coroutines-core` dependency が未設定 → 同様に追加
   - TupleResult.kt は stdlib の `Result` のみを使うため追加依存は不要
5. **Build verification**: Run a compile check to ensure the generated files have no errors:
   - KMP: `./gradlew :<module>:compileKotlinJvm`
   - Android: `./gradlew :<module>:compileDebugKotlin`
   - If errors occur, fix them before completing
6. **Completion message** — 以下のテンプレートに従って出力する:
    ```
    ## 生成完了

    **パッケージ**: `<package>`
    **出力先**: `<output_dir>`

    ### 生成ファイル
    - Tuple.kt
    - TupleFactory.kt
    - ...

    ### 依存関係
    - [変更なし / 追加: kotlinx-serialization plugin, kotlinx-serialization-json, ...]

    ### 既存 Tuple パッケージ
    - [なし / `<package1>`, `<package2>`, ... — 不要であれば削除を検討してください]

    ### ビルド結果
    - [SUCCESS / FAILED — エラー内容]
    ```
    - Step 2 の generate.py が検出した既存 Tuple パッケージ（JSON の `existing`: テキスト検索・ディレクトリ検索の両方の結果）を **必ず** 報告する

### Why This Approach

- The Tuple utilities are ~3,100 lines of fully regular, repetitive Kotlin — exactly what a deterministic script generates better than an AI: zero context consumption, no trim/extend mistakes, any N from 4 to 99, and no BSD-`sed` portability issues
- `example/` remains the golden reference: `scripts/generate.py --self-test` verifies that the generator reproduces it byte-for-byte（メンテナ用。通常の生成フローで実行する必要はない）
- The reference `.md` files ([tuple-to-list.md](./tuple-to-list.md), [tuple-serializer.md](./tuple-serializer.md), [await-all.md](./await-all.md), [await-all-catching.md](./await-all-catching.md), [tuple-result.md](./tuple-result.md), [all-not-null-or-null.md](./all-not-null-or-null.md)) document the code patterns for human readers — they are NOT needed during generation
