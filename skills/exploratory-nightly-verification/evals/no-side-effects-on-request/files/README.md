# tiny-conf

Tiny key=value config loader for Kotlin/JVM.

```kotlin
val config: Config? = ConfigLoader.load("app.conf")
// returns null when the file does not exist
val port = config?.int("port") ?: 8080
```

- `ConfigLoader.load(path)` returns `null` if the file is missing.
- Lines starting with `#` are comments.
- Invalid lines throw `ConfigParseException` with the line number.
