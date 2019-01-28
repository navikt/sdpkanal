package no.nav.kanal.camel

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsPullRequest
import no.nav.kanal.MPC_ID_HEADER
import no.nav.kanal.PRIORITY_HEADER
import no.nav.kanal.ebms.EbmsSender
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class EbmsPull constructor(
    private val databehandler: EbmsAktoer,
    private val ebmsSender: EbmsSender
) : Processor {
    private val log = LoggerFactory.getLogger(EbmsPull::class.java)

    override fun process(exchange: Exchange) {
        val receipt = ebmsSender.fetchReceipt(EbmsPullRequest(databehandler, exchange.getIn().header(PRIORITY_HEADER), exchange.getIn().header(MPC_ID_HEADER)))

        if (receipt != null) {
            val loggingValues = arrayOf(
                    keyValue("callId", receipt.conversationId),
                    keyValue("messageId", receipt.meldingsId),
                    keyValue("conversationId", receipt.conversationId)
            )
            val loggingKeys = loggingValues.joinToString(", ", "(", ")") { "{}" }

            log.info("Received a receipt $loggingKeys", loggingValues)

            ebmsSender.confirmReceipt(receipt)

            if (log.isDebugEnabled) {
                log.debug("Receipt content ${receipt.sbdBytes.toString(Charsets.UTF_8)}")
            }
            exchange.`in`.body = receipt.sbdBytes
        }
    }
}
