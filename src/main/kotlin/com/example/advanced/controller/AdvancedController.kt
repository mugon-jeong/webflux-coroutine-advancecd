package com.example.advanced.controller

import com.example.advanced.service.AdvancedService
import mu.KotlinLogging
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
private val logger = KotlinLogging.logger {  }
@RestController
class AdvancedController(
    private val service: AdvancedService,
) {

    @GetMapping("/test/mdc")
    suspend fun testRequestTxid(){
        logger.debug { "start MDC TxId" }
        service.mdc()
        logger.debug { "end MDC TxId" }
    }
}