package com.example.advanced.controller

import com.example.advanced.service.AdvancedService
import kotlinx.coroutines.delay
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.KotlinDetector
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.coroutines.Continuation

private val logger = KotlinLogging.logger { }

@RestController
class AdvancedController(
    private val service: AdvancedService,
) {

    @GetMapping("/test/mdc")
    suspend fun testRequestTxid() {
        // 로깅을 하나의 스레드에서 처리
//        withContext(MDCContext()) {
        logger.debug { "start MDC TxId" }
        delay(100)
        service.mdc()
        logger.debug { "end MDC TxId" }
//        }
    }

    @GetMapping("/test/mdc2")
    fun testAnother() {
        logger.debug { "test anothoer!!" }
    }
}