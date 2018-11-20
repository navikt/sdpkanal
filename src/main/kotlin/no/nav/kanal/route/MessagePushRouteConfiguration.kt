package no.nav.kanal.route

import no.nav.kanal.camel.BOQLogger
import no.nav.kanal.camel.BackoutReason
import no.nav.kanal.camel.DocumentPackageCreator
import no.nav.kanal.camel.XmlExtractor
import no.nav.kanal.camel.ebms.EbmsPush
import no.nav.kanal.config.MPC_ID
import org.apache.camel.CamelContext
import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint

fun CamelContext.createDeadLetterRoute(
        routeName: String,
        backoutQueue: JmsEndpoint,
        boqLogger: BOQLogger,
        backoutReason: BackoutReason
) = object: RouteBuilder(this) {
    override fun configure() {
        // @formatter:off
        from("direct:$routeName")
                .log(LoggingLevel.ERROR, "EXCEPTION: Log and send to DLQ")
                //.to("log:no.nav.kanal?level=ERROR&amp;showAll=true&amp;showCaughtException=true&amp;showStackTrace=true")
                .process(boqLogger)
                .process(backoutReason)
                .to(backoutQueue)
        // @formatter:on
    }
}

fun CamelContext.createSendRoute(
        routeName: String,
        mpcId: String,
        inputQueue: JmsEndpoint,
        backoutRoute: String,
        xmlExtractor: XmlExtractor,
        documentPackageCreator: DocumentPackageCreator,
        ebmsPush: EbmsPush
) = object: RouteBuilder(this) {
    override fun configure() {
        // @formatter:off
        from(inputQueue)
                .id(routeName)
                .setHeader(MPC_ID) { mpcId }
                .onException(Exception::class.java).handled(true).to("direct:$backoutRoute")
                .end()
                .process(xmlExtractor)
                .process(documentPackageCreator)
                .process(ebmsPush)
        // @formatter:on
    }

}
