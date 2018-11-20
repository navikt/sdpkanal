package no.nav.kanal.camel

import no.digipost.api.MessageSender
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsPullRequest
import no.digipost.api.xml.Marshalling
import no.nav.kanal.config.MPC_ID
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream

class EbmsPull constructor(
        val messageSender: MessageSender,
        val databehandler: EbmsAktoer
) : Processor {
    private val log = LoggerFactory.getLogger(EbmsPull::class.java)

    override fun process(exchange: Exchange) {
        val sender = messageSender

        val receipt = sender.hentKvittering(EbmsPullRequest(databehandler, exchange.getIn().header<String>(MPC_ID)))

        if (receipt != null) {
            sender.bekreft(receipt)
            val bytes = ByteArrayOutputStream()
            Marshalling.getMarshallerSingleton().jaxbContext.createMarshaller().marshal(receipt.sbd, bytes)
            exchange.`in`.body = bytes
        }
    }
}
