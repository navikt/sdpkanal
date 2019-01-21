package no.nav.kanal.route

import no.digipost.api.representations.EbmsOutgoingMessage
import no.nav.kanal.camel.BackoutReason
import no.nav.kanal.camel.DocumentPackageCreator
import no.nav.kanal.camel.XmlExtractor
import no.nav.kanal.camel.EbmsPush
import no.nav.kanal.camel.MetadataExtractor
import no.nav.kanal.camel.SdpPriceStatisticsCollector
import no.nav.kanal.config.MPC_ID_HEADER
import no.nav.kanal.config.PRIORITY_HEADER
import org.apache.camel.CamelContext
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint
import org.apache.camel.processor.RedeliveryPolicy

fun CamelContext.createDeadLetterRoute(
        routeName: String,
        backoutQueue: JmsEndpoint,
        backoutReason: BackoutReason
) = object: RouteBuilder(this) {
    override fun configure() {
        // @formatter:off
        from("direct:$routeName")
                .process(backoutReason)
                .to(backoutQueue)
        // @formatter:on
    }
}

fun CamelContext.createSendRoute(
        routeName: String,
        mpcId: String,
        priority: EbmsOutgoingMessage.Prioritet,
        inputQueue: JmsEndpoint,
        backoutRoute: String,
        documentPackageCreator: DocumentPackageCreator,
        ebmsPush: EbmsPush
) = object: RouteBuilder(this) {
    override fun configure() {
        // @formatter:off
        from(inputQueue)
                .id(routeName)
                .setHeader(PRIORITY_HEADER) { priority }
                .setHeader(MPC_ID_HEADER) { mpcId }
                //.errorHandler(deadLetterChannel("direct:$backoutRoute")
                //        .useOriginalMessage()
                //        .maximumRedeliveries(5)
                //        .redeliveryDelay(5000))
                .onException(Exception::class.java).handled(true).to("direct:$backoutRoute")
                .end()
                .process(XmlExtractor())
                .process(MetadataExtractor())
                .process(documentPackageCreator)
                .process(ebmsPush)
                .process(SdpPriceStatisticsCollector())
        // @formatter:on
    }

}
