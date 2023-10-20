package com.example.advanced.controller

import com.example.advanced.config.validator.DateString
import com.example.advanced.service.AdvancedService
import com.example.advanced.service.ExternalApi
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import kotlinx.coroutines.delay
import mu.KotlinLogging
import org.springframework.web.bind.annotation.*

private val logger = KotlinLogging.logger { }

@RestController
class AdvancedController(
    private val service: AdvancedService,
    private val externalApi: ExternalApi,
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

    // webflux에서는 BindingResult를 지원하지 않음
    @PutMapping("/test/error")
    suspend fun error(@RequestBody @Valid request: ReqErrorTest) {
//        logger.debug { "request" }

//        if (request.message == "error") {
//            throw InvalidParameter(request, request::message, code = "custom code", message = "custom error")
//        }

//        throw RuntimeException("yahoo !!")

    }

    @GetMapping("/external/delay")
    suspend fun delay() {
        externalApi.delay()
    }

    @GetMapping("/external/circuit/{flag}")
    suspend fun testCircuitBreaker(@PathVariable flag: String): String {
        return externalApi.testCircuitBreaker(flag)
    }
}

data class ReqErrorTest(
    @field:NotEmpty
    val id: String?,
    val age: Int?,
    @field:DateString
    val birthDay: String?,
    val message: String? = null
)