package no.nav.kanal.route

import no.nav.kanal.camel.BOQLogger
import no.nav.kanal.camel.BackoutReason
import no.nav.kanal.camel.DocumentPackageCreator
import no.nav.kanal.camel.XmlExtractor
import no.nav.kanal.camel.ebms.EbmsPush
import org.apache.camel.LoggingLevel
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jms.JmsEndpoint
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MessagePushRouteConfiguration @Autowired constructor(
        private val boqLogger: BOQLogger,
        private val backoutReason: BackoutReason,
        private val xmlExtractor: XmlExtractor,
        private val documentPackageCreator: DocumentPackageCreator,
        private val ebmsPush: EbmsPush,
        private val inputQueueNormal: JmsEndpoint,
        private val inputQueuePriority: JmsEndpoint,
        private val inputQueueNormalBackout: JmsEndpoint,
        private val inputQueuePriorityBackout: JmsEndpoint
) {
    @Bean
    open fun backoutQueueNormal() = createDeadLetterRoute("backoutMessageNormal", inputQueueNormalBackout)
    @Bean
    open fun backoutQueuePriority() = createDeadLetterRoute("backoutMessagePriority", inputQueuePriorityBackout)

    @Bean
    open fun sendSDPNormal() = createSendRoute("sendSDPNormal", inputQueueNormal, "backoutMessageNormal")
    @Bean
    open fun sendSDPPriority() = createSendRoute("sendSDPPriority", inputQueuePriority, "backoutMessagePriority")

    private fun createDeadLetterRoute(routeName: String, backoutQueue: JmsEndpoint) = object: RouteBuilder() {
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

    private fun createSendRoute(routeName: String, inputQueue: JmsEndpoint, backoutRoute: String) = object: RouteBuilder() {
        override fun configure() {
            // @formatter:off
            from(inputQueue)
                    .id(routeName)
                    .onException(Exception::class.java).handled(true).to("direct:$backoutRoute")
                    .end()
                    .process(xmlExtractor)
                    .process(documentPackageCreator)
                    .process(ebmsPush)
            // @formatter:on
        }

    }
}
