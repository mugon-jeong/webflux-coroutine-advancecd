package com.example.advanced.config

import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.cache.interceptor.SimpleKey
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val logger = KotlinLogging.logger { }

@Component
class Locker(
    template: ReactiveRedisTemplate<Any, Any>
) {
    private val localLock = ConcurrentHashMap<SimpleKey, Boolean>()

    private val ops = template.opsForValue()
    suspend fun <T> lock(key: SimpleKey, work: suspend () -> T): T {
        if (!tryLock(key))
            throw TimeoutException("fail to obtain lock ($key)")
        try {
            return work.invoke()
        } finally {
            unlock(key)
        }
    }

    private suspend fun tryLock(key: SimpleKey): Boolean {
        // sping lock 구현
        // 요청시 실패하면 다시 요청

        val start = System.nanoTime()

        // setIfAbsent 보단 pub/sub이 나음
        // redisson 라이브러리 사용하면 ops.tryLock()을 사용하면 더 좋음 -> pub/sub 기반으로 작동함
        // 알아서 구현되어 있음
        while (
            !localLock.contains(key) &&
            !ops.setIfAbsent(key, true, 10.seconds.toJavaDuration()).awaitSingle()
        ) {
            logger.debug { "-spin lock : $key" }
            // 계속 바로바로 요청하면 리소스 낭비하니 딜레이 적용
            delay(100)

            val elapsed = (System.nanoTime() - start).nanoseconds
            // 10초 동안 lock을 못잡으면 그냥 종료
            if (elapsed >= 10.seconds) {
                return false
            }
        }
        localLock[key] = true
        // loop가 끝났으면 lock을 잡았다는 의미
        return true
    }

    private suspend fun unlock(key: SimpleKey) {
        try {
            ops.delete(key).awaitSingle()
        } catch (e: Exception) {
            logger.warn(e.message, e)
        } finally {
            localLock.remove(key)
        }
    }
}