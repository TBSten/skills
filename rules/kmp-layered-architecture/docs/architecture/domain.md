# Layer3. Domain

- Dependencies: Data -> Domain <- UI (, App)
- モジュール構成:
    - domain
        - {Domain} ... ドメイン領域ごとにモジュールを切り、その中に Model, UseCase, Repository を配置する。
- アプリ内で扱うデータを Model として、データの操作方法を UseCase として、データソースを Repository として定義する。

## Domain Model

- アプリ内のデータを表す。
- UI で扱いやすくしたり、スナップショットテストなどのために基本的に `@Serializable` にする。難しい場合は必須ではない。
- ID など他と区別すべき primitive なデータは value class を用いる。

```kt
@Serializable
data class UserInfo(
    val userId: UserId,
    val name: String,
    // ...
)

@Serializable
@JvmInline
value class UserId(val value: String)
```

## UseCase interface, 実装

- アプリ内のデータ操作を表す fun interface (operator fun を定義)。
- 取得・更新など 1 操作ごとに 1 つ作成する。
  - ex) GetAppConfig, UpdateUserInfo
- 実装クラスでは constructor に依存関係として別の UseCase または Repository の interface を受け取る。

```kt
fun interface GetAppConfig {
    suspend operator fun invoke(): MergedAppConfig
}

class GetAppConfigImpl(
    private val remoteRepository: RemoteRepository,
    private val localRepository: LocalRepository,
) : GetAppConfig {
    override suspend fun invoke(): MergedAppConfig =
        TODO("implement get app config logic")
}

class FakeGetAppConfig : GetAppConfig {
    override suspend fun invoke(): MergedAppConfig = MergedAppConfig(/* TODO */)
}
```

UseCase の実装は以下のユーティリティを活用して読みやすく実装する:

- `suspend { }` で関数の主作用を実装する。
- メソッドチェーンで retry などの副次的な仕様を記載する。
    - `.recover { error -> ... }` ... 特定条件のエラー時に復帰方法を定義
    - `.retry(/* maxRetryCount = ... */)` ... 最大回数を指定して自動で retry する
    - `.retryWithBackoff(/* maxRetryCount, initialDelay, maxDelay, factor */)` ... 指数バックオフ付きリトライ
    - `.timeout(duration)` ... 指定時間内に完了しなければ `TimeoutCancellationException`
    - `.onSuccess { result -> ... }`, `.onError { error -> ... }` ... 成功/失敗時にアクション実行
    - `.measure { duration, result -> ... }` ... 実行時間と結果を計測
    - `.minimumDelay(duration)` ... 最低実行時間を保証（UX 向けのローディング表示等）
- `.invoke()` で実行する

```kt
class GetAppConfigImpl(
    private val remoteRepository: RemoteRepository,
    private val localRepository: LocalRepository,
) : GetAppConfig {
    override suspend fun invoke(): MergedAppConfig =
        suspend {
            MergedAppConfig(
                remoteRepository.get(),
                localRepository.get(),
            )
        }
            .retry()
            .recover {
                MergedAppConfig(
                    DefaultFallbackValue,
                    localRepository.get(),
                )
            }
            .invoke()
}
```

## Repository interface, Fake 実装

- UseCase で必要とするデータの取得元・更新先を Repository として定義する。
- UseCase 利用者の Test がしやすいように Fake 実装も提供する。

```kt
// AppConfigRemoteRepository.kt
interface AppConfigRemoteRepository {
    suspend fun get(): ServerAppConfig
}

// FakeAppConfigRemoteRepository.kt
class FakeAppConfigRemoteRepository : AppConfigRemoteRepository {
    override suspend fun get(): ServerAppConfig = ServerAppConfig(/* TODO */)
}
```
