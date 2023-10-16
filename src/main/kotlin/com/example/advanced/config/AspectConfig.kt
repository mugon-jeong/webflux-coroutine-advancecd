package com.example.advanced.config

import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.core.KotlinDetector
import org.springframework.stereotype.Component
import kotlin.coroutines.Continuation

private val logger = KotlinLogging.logger { }
@Aspect
@Component
class AspectConfig {
    @Around(
        """
        @annotation(org.springframework.web.bind.annotation.GetMapping)||
        @annotation(org.springframework.web.bind.annotation.RequestMapping)||
        @annotation(org.springframework.web.bind.annotation.PostMapping)||
        @annotation(org.springframework.web.bind.annotation.PutMapping)||
        @annotation(org.springframework.web.bind.annotation.DeleteMapping)
    """
    )
    fun wrapCoroutinController(jp: ProceedingJoinPoint): Any? {
//        logger.debug { ">> before wrapper" }
//        val method = (jp.signature as MethodSignature).method
        // coroutine 함수인지 체크
//        return if (KotlinDetector.isSuspendingFunction(method)) {
        return if (jp.hasSuspendFunction) {
//            try {
//                logger.debug { ">> method: ${jp.signature}" }
//                logger.debug { ">> arg: ${jp.args.toList()}" }
//                logger.debug { "in suspend function" }
            val continuation = jp.args.last() as Continuation<*>
            val newContext = continuation.context + MDCContext()
            val newContinuation = Continuation(newContext) { continuation.resumeWith(it) }
            val newArgs = jp.args.dropLast(1) + newContinuation
            jp.proceed(newArgs.toTypedArray())
//            } finally {
//                logger.debug { ">> after wrapper" }
//            }
        } else {
//            logger.debug { "in non-suspend function" }
            jp.proceed()
        }
    }

    /**
     * 함수 확장
     * val method = (jp.signature as MethodSignature).method
     * if (KotlinDetector.isSuspendingFunction(method)) 부분을 확장함수로 처리
     */
    private val ProceedingJoinPoint.hasSuspendFunction: Boolean
        get() {
            val method = (this.signature as MethodSignature).method
            return KotlinDetector.isSuspendingFunction(method)
        }
}