package no.nav.kanal.route

import no.nav.kanal.camel.EbmsPull
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint

class ReceiptPollingRoute constructor(
        val routeName: String,
        val mpcId: String,
        val ebmsPull: EbmsPull,
        val receiptQueue: JmsEndpoint,
        val pollDelay: Long
) : RouteBuilder() {
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
