package no.nav.kanal.camel

import com.fasterxml.jackson.databind.ObjectMapper
import no.digipost.api.representations.EbmsPullRequest
import no.digipost.api.xml.Marshalling
import no.nav.kanal.camel.ebms.DigipostEbms
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream

@Service
class EbmsPull @Autowired constructor(
        @Value("\${ebms.mpc.normal}")
        val mpcNormal: String,
        @Value("\${ebms.mpc.prioritert}")
        val mpcPrioritert: String,
        val digipostEbms: DigipostEbms
) : Processor {
    private val log = LoggerFactory.getLogger(EbmsPull::class.java)

    override fun process(exchange: Exchange) {
        val sender = digipostEbms.messageSender

        val receipt = sender.hentKvittering(EbmsPullRequest(digipostEbms.databehandler, mpcNormal))

        if (receipt != null) {
            sender.bekreft(receipt)
            val bytes = ByteArrayOutputStream()
            Marshalling.getMarshallerSingleton().jaxbContext.createMarshaller().marshal(receipt.sbd, bytes)
            exchange.`in`.body = bytes

            log.info(ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(receipt.sbd))
        }
    }

}
