package com.example.advanced

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.test.context.ActiveProfiles
import java.util.NoSuchElementException
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@SpringBootTest
@ActiveProfiles("local")
class RedisTemplateTest(
    private val template: ReactiveRedisTemplate<Any, Any>
) : StringSpec({
    val KEY = "key"
    afterTest {
        template.delete(KEY).awaitSingle()
    }
    "hello reactive redis" {
        val ops = template.opsForValue()

        shouldThrow<NoSuchElementException> {
            ops.get("key").awaitSingle()
        }
        ops.set("key", "blabla fastcampus").awaitSingle()
        ops.get("key").awaitSingle() shouldBe "blabla fastcampus"

        template.expire(KEY, 3.seconds.toJavaDuration()).awaitSingle()
        delay(5.seconds)
        shouldThrow<NoSuchElementException> {
            ops.get("key").awaitSingle()
        }
    }
})