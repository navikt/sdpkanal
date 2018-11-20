package no.nav.kanal.config

import no.nav.kanal.camel.EbmsPull
import no.nav.kanal.route.ReceiptPollingRoute
import org.apache.camel.component.jms.JmsEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class RouteConfig @Autowired constructor(
        val ebmsPull: EbmsPull,
        val receiptQueue: JmsEndpoint,
        val receiptPriorityQueue: JmsEndpoint,
        @Value("\${ebms.pullinterval.normal}") val receiptPollIntervalNormal: Long,
        @Value("\${ebms.pullinterval.normal}") val receiptPollIntervalPriority: Long,
        @Value("\${ebms.mpc.normal}") val mpcNormal: String,
        @Value("\${ebms.mpc.prioritert}") val mpcPrioritert: String
) {

    @Bean
    open fun pullReceiptsNormal(): ReceiptPollingRoute =
            ReceiptPollingRoute("pullReceiptsNormal", mpcNormal, ebmsPull, receiptQueue, receiptPollIntervalNormal)
    @Bean
    open fun pullReceiptsPriority(): ReceiptPollingRoute =
            ReceiptPollingRoute("pullReceiptsPriority", mpcPrioritert, ebmsPull, receiptPriorityQueue, receiptPollIntervalPriority)
}
