package no.nav.kanal.camel

import no.difi.begrep.sdp.schema_v10.SDPDigitalPost
import no.difi.begrep.sdp.schema_v10.SDPManifest
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.LoggerFactory
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument
import java.io.StringReader
import java.util.Base64
import javax.xml.bind.JAXBContext
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamReader

class XmlExtractor : Processor {
    private val log = LoggerFactory.getLogger(XmlExtractor::class.java)
    private val xmlInputFactory = XMLInputFactory.newFactory()

    private val base64Decoder = Base64.getDecoder()
    private val sbdUnmarshaller = JAXBContext.newInstance(StandardBusinessDocument::class.java, SDPDigitalPost::class.java).createUnmarshaller()
    private val manifestUnmarshaller = JAXBContext.newInstance(SDPManifest::class.java).createUnmarshaller()

    override fun process(exchange: Exchange) {
        log.info("Extracting required metadata")
        val xmlReader = xmlInputFactory.createXMLStreamReader(StringReader(exchange.getIn().body()))

        var sbdFound = false
        var manifestFound = false
        var priorityFound = false
        var certificateFound = false

        while (!sbdFound || !manifestFound || !certificateFound || !priorityFound) {
            if (!xmlReader.hasNext()) {
               throw RuntimeException("Reached end of document without finding all required fields")
            }
            val event = xmlReader.next()
            if (event == XMLStreamReader.START_ELEMENT) {
                when (xmlReader.localName) {
                    STANDARD_BUSINESS_DOCUMENT -> {
                        log.info("SBD found")
                        exchange.getIn().setHeader(STANDARD_BUSINESS_DOCUMENT, sbdUnmarshaller.unmarshal(xmlReader, StandardBusinessDocument::class.java).value)
                        sbdFound = true
                    }
                    MANIFEST_HEADER -> {
                        log.info("Found manifest")
                        exchange.getIn().setHeader(MANIFEST_HEADER, manifestUnmarshaller.unmarshal(xmlReader, SDPManifest::class.java).value)
                        manifestFound = true
                    }
                    CERTIFICATE -> {
                        val b64 = StringBuilder()
                        while (xmlReader.next() != XMLStreamReader.END_ELEMENT) {
                            b64.append(xmlReader.text)
                        }
                        val b64Cert = base64Decoder.decode(b64.toString())
                        val cert = base64Decoder.decode(b64Cert)
                        exchange.getIn().setHeader(CERTIFICATE, cert)
                        log.info("Found certificate {}", b64)
                        certificateFound = true
                    }
                    HAS_PRIORITY -> {
                        while (xmlReader.next() != XMLStreamReader.CHARACTERS) { }
                        exchange.getIn().setHeader(HAS_PRIORITY, xmlReader.text == "true")
                        log.info("Found priority {}", xmlReader.text)
                        priorityFound = true
                    }
                }
            }
        }
    }

    companion object {
        const val STANDARD_BUSINESS_DOCUMENT = "standardBusinessDocument"
        const val MANIFEST_HEADER = "manifest"
        const val CERTIFICATE = "sertifikat"
        const val HAS_PRIORITY = "erPrioritert"
    }
}
