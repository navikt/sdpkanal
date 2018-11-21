package no.nav.kanal.camel

import com.jcraft.jsch.ChannelSftp
import no.difi.begrep.sdp.schema_v10.SDPDokument
import no.difi.begrep.sdp.schema_v10.SDPManifest
import no.difi.sdp.client2.asice.AsicEAttachable
import no.difi.sdp.client2.asice.archive.CreateZip
import no.difi.sdp.client2.asice.manifest.Manifest
import no.difi.sdp.client2.asice.signature.CreateSignature
import no.difi.sdp.client2.domain.Sertifikat
import no.difi.sdp.client2.internal.CreateCMSDocument
import no.digipost.api.representations.Dokumentpakke
import no.digipost.api.xml.Schemas
import no.nav.kanal.config.SdpKeys
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Base64
import javax.xml.transform.stream.StreamResult

class DocumentPackageCreator @Autowired constructor(
        private val sdpKeys: SdpKeys,
        private val sftpChannel: ChannelSftp,
        private val documentDirectory: String
) : Processor {
    val log: Logger = LoggerFactory.getLogger(DocumentPackageCreator::class.java)
    private val createCMSDocument = CreateCMSDocument()
    private val marshaller: Jaxb2Marshaller = Jaxb2Marshaller().apply {
        setClassesToBeBound(SDPManifest::class.java)
        setSchema(Schemas.SDP_MANIFEST_SCHEMA)
        afterPropertiesSet()
    }

    private val createSignature = CreateSignature()
    private val createZip = CreateZip()

    override fun process(exchange: Exchange) {
        val manifest = exchange.getIn().getHeader(XmlExtractor.MANIFEST_HEADER, SDPManifest::class.java)
        val manifestFiles = listOf(listOf(manifest.hoveddokument), manifest.vedleggs).flatten()
        val files: MutableList<AsicEAttachable> = manifestFiles.map { it.toAttachable() }.toMutableList()

        // TODO: Do some cleanup, this could be done better
        manifestFiles.forEach { it.href = it.href.split("/").last()  }

        val asicEManifest = Manifest(ByteArrayOutputStream().use {
            marshaller.marshal(manifest, StreamResult(it))
            it.toByteArray()
        })

        files.add(createSignature.createSignature(sdpKeys.keypair, files))
        files.add(asicEManifest)

        // TODO: We probably don't need to get the billable bytes, might be interesting for grafana?
        val bytes = files.map { it.bytes.size.toLong() }.sum()

        val archive = createZip.zipIt(files)

        val certificate = Sertifikat.fraByteArray(exchange.getIn().getHeader(XmlExtractor.CERTIFICATE, ByteArray::class.java))
        val encryptedASiCE = createCMSDocument.createCMS(archive.bytes, certificate)
        log.info("Encrypted package with {} bytes, is now {}, used certificate {}",
                archive.bytes.size,
                encryptedASiCE.bytes.size,
                Base64.getEncoder().encodeToString(Base64.getEncoder().encode(certificate.encoded)))

        log.info("Certificate information: {}", certificate.x509Certificate)

        exchange.getIn().setHeader(DOCUMENT_PACKAGE, Dokumentpakke(encryptedASiCE.bytes))
    }

    companion object {
        const val DOCUMENT_PACKAGE = "DOCUMENT_PACKAGE"
    }

    private fun SDPDokument.toAttachable() = SDPDokumentAsicEWrapper(this, sftpChannel, documentDirectory)

    class SDPDokumentAsicEWrapper(
            private val sdpDokument: SDPDokument,
            sftpChannel: ChannelSftp, documentDirectory: String
    ) : AsicEAttachable {
        private val documentBytes: ByteArray = ByteArrayOutputStream().use {
            sftpChannel.get(documentDirectory + sdpDokument.href, it)
            it.toByteArray()
        }

        override fun getFileName(): String = sdpDokument.href
        override fun getMimeType(): String = sdpDokument.mime
        override fun getBytes(): ByteArray = documentBytes
    }
}
