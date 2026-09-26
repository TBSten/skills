package com.example.kotlinlibrarybootup

/**
 * ExampleLib のエントリポイント (サンプル API)。ライブラリの実 API に置き換える。
 *
 * explicitApi モードなので、公開する宣言には必ず可視性 (public / internal) を明示する。
 *
 * @property greeting 挨拶の前置き。
 */
public class ExampleLib(
    private val greeting: String = "Hello",
) {
    /**
     * [name] への挨拶文を返す。
     *
     * @throws IllegalArgumentException [name] が空白のみの場合。
     */
    public fun greet(name: String): String {
        require(name.isNotBlank()) { "name must not be blank" }
        return "$greeting, $name!"
    }

    /**
     * 複数人へまとめて挨拶する。API が固まるまでは [ExperimentalExampleLibApi] で opt-in を要求する。
     */
    @ExperimentalExampleLibApi
    public fun greetAll(names: List<String>): List<String> = names.map(::greet)

    /**
     * ライブラリ内部 (他モジュール) からだけ使う API の例。利用者向けの互換性は保証しない。
     */
    @InternalExampleLibApi
    public fun debugDescription(): String = "ExampleLib(greeting=$greeting)"
}
