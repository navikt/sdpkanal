package no.nav.kanal.route

import no.nav.kanal.camel.EbmsPull
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ReceiptPollingRoute constructor(
        @Autowired val ebmsPull: EbmsPull,
        @Autowired val receiptQueue: JmsEndpoint//,
        //@Value("\${ebms.poll.delay}") val pollDelay: String
) : RouteBuilder() {
    init {
        log.info("Creating receipt poller")
    }
    override fun configure() {
        from("timer://sdpkanalReceiptPoll")
                .process(ebmsPull)
                .choice()
                .`when`(body().isNotNull)
                .log("Received receipt")
                .to(receiptQueue)
                .otherwise()
                .log("Poll returned empty result")
                .delay(5000L)
                .endChoice()
                .end()
    }

}
