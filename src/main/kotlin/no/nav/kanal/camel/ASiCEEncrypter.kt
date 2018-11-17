package no.nav.kanal.camel

import no.difi.sdp.client2.domain.Sertifikat
import no.difi.sdp.client2.internal.CreateCMSDocument
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Qualifier("asiceEncrypter")
@Service
class ASiCEEncrypter : Processor {
    private val createCMSDocument = CreateCMSDocument()

    override fun process(exchange: Exchange) {
        val asice = exchange.getIn().getHeader(AsicECreator.SIGNED_ASICE, ByteArray::class.java)
        val certificate = Sertifikat.fraByteArray(exchange.getIn().getHeader(XmlExtractor.CERTIFICATE, ByteArray::class.java))
        val encryptedASiCE = createCMSDocument.createCMS(asice, certificate)
        exchange.getIn().setHeader(ENCRYPTED_ASICE, encryptedASiCE.bytes)
    }

    companion object {
        const val ENCRYPTED_ASICE = "ENCRYPTED_ASiCE"
    }
}
