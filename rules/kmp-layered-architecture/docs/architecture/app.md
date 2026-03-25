# Layer1. App

- Dependencies: UI, Domain, Data -> App
- モジュール構成:
    - app
        - di
        - {AppEntryPoints}
- アプリのエントリーポイント
- App flavor の切り替え
- DI の起点

## App Flavor Processing

gradle.properties の flavor 設定に応じてソースセットが切り替わる。

```properties
# gradle.properties
buildkonfig.flavor=fake_server # or dev_server or stg_server or prod_server
```

各 flavor ごとに専用ソースセットディレクトリを配置できる。
replace する DI module などを配置する。

```kt
// src/commonFakeServer/kotlin/.../di/FakeServerProviders.kt
@ContributesTo(AppScope::class, replaces = [DataProviders::class])
internal interface FakeServerProviders {
    @Provides
    fun someRepository(): SomeRepository =
        FakeSomeRepository()
}
```

## DI

> **Note:** 以下のコード例は [Metro](https://github.com/nicholasgasior/metro) を使用しています。プロジェクトの DI フレームワークに応じて読み替えてください。

- DI フレームワーク (Metro, Dagger/Hilt, Koin 等) を使って Domain の UseCase や Repository の interface と実装を Providers に定義する。
- Providers の命名規則: `{Category}Providers` (例: `DomainAppConfigProviders`, `DataCacheProviders`, `LoaderProviders`)
  - **必ず `Providers` サフィックスに統一すること。`Module` や単数形 `Provider` は使わない。**

```kt
@ContributesTo(AppScope::class)
internal interface DomainAppConfigProviders {
    @Provides
    fun getAppConfigImpl(
        remoteRepository: AppConfigRemoteRepository,
    ): GetAppConfig = GetAppConfigImpl(
        remoteRepository = remoteRepository,
    )
}
```

```kt
@ContributesTo(AppScope::class)
internal interface DataAppConfigProviders {
    @Provides
    fun appConfigRemoteRepository(
        httpClient: HttpClient,
    ): AppConfigRemoteRepository = AppConfigRemoteRepositoryImpl(
        httpClient = httpClient,
    )
}
```

Fake flavor で差し替えたい場合は `replaces` で差し替える。

```kt
@ContributesTo(AppScope::class, replaces = [DataAppConfigProviders::class])
internal interface FakeDataAppConfigProviders {
    @Provides
    fun appConfigRemoteRepository(): AppConfigRemoteRepository =
        FakeAppConfigRemoteRepository()
}
```
