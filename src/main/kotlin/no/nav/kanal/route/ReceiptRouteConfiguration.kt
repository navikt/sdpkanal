package no.nav.kanal.route

import no.nav.kanal.camel.EbmsPull
import no.nav.kanal.config.MPC_ID
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint

fun CamelContext.createReceiptPollingRoute(
        routeName: String,
        mpcId: String,
        pollDelay: Long,
        ebmsPull: EbmsPull,
        receiptQueue: JmsEndpoint
) = object: RouteBuilder(this) {
    override fun configure() {
        // @formatter:off
        from("timer://${routeName}Timer")
                .id(routeName)
                .setHeader(MPC_ID) { mpcId }
                .process(ebmsPull)
                .choice()
                .`when`(body().isNotNull).to(receiptQueue).log("Received receipt")
                .otherwise().log("Polling $routeName returned empty result").delay(pollDelay)
                .endChoice()
                .end()
        // @formatter:on
    }

}
