package com.example.advanced.service

import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.kotlin.circuitbreaker.CircuitBreakerConfig
import io.github.resilience4j.kotlin.circuitbreaker.executeSuspendFunction
import io.github.resilience4j.kotlin.ratelimiter.RateLimiterConfig
import io.github.resilience4j.kotlin.ratelimiter.executeSuspendFunction
import io.github.resilience4j.ratelimiter.RateLimiter
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBody
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private val logger = KotlinLogging.logger {}

@Service
class ExternalApi(
    @Value("\${api.externalUrl}")
    private val externaleUrl: String
) {

    private val client = WebClient.builder().baseUrl(externaleUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build()

    suspend fun delay() {
        return client.get().uri("/delay").retrieve().awaitBody()
    }

    suspend fun testCircuitBreaker(flag: String): String {
        logger.debug { "1. request call" }
        return try {
            rateLimiter.executeSuspendFunction {
                circuitBreaker.executeSuspendFunction {
                    logger.debug { "2. call external" }
                    client.get().uri("/test/circuit/child/${flag}").retrieve().awaitBody()
                }
            }
        } catch (e: CallNotPermittedException) {
            "Call later (blocked by circuit breaker)"
        } catch (e: RequestNotPermitted){
            "Call later (blocked by rate limiter)"
        }
    }

    /**
     * close : 회로가 닫힘 -> 정상
     * open : 회로가 열림 -> 차단
     * half-open : 반열림 -> 간 보기
     */
    val circuitBreaker = CircuitBreaker.of(
        "test",
        CircuitBreakerConfig {
            slidingWindowSize(10) // 요청 10개에 대해서
            failureRateThreshold(20.0F) // 20%가 에러나면 실패
            // opne(차단) 후 몇 초 후 close(열림) 상태로 변경 : 완전 close 아니라 half-open
            // 서킷브레이커가 열려 있는 상태지만 내부의 프로세스로 요청을 보내고 실패율을 측정해 상태를 CLOSED 혹은 OPEN 상태로 변경한다.
            waitDurationInOpenState(10.seconds.toJavaDuration())
            // half-open 상태에서 허용할 요청 수
            permittedNumberOfCallsInHalfOpenState(3)
        },
    )

    val rateLimiter = RateLimiter.of("rps-limiter", RateLimiterConfig {
        limitForPeriod(2) // 2번
        timeoutDuration(5.seconds.toJavaDuration()) // 얼마동안 2번할지 (5초동안 2번)
        limitRefreshPeriod(10.seconds.toJavaDuration()) // 10초 후에는 풀어줘
    })
}
