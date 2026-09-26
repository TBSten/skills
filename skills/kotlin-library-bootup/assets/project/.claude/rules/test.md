---
paths:
  - "**/src/*Test/**/*.kt"
  - "**/src/test/**/*.kt"
  - "integrationTest/**/*.kt"
---

# テスト規約

## フレームワーク

- **kotest** の `FreeSpec` で書く (JUnit Platform 上で動く)
- アサーションは kotest の matcher (`shouldBe`, `shouldThrow<T>` 等) を使う
- テストは可能な限り `commonTest` に置く。プラットフォーム固有の挙動だけ `jvmTest` 等に置く

```kt
class ExampleLibSpec : FreeSpec({
    "空の入力を渡すと空の結果を返す" {
        // ...
    }

    "不正な入力" - {
        "負の数を渡すと ExampleLibInvalidInputException を投げる" {
            shouldThrow<ExampleLibInvalidInputException> { /* ... */ }
        }
    }
})
```

## テスト名

- **日本語で intent (何を保証しているか) を書く。** 「〇〇のとき △△ になる」の形が基本
- 実装の手順 (`fooを呼ぶ`) ではなく、振る舞い・仕様を書く。テスト名だけ読んで仕様が分かるようにする
- **連番を付けない。** テスト名・関数名・snapshot 名・テスト用パッケージ名・golden ファイル名のいずれにも
  `01`, `case1`, `p01` のような番号を入れない。番号は追加・削除のたびにずれ、意味を運ばない
- ネスト (`"文脈" - { ... }`) で前提条件をまとめ、葉のテスト名を短く保つ

## 書き方

- 1 テスト 1 振る舞い。複数の性質を 1 つに詰め込まない
- 新しいテストを足したら、**一度わざと実装を壊して落ちることを確かめる**。緑であることは、見ていることの証明にならない
- 例外のテストは型だけでなく、利用者に見える文面 (何が起きたか / どう直すか) も確認する
- 公開 API の使い勝手は `integrationTest/` (Maven 座標経由の独立ビルド) で確かめる。
  `@InternalExampleLibApi` の opt-in 壁や `explicitApi` の漏れは、本体モジュールの中からは見えない

## 実行

```bash
./gradlew jvmTest                    # 素早いフィードバック
./gradlew allTests                   # 全ターゲット
./gradlew -p integrationTest test    # 結合テスト
```

ログは `.local/tmp/` に保存してから読む (`.claude/skills/verify-changes/` 参照)。
