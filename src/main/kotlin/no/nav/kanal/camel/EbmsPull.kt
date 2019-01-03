package no.nav.kanal.camel

import no.digipost.api.MessageSender
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsPullRequest
import no.nav.kanal.config.MPC_ID
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
        val receipt = ebmsSender.fetchReceipt(EbmsPullRequest(databehandler, exchange.getIn().header<String>(MPC_ID)))

        if (receipt != null) {
            ebmsSender.confirmReceipt(receipt)
            //val bytes = IOUtils.toByteArray(receipt.sbdStream)

            //val bytes = ByteArrayOutputStream().use {
            //    Marshalling.getMarshallerSingleton().jaxbContext.createMarshaller().apply {
            //        setProperty(Marshaller.JAXB_FRAGMENT, true)
            //    }.marshal(receipt.sbd, it)
            //    it
            //}.toByteArray()
            if (log.isDebugEnabled) {
                log.debug("Receipt content ${receipt.sbdBytes.toString(Charsets.UTF_8)}")
            }
            exchange.`in`.body = receipt.sbdBytes
        }
    }
}
