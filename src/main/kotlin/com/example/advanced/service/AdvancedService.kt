package com.example.advanced.service

import com.example.advanced.repository.ArticleRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.reactor.awaitSingle
import mu.KotlinLogging
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers

private val logger = KotlinLogging.logger { }

@Service
class AdvancedService(
    private val repository: ArticleRepository
) {

    suspend fun mdc() {
        logger.debug { "start mdc 1" }
//        mdc2()
        logger.debug { "end mdc 1" }
    }
}
