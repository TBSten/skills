# 下位互換の床 (古い Kotlin の利用者への配慮)

## 何をしているか

- `languageVersion` / `apiVersion` を catalog の `kotlinLanguageVersion` (既定 2.2) に下げる
- `coreLibrariesVersion` を `kotlinCoreLibrariesVersion` (既定 2.2.20) に下げる
- JS / Wasm の configuration だけは `kotlin-*` をコンパイラの版 (`kotlin`) で解決させる
  (`build-logic/src/main/kotlin/Targets.kt`)

## なぜ必要か

既定のままだと、公開される metadata と推移的な kotlin-stdlib がコンパイラの版 (2.4) になる。
Kotlin コンパイラは 1 つ先の版の metadata までしか読めないため、Kotlin 2.2 の利用者からは
ライブラリのシンボルがすべて Unresolved reference になる (katachi で実際に報告があった)。

## KMP での実情 (JS / Wasm の ABI 問題)

`coreLibrariesVersion` はプロジェクト全体に効く。KMP でそのまま下げると、JS / Wasm のコンパイルが
次のエラーで落ちる。

```
The Kotlin/Wasm standard library has the ABI version (2.2.0) that is not compatible
with the compiler's current ABI compatibility level (2.4).
```

JS / Wasm の stdlib は klib で、コンパイラと同じ ABI 版でないと読めないためだ。そこで
JS / Wasm 系の configuration (`js*` / `wasmJs*` / `wasmWasi*` / `web*`) だけ解決時に
コンパイラの版へ差し替えている。公開される Gradle Module Metadata 上の依存宣言は床 (2.2.20) のままなので、
利用側は自分のコンパイラに合った stdlib を使う。

## 利用者から見た下限

| 利用側のターゲット | 必要な Kotlin |
|---|---|
| JVM / Android | 床 (既定 2.2) 以上 |
| JS / Wasm / Native | このライブラリのビルドに使った版 (既定 2.4) 以上。Kotlin は新しいコンパイラが作った klib を読めない |

README (en / ja) の Setup にこの表記がある。床や `kotlin` を変えたら README も合わせる。

## 床を変える時

1. `gradle/libs.versions.toml` の `kotlinLanguageVersion` と `kotlinCoreLibrariesVersion` を同時に変える
2. 床より新しい言語機能・stdlib API を使っている箇所がコンパイルエラーになるので直す。
   ただし床のバージョンでは preview 扱いに戻るだけの機能 (context parameters 等) は、
   書き換えずに対応する `-X` フラグ (`-Xcontext-parameters` 等) を `freeCompilerArgs` に足せばよい
   (自ライブラリのビルド自体は常にコンパイラの版で行うため、preview 警告以外の影響は無い)
3. README (en / ja) の Kotlin 下限を直す

jvm 構成 (`--targets jvm`) には JS / Wasm が無いので、床はそのまま効く。

## 未調査

- `-Xklib-abi-compatibility-level` 等で klib の下限を下げ、JS / Wasm / Native の利用者の下限も下げられるか
