package dev.example.conf

public class Config internal constructor(private val values: Map<String, String>) {
    public fun string(key: String): String? = values[key]
    public fun int(key: String): Int? = values[key]?.toIntOrNull()
}
