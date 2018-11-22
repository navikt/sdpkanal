package no.nav.kanal.camel

import no.nav.kanal.SdpPayload
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import java.io.StringReader
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

class XmlExtractor : Processor {
    private val log = LoggerFactory.getLogger(XmlExtractor::class.java)
    private val xmlInputFactory = XMLInputFactory.newFactory()

    private val payloadUnmarshaller = JAXBContext.newInstance(SdpPayload::class.java)

    override fun process(exchange: Exchange) {
        log.info("Extracting required metadata")
        exchange.`in`.setHeader(SDP_PAYLOAD, extractPayload(exchange.getIn().body()))
    }

    fun extractPayload(xml: String): SdpPayload = xmlInputFactory.createXMLStreamReader(StringReader(xml)).let {
        // Seek until we find the sendDigitalPostRequest
        while (it.hasNext() && (it.next() != XMLStreamReader.START_ELEMENT || it.localName != "sendDigitalPostRequest")) {}
        payloadUnmarshaller.createUnmarshaller().unmarshal(it, SdpPayload::class.java).value
    }

    companion object {
        const val SDP_PAYLOAD = "sdpPayload"
    }
}
