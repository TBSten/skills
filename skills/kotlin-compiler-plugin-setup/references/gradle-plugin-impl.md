# Gradle Plugin Implementation

`KotlinCompilerPluginSupportPlugin` を使って compiler plugin を Gradle plugin としてラップするパターン。

## 基本実装

```kotlin
package <your-package>.gradle

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilerPluginSupportPlugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption

class <YourPlugin>GradlePlugin : KotlinCompilerPluginSupportPlugin {

    override fun apply(target: org.gradle.api.Project) {
        // runtime 依存を自動追加
        target.afterEvaluate {
            val hasKmpPlugin = target.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform")
            val configName = if (hasKmpPlugin) "commonMainImplementation" else "implementation"
            target.dependencies.add(
                configName,
                "<your-group-id>:runtime:<version>",
            )
        }
    }

    override fun isApplicable(kotlinCompilation: KotlinCompilation<*>): Boolean = true

    override fun getCompilerPluginId(): String = "<your-plugin-id>"

    override fun getPluginArtifact(): SubpluginArtifact = SubpluginArtifact(
        groupId = "<your-group-id>",
        artifactId = "compiler-plugin",
        version = "<version>",
    )

    override fun applyToCompilation(
        kotlinCompilation: KotlinCompilation<*>,
    ): Provider<List<SubpluginOption>> {
        return kotlinCompilation.target.project.provider { emptyList() }
    }
}
```

## ポイント

### runtime 依存の自動追加

ユーザーが `plugins { id("<your-plugin-id>") }` だけで使えるように、runtime 依存を自動追加する。
KMP プロジェクトでは `commonMainImplementation`、単一ターゲットでは `implementation` に追加。

### CLI オプションの受け渡し

Gradle extension から compiler plugin に設定を渡す場合:

```kotlin
override fun applyToCompilation(
    kotlinCompilation: KotlinCompilation<*>,
): Provider<List<SubpluginOption>> {
    val project = kotlinCompilation.target.project
    val extension = project.extensions.getByType(<YourPlugin>Extension::class.java)
    return project.provider {
        listOf(
            SubpluginOption(key = "enabled", value = extension.enabled.get().toString()),
        )
    }
}
```

### build.gradle.kts での登録

```kotlin
gradlePlugin {
    plugins {
        create("<plugin-short-name>") {
            id = "<your-plugin-id>"
            implementationClass = "<your-package>.gradle.<YourPlugin>GradlePlugin"
        }
    }
}
```

`java-gradle-plugin` が `META-INF/gradle-plugins/<your-plugin-id>.properties` を自動生成する。
