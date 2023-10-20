package com.example.advanced.service

import com.example.advanced.config.CacheKey
import com.example.advanced.config.CacheManager
import com.example.advanced.config.Locker
import com.example.advanced.config.extension.toLocalDate
import com.example.advanced.config.validator.DateString
import com.example.advanced.exception.NotFoundException
import com.example.advanced.model.Article
import com.example.advanced.repository.ArticleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.flow
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.Serializable
import java.time.LocalDateTime
import kotlin.time.Duration.Companion.seconds
private val logger = KotlinLogging.logger {  }
@Service
class ArticleService(
    private val repository: ArticleRepository,
    private val dbClient: DatabaseClient,
    private val cache: CacheManager,
    private val locker: Locker
    redisTemplate: ReactiveRedisTemplate<Any, Any>
) {
    private val ops = redisTemplate.opsForValue()

    init {
        cache.TTL["/article/get"] = 10.seconds
        cache.TTL["/article/get/all"] = 10.seconds
    }

    @Transactional
    suspend fun addBalance(id: Long, amount: Int): Article {
        val key = SimpleKey(ArticleService::addBalance.name, id)
        return locker.lock(key){
            val account = repository.findById(id) ?: throw throw RuntimeException("error")
            delay(3.seconds)
            account.balance += amount
            logger.debug { "before commit: ${account.toResBalance()}" }
            repository.save(account)
            repository.findById(id)!!
        }

    }


    fun getAll(): Flow<Article> {
        return repository.findAll()
    }

//    suspend fun getAll(title: String): Flow<Article> {
//        return repository.findAllByTitleContains(title)
//    }

    suspend fun getAllCached(request: QryArticle): Flow<Article> {
        val key = CacheKey("/article/get/all", request)
        return cache.get(key) {
            getAll(request).toList()
        }?.asFlow() ?: emptyFlow()
    }

    suspend fun getAll(request: QryArticle): Flow<Article> {
        val params = HashMap<String, Any>()
        var sql = dbClient.sql(
            """
            SELECT id, title, body, author_id, created_at, updated_at
            FROM TB_ARTICLE
            WHERE 1=1
            ${
//            if (request.title.isNullOrBlank()) {
//                ""
//            } else {
//                params["title"] = request.title.trim().let { "%$it" }
//                "AND title LIKE :title"
//            }
                // 함수 확장
                request.title.query {
                    params["title"] = it.trim().let { "%$it" }
                    "AND title LIKE :title"
                }
            }
                ${
                request.authorId.query {
                    params["authorId"] = it
                    "AND author_id IN (:authorId)"
                }
            }
                ${
                request.from.query {
                    params["from"] = it.toLocalDate()
                    "AND created_at >= :from"
                }
            }
                ${
                request.to.query {
                    params["to"] = it.toLocalDate().plusDays(1)
                    "AND created_at < :to"
                }
            }
        """.trimIndent()
        )
        params.forEach { (key, value) -> sql = sql.bind(key, value) }
        return sql.map { row ->
            Article(
                id = row.get("id") as Long,
                title = row.get("title") as String,
                body = row.get("body") as String?,
                authorId = row.get("author_id") as Long,
            ).apply {
                createdAt = row.get("created_at") as LocalDateTime?
                updatedAt = row.get("updated_at") as LocalDateTime?
            }
        }.flow()
    }


    suspend fun get(articleId: Long): Article {
        val key = CacheKey("/article/get", articleId)
//        return cache.get<ResArticle>(key) ?: repository.findById(articleId)?.let {
//            cache.set(key, it)
//            ResArticle(it)
//        } ?: throw NotFoundException("post id : $articleId")

        return cache.get(key) { repository.findById(articleId) }
            ?: throw NotFoundException("post id : $articleId")
    }

    @Transactional
    suspend fun create(request: SaveArticle): ResArticle {
        return repository.save(
            Article(
                title = request.title ?: "",
                body = request.body,
                authorId = request.authorId,
            )
        ).let {
            if (it.title == "error") {
                throw RuntimeException("error")
            }
            ResArticle(it)
        }
    }

    @Transactional
    suspend fun update(articleId: Long, request: SaveArticle): ResArticle {
        return repository.findById(articleId)?.let { article ->
            request.title?.let { article.title = it }
            request.body?.let { article.body = it }
            request.authorId?.let { article.authorId = it }
            repository.save(article).let {
                val key = CacheKey("/article/get", articleId)
                cache.delete(key)
                ResArticle(it)
            }
        } ?: throw NotFoundException("No post(id:$articleId) found")
    }

    @Transactional
    suspend fun delete(articleId: Long) {
        repository.deleteById(articleId).also {
            val key = CacheKey("/article/get", articleId)
            cache.delete(key)
        }
    }

}

fun <T> T?.query(f: (T) -> String): String {
    return when {
        this == null -> ""
        this is String && this.isBlank() -> ""
        this is Collection<*> && this.isEmpty() -> ""
        this is Array<*> && this.isEmpty() -> ""
        else -> f.invoke(this)
    }
}

data class SaveArticle(
    var title: String? = null,
    var body: String? = null,
    var authorId: Long? = null,
)

data class ResArticle(
    var id: Long,
    var title: String,
    var body: String,
    var authorId: Long,
    var createdAt: LocalDateTime?,
    var updatedAt: LocalDateTime?,
) {
    constructor(article: Article) : this(
        id = article.id,
        title = article.title ?: "",
        body = article.body ?: "",
        authorId = article.authorId ?: 0,
        createdAt = article.createdAt,
        updatedAt = article.updatedAt,
    )
}

data class QryArticle(
    val title: String?,
    val authorId: List<Long>?,
    @DateString
    val from: String?,
    @DateString
    val to: String?,
) : Serializable