# Layer4. Data

- Dependencies: App <- Data -> Domain
- モジュール構成:
    - data:
        - {DataSource} ... データソースごとにモジュールを切り、その中に Repository 実装と Data Model を配置する。
- Domain で定義したデータソースである Repository の本実装を配置する。
- Repository の実装には Ktor client, DataStore, Room などのデータ層ライブラリを利用する。

## Repository の実装

- Domain で定義された Repository の実装を置く。

```kt
// ServerAppConfigRepositoryImpl.kt
class ServerAppConfigRepositoryImpl(
    private val httpClient: HttpClient,
) : ServerAppConfigRepository {
    override suspend fun get(): ServerAppConfig =
        httpClient
            .get("app-config")
            .body<AppConfigResponse>()
            .toServerAppConfig()

    private fun AppConfigResponse.toServerAppConfig() =
        ServerAppConfig(
            flag1 = flag1,
        )
}

@Serializable
private data class AppConfigResponse(
    val flag1: Boolean,
    // ...
)
```

## データ層ライブラリ

- app でも利用する必要があるため、依存ライブラリは `implementation` ではなく `api` で依存を追加する。

```kts
// data/appApi/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.ktor.client)
        }
    }
}

// data/local/build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            api(libs.dataStore)
            api(libs.room)
        }
    }
}
```
