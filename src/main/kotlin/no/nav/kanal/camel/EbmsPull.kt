package no.nav.kanal.camel

import no.digipost.api.MessageSender
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsPullRequest
import no.nav.kanal.config.MPC_ID_HEADER
import no.nav.kanal.config.PRIORITY_HEADER
import no.nav.kanal.ebms.EbmsSender
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory

class EbmsPull constructor(
    messageSender: MessageSender,
    private val databehandler: EbmsAktoer,
    private val ebmsSender: EbmsSender = EbmsSender.fromMessageSender(messageSender)
) : Processor {
    private val log = LoggerFactory.getLogger(EbmsPull::class.java)

    override fun process(exchange: Exchange) {
        val receipt = ebmsSender.fetchReceipt(EbmsPullRequest(databehandler, exchange.getIn().header(PRIORITY_HEADER), exchange.getIn().header(MPC_ID_HEADER)))

        if (receipt != null) {
            ebmsSender.confirmReceipt(receipt)

            if (log.isDebugEnabled) {
                log.debug("Receipt content ${receipt.sbdBytes.toString(Charsets.UTF_8)}")
            }
            exchange.`in`.body = receipt.sbdBytes
        }
    }
}
