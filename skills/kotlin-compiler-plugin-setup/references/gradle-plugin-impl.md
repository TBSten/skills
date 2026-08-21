# Gradle Plugin Implementation

`KotlinCompilerPluginSupportPlugin` を使って compiler plugin を Gradle plugin としてラップするパターンの設計解説。
**完全なコードは `example/gradle-plugin/src/main/kotlin/com/example/compilerpluginsetup/gradle/ExampleGradlePlugin.kt` が SSoT** (scaffold.sh がコピー・置換する)。

## 実装の構成要素

| override | 役割 |
|---|---|
| `apply(target)` | runtime 依存の自動追加 |
| `isApplicable(compilation)` | plugin を適用する compilation の選別 (通常 `true`) |
| `getCompilerPluginId()` | `CommandLineProcessor.pluginId` と一致させる |
| `getPluginArtifact()` | compiler plugin の Maven 座標 (groupId / artifactId / version) |
| `applyToCompilation(compilation)` | compiler plugin へ渡す `SubpluginOption` のリスト |

## ポイント

### runtime 依存の自動追加

ユーザーが `plugins { id("<plugin-id>") }` だけで使えるように、`apply()` 内で runtime 依存を自動追加する。
KMP プロジェクトでは `commonMainImplementation`、単一ターゲットでは `implementation` に追加 (実ファイル参照)。

### CLI オプションの受け渡し

Gradle extension から compiler plugin に設定を渡す場合、`applyToCompilation` で extension を読んで `SubpluginOption` に変換する:

```kotlin
override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>,
): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(YourPluginExtension::class.java)
    return project.provider {
        listOf(
            SubpluginOption(key = "enabled", value = extension.enabled.get().toString()),
        )
    }
}
```

compiler plugin 側の受け口は `plugin-registration.md` の「CLI オプションの追加」を参照。

### build.gradle.kts での登録

`example/gradle-plugin/build.gradle.kts` の `gradlePlugin { plugins { create(...) } }` ブロック参照。
`java-gradle-plugin` が `META-INF/gradle-plugins/<plugin-id>.properties` を自動生成する。
