package dev.hoge.kvstore

interface KeyValueStore {
    suspend fun get(key: String): String?
    suspend fun put(key: String, value: String)
}
