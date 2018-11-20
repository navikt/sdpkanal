package no.nav.kanal.camel

import no.digipost.api.representations.EbmsPullRequest
import no.digipost.api.xml.Marshalling
import no.nav.kanal.camel.ebms.DigipostEbms
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class EbmsPull @Autowired constructor(
        val digipostEbms: DigipostEbms
) : Processor {
    private val log = LoggerFactory.getLogger(EbmsPull::class.java)

    override fun process(exchange: Exchange) {
        val sender = digipostEbms.messageSender

        val receipt = sender.hentKvittering(EbmsPullRequest(digipostEbms.databehandler, exchange.getIn().header<String>(MPC_ID)))

        if (receipt != null) {
            sender.bekreft(receipt)
            val bytes = ByteArrayOutputStream()
            Marshalling.getMarshallerSingleton().jaxbContext.createMarshaller().marshal(receipt.sbd, bytes)
            exchange.`in`.body = bytes
        }
    }

    companion object {
        const val MPC_ID: String = "MPC_ID"
    }
}
