package com.example.advanced.config

import com.example.advanced.config.extension.txid
import mu.KotlinLogging
import org.slf4j.MDC
import org.springframework.boot.web.error.ErrorAttributeOptions
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.server.ServerRequest

private val logger = KotlinLogging.logger { }

@Configuration
class ErrorConfig {
    @Bean
    fun errorAttribute(): DefaultErrorAttributes {
        return object : DefaultErrorAttributes() {
            override fun getErrorAttributes(
                serverRequest: ServerRequest,
                options: ErrorAttributeOptions? // application.yml의 server.error 옵션과 동일
            ): MutableMap<String, Any> {
                val request = serverRequest.exchange().request
                val txId = request.txid ?: ""
                MDC.put(TX_ID, txId)
                try {
                    // request마다 고유의 오브젝트의 해시를 가지고 있음, 인스턴스별로 유효
                    logger.debug { "request id: ${serverRequest.exchange().request.id}" }

                    super.getError(serverRequest).let { e ->
                        logger.error(e.message ?: "Internal Server Error", e)
                    }
                    return super.getErrorAttributes(serverRequest, options).apply {
                        remove("requestId")
                        put(TX_ID, txId)
                    }
                } finally {
                    request.txid = null
                    MDC.remove(TX_ID)
                }
            }
        }
    }
}