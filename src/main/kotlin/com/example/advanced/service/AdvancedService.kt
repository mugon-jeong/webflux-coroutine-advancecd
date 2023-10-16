package com.example.advanced.service

import mu.KotlinLogging
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger { }

@Service
class AdvancedService {

    fun mdc() {
        logger.debug { "mdc service !" }
    }
}
