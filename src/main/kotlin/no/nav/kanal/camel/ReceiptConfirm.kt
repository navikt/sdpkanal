package no.nav.kanal.camel

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.kanal.ebms.EbmsReceipt
import no.nav.kanal.ebms.EbmsSender
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.Logger
import org.slf4j.LoggerFactory

const val RECEIPT_CONFIRMATION_HEADER = "receipt_confirmation_header"

class ReceiptConfirm(private val ebmsSender: EbmsSender) : Processor {
    val log: Logger = LoggerFactory.getLogger("receipt-confirmer")

    override fun process(exchange: Exchange) {
        val receipt: EbmsReceipt = exchange.getIn().header(RECEIPT_CONFIRMATION_HEADER)
        log.info("Confirming receipt: {} {}",
                keyValue("conversationId", receipt.conversationId),
                keyValue("refToMessageId", receipt.refToMessageId))
        ebmsSender.confirmReceipt(receipt)
    }

}
