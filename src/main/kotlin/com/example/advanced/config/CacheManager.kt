package com.example.advanced.config

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

@Component
class CacheManager(
    redisTemplate: ReactiveRedisTemplate<Any, Any>
) {
    private val ops = redisTemplate.opsForValue()

    val TTL = HashMap<Any, Duration>()

    suspend fun <T> get(key: CacheKey): T? {
        return ops.get(key).awaitSingleOrNull()?.let { it as T }
    }
}

class CacheKey(val group: Any, vararg elements: Any) : SimpleKey(group, *elements)