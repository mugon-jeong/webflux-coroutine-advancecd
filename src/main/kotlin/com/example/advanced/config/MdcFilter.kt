package com.example.advanced.config

import io.micrometer.context.ContextRegistry
import org.slf4j.MDC
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.util.context.Context
import java.util.UUID

const val TX_ID = "txId"

/**
 * 모든 요청이 거치는 컴포넌트
 */
@Component
@Order(1) // 가장 먼저 실행
class MdcFilter : WebFilter {
    init {
        // reactor 체인간에 컨텍스트 전파 설정
        Hooks.enableAutomaticContextPropagation() // 퍼블리셔들 간에 체인이 이어지면서 전달이 될때 퍼블리셔들간에 컨텍스트들이 복사가 됨
        ContextRegistry.getInstance().registerThreadLocalAccessor( // 컨텍스트들이 복사될때 무엇이 복사될지 설정
            TX_ID, // 어떤 컨텍스트 복사할지
            { MDC.get(TX_ID) }, // 무슨 값을 읽어올것인지
            { value -> MDC.put(TX_ID, value) }, // 읽어온 값을 그 이후 체인에 등록
            { MDC.remove(TX_ID) } // subscribe되면 제거
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val uuid = exchange.request.headers["x-txid"]?.firstOrNull() ?: "${UUID.randomUUID()}"
        MDC.put(TX_ID, uuid)
        // reactor로 context 전파
        return chain.filter(exchange).contextWrite {
            // MDC 체인들 간에 context 유통
            Context.of(TX_ID, uuid)
        }
    }
}