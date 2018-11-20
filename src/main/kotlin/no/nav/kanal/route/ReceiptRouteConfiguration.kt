package no.nav.kanal.route

import no.nav.kanal.camel.EbmsPull
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class ReceiptRouteConfiguration @Autowired constructor(
        val ebmsPull: EbmsPull,
        val receiptQueueNormal: JmsEndpoint,
        val receiptQueuePriority: JmsEndpoint,
        @Value("\${ebms.pullinterval.normal}") val receiptPollIntervalNormal: Long,
        @Value("\${ebms.pullinterval.normal}") val receiptPollIntervalPriority: Long,
        @Value("\${ebms.mpc.normal}") val mpcNormal: String,
        @Value("\${ebms.mpc.prioritert}") val mpcPrioritert: String
){
    @Bean
    open fun pullReceiptsNormal(): RouteBuilder = createReceiptPollingRoute("pullReceiptsNormal",
            mpcNormal, receiptPollIntervalNormal, ebmsPull, receiptQueueNormal)
    @Bean
    open fun pullReceiptsPriority(): RouteBuilder = createReceiptPollingRoute("pullReceiptsPriority",
            mpcPrioritert, receiptPollIntervalPriority, ebmsPull, receiptQueuePriority)

    private fun createReceiptPollingRoute(
            routeName: String,
            mpcId: String,
            pollDelay: Long,
            ebmsPull: EbmsPull,
            receiptQueue: JmsEndpoint
    ) = object: RouteBuilder() {
        override fun configure() {
            // @formatter:off
            from("timer://${routeName}Timer")
                    .id(routeName)
                    .setHeader(EbmsPull.MPC_ID) { mpcId }
                    .process(ebmsPull)
                    .choice()
                    .`when`(body().isNotNull).to(receiptQueue).log("Received receipt")
                    .otherwise().log("Polling $routeName returned empty result").delay(pollDelay)
                    .endChoice()
                    .end()
            // @formatter:on
        }

    }
}
