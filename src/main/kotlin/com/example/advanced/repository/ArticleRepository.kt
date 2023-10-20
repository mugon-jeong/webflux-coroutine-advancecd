package com.example.advanced.repository

import com.example.advanced.model.Article
import kotlinx.coroutines.flow.Flow
import org.springframework.data.relational.core.sql.LockMode
import org.springframework.data.relational.repository.Lock
import org.springframework.data.repository.kotlin.CoroutineCrudRepository
import org.springframework.stereotype.Repository

@Repository
interface ArticleRepository : CoroutineCrudRepository<Article, Long> {

    suspend fun findAllByTitleContains(title: String): Flow<Article>

    // 조회하면 해당 row에 대해 locking
//    @Lock(LockMode.PESSIMISTIC_WRITE)
    suspend fun findArticleById(id: Long): Article?
}