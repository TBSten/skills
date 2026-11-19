package jp.blog.app.domain

data class Article(val id: Long, val title: String, val excerpt: String)

interface ArticleRepository {
    suspend fun fetchLatest(): List<Article>
}
