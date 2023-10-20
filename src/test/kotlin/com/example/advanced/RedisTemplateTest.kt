package com.example.advanced

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Range
import org.springframework.data.geo.Circle
import org.springframework.data.geo.Distance
import org.springframework.data.geo.Metrics
import org.springframework.data.geo.Point
import org.springframework.data.redis.connection.DataType
import org.springframework.data.redis.connection.RedisGeoCommands
import org.springframework.data.redis.connection.RedisGeoCommands.*
import org.springframework.data.redis.core.ReactiveListOperations
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveZSetOperations
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.util.*
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val logger = KotlinLogging.logger {}

@OptIn(ExperimentalStdlibApi::class)
@SpringBootTest
@ActiveProfiles("local")
class RedisTemplateTest(
    private val template: ReactiveRedisTemplate<Any, Any>
) :WithRedisContainer, StringSpec({
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
    "LinkedList" {
        val ops = template.opsForList()
        ops.rightPushAll(KEY, 2, 3, 4, 5).awaitSingle()
        template.type(KEY).awaitSingle() shouldBe DataType.LIST
        ops.size(KEY).awaitSingle() shouldBe 4

        for (i in 0..<ops.size(KEY).awaitSingle()) {
            ops.index(KEY, i).awaitSingle().let {
                logger.debug { "$i: $it" }
            }
        }

        ops.range(KEY, 0, -1).asFlow().collect { logger.debug { it } }

        ops.range(KEY, 0, -1).toStream().forEach { logger.debug { it } }

        ops.range(KEY, 0, -1).asFlow().toList() shouldBe listOf(2, 3, 4, 5)

        ops.all(KEY) shouldBe listOf(2, 3, 4, 5)

        ops.rightPush(KEY, 6).awaitSingle() shouldBe listOf(2, 3, 4, 5, 6)

        ops.leftPop(KEY).awaitSingle() shouldBe listOf(3, 4, 5, 6)

        ops.leftPush(KEY, 9).awaitSingle()
        ops.rightPop(KEY).awaitSingle() shouldBe listOf(9, 3, 4, 5)
    }

    "LinkedList LRU" {
        val ops = template.opsForList()
        // LRU : 사용하면 배열의 제일 앞으로 이동
        ops.rightPushAll(KEY, 7, 6, 2, 3, 4, 5).awaitSingle()

        ops.remove(KEY, 0, 2).awaitSingle()
        ops.all(KEY) shouldBe listOf(7, 6, 3, 4, 5)
        ops.rightPop(KEY).awaitSingle()
        ops.leftPush(KEY, 2).awaitSingle()
        ops.all(KEY) shouldBe listOf(2, 7, 6, 3, 4)
    }

    "hash" {
        val ops = template.opsForHash<Int, String>()
        val map = (1..10).associateWith { "val-$it" }
        ops.putAll(KEY, map).awaitSingle()

        ops.size(KEY).awaitSingle() shouldBe 10
        ops.get(KEY, 1).awaitSingle() shouldBe "val-1"
    }

    "sorted set" {
        val ops = template.opsForZSet()
        listOf(8, 7, 1, 4, 13, 22, 9, 7, 8).forEach {
            // 날짜로 정의
            ops.add(KEY, "$it", -1.0 * Date().time).awaitSingle()
            ops.all(KEY).let { logger.debug { it } }
        }
        template.delete(KEY).awaitSingle()

        // 실시간 랭킹
        listOf(
            "jake" to 123,
            "chulsoo" to 752,
            "yeonghee" to 830,
            "john" to 234,
            "jake" to 623
        ).also {
            it.toMap().toList().sortedBy { it.second }.let { logger.debug { "original: $it" } }
        }.forEach {
            ops.add(KEY, it.first, it.second * -1.0).awaitSingle()
            ops.all(KEY).let { logger.debug { it } }
        }
    }

    "geo redis" {
        // 내 주변 택시
        // 배달 라이더 위치 등
        val ops = template.opsForGeo()
        // 이름/경도(longitude)/위도(latitude)
        ops.add(KEY, GeoLocation("seoul", Point(126.97806, 37.56667))).awaitSingle()
        listOf(
            GeoLocation("seoul", Point(126.97806, 37.56667)),
            GeoLocation("busan", Point(129.07556, 35.17944)),
            GeoLocation("incheon", Point(126.70528, 37.45639)),
            GeoLocation("daegu", Point(128.60250, 35.87222)),
            GeoLocation("anyang", Point(126.95556, 37.39444)),
            GeoLocation("daejeon", Point(127.38500, 36.35111)),
            GeoLocation("gwangju", Point(126.85306, 35.15972)),
            GeoLocation("suwon", Point(127.02861, 37.26389)),
        ).forEach {
            ops.add(KEY, it as GeoLocation<Any>).awaitSingle()
        }

        ops.distance(KEY, "seoul", "busan").awaitSingle().let { logger.debug { "seoul -> busan : $it METERS" } }

        val p = ops.position(KEY, "daegu").awaitSingle().also { logger.debug { it } }
        val circle = Circle(p, Distance(100.0, Metrics.KILOMETERS))
        ops.radius(KEY, circle).asFlow().map { it.content.name }.toList().let {
            logger.debug { "cities near daegu: $it" }
        }
    }

    "hyper loglog" {
        // 페이지 방문 건수 등에 유용
        val ops = template.opsForHyperLogLog()
        ops.add("page1", "192.178.0.23", "225.225.105.161").awaitSingle()
        ops.add("page2", "192.1.0.2", "225.225.7.161").awaitSingle()
        ops.add("page3", "3.178.0.6", "225.7.105.161").awaitSingle()
        ops.add("page3", "192.4.0.23", "4.225.105.8").awaitSingle()
        ops.add("page4", "7.4.0.23", "4.225.9.8").awaitSingle()

        ops.size("page3").awaitSingle().let { logger.debug { it } }

        val logs = (1..100_000).map { "$it" }.toTypedArray()
        ops.add(KEY, *logs).awaitSingle()

        // 원소 개수는 추정치이므로 정확하지 않다.
        ops.size(KEY).awaitSingle().let { logger.debug { it } }
    }

    "pub / sub" {
        template.listenToChannel("channel-1").doOnNext {
            logger.debug { ">> received-1: $it" }
        }.subscribe()

        template.listenToChannel("channel-1").asFlow().onEach {
            logger.debug { ">> received-2: $it" }
        }.launchIn(CoroutineScope(Dispatchers.Default))

        repeat(10) {
            val message = "test message (${it + 1})"
            logger.debug { ">> send: $message" }
            template.convertAndSend("channel-1", message).awaitSingle()
            delay(1000)
        }
    }
})

interface WithRedisContainer {
    companion object {
        private val container = GenericContainer(DockerImageName.parse("redis")).apply {
            addExposedPort(6379)
            start()
        }

        @DynamicPropertySource
        @JvmStatic // 스프링에서 호출해서 부를수 있게끔
        fun setProperty(registry: DynamicPropertyRegistry) {
            logger.debug { "redis mapped port: ${container.getMappedPort(6379)}" }
            // 도커에 컨테이너가 등록 되었을때 6379의 포트를 연결
            // 외부로 나가는 포트가 랜덤이기 때문
            registry.add("spring.data.redis.port") {
                "${container.getMappedPort(6379)}"
            }
        }
    }
}

suspend fun ReactiveZSetOperations<Any, Any>.all(key: Any): List<Any> {
    return this.range(key, Range.closed(0, -1)).asFlow().toList()
}

suspend fun ReactiveListOperations<Any, Any>.all(key: Any): List<Any> {
    return this.range(key, 0, -1).asFlow().toList()
}