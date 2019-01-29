package no.nav.kanal.camel

import net.logstash.logback.argument.StructuredArgument
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.kanal.SdpPayload
import org.apache.camel.Exchange
import org.apache.camel.Processor

const val MESSAGE_ID_HEADER = "MESSAGE_ID_HEADER"
const val CONVERSATION_ID_HEADER = "CONVERSATION_ID_HEADER"
const val LOGGING_KEYS_HEADER = "LOGGING_KEYS_HEADER"
const val LOGGING_VALUES_HEADER = "LOGGING_VALUES_HEADER"

class MetadataExtractor : Processor {
    override fun process(exchange: Exchange) {
        val sbd = exchange.getIn().getHeader(XmlExtractor.SDP_PAYLOAD, SdpPayload::class.java).standardBusinessDocument

        val messageId = sbd.standardBusinessDocumentHeader.documentIdentification.instanceIdentifier
        val conversationId = sbd.standardBusinessDocumentHeader.businessScope.scopes.first().instanceIdentifier
        exchange.getIn().setHeader(MESSAGE_ID_HEADER, messageId)
        exchange.getIn().setHeader(CONVERSATION_ID_HEADER, conversationId)
        val loggingValues = arrayOf(
                keyValue("callId", messageId),
                keyValue("messageId", messageId),
                keyValue("conversationId", conversationId)
        )
        exchange.getIn().setHeader(LOGGING_VALUES_HEADER, loggingValues)
        exchange.getIn().setHeader(LOGGING_KEYS_HEADER, loggingValues.joinToString(", ", "(", ")") { "{}" })
    }
}

fun Exchange.loggingKeys(): String = getIn().header(LOGGING_KEYS_HEADER)
fun Exchange.loggingValues(): Array<StructuredArgument> = getIn().header(LOGGING_VALUES_HEADER)
