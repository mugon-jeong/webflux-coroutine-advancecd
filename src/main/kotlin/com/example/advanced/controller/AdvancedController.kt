package com.example.advanced.controller

import com.example.advanced.config.validator.DateString
import com.example.advanced.exception.InvalidParameter
import com.example.advanced.service.AdvancedService
import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty
import kotlinx.coroutines.delay
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.KotlinDetector
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.coroutines.Continuation
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

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

    // webflux에서는 BindingResult를 지원하지 않음
    @PutMapping("/test/error")
    suspend fun error(@RequestBody @Valid request: ReqErrorTest) {
//        logger.debug { "request" }

//        if (request.message == "error") {
//            throw InvalidParameter(request, request::message, code = "custom code", message = "custom error")
//        }

//        throw RuntimeException("yahoo !!")

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