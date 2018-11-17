package no.nav.kanal.camel

import no.difi.begrep.sdp.schema_v10.SDPDokument
import no.difi.begrep.sdp.schema_v10.SDPManifest
import no.difi.sdp.client2.asice.AsicEAttachable
import no.difi.sdp.client2.asice.archive.CreateZip
import no.difi.sdp.client2.asice.manifest.Manifest
import no.difi.sdp.client2.asice.signature.CreateSignature
import no.nav.kanal.config.SdpKeys
import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.springframework.beans.factory.annotation.Autowired
import no.digipost.api.xml.Schemas
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.stereotype.Service
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.transform.stream.StreamResult

val log = LoggerFactory.getLogger(AsicECreator::class.java)

@Service
class AsicECreator @Autowired constructor(
        val sdpKeys: SdpKeys,
        @Value("\${no.nav.kanal.dokument.path.prefix}") documentDirectory: String
): Processor {
    private val marshaller: Jaxb2Marshaller = Jaxb2Marshaller().apply {
        setClassesToBeBound(SDPManifest::class.java)
        setSchema(Schemas.SDP_MANIFEST_SCHEMA)
        afterPropertiesSet()
    }

    private val createSignature = CreateSignature()
    private val createZip = CreateZip()
    private val documentReceiver = DocumentReceiver(documentDirectory)

    override fun process(exchange: Exchange) {
        val manifest = exchange.getIn().getHeader(XmlExtractor.MANIFEST_HEADER, SDPManifest::class.java)
        val asicEManifest = Manifest(ByteArrayOutputStream().use {
            marshaller.marshal(manifest, StreamResult(it))
            it.toByteArray()
        })

        val files = listOf(
                manifest.vedleggs.map { it.toAttachable() },
                listOf(manifest.hoveddokument.toAttachable(), asicEManifest)

        ).flatten().toMutableList()

        files.add(createSignature.createSignature(sdpKeys.keypair, files))

        // TODO: We probably don't need to get the billable bytes, might be interesting for grafana?
        val bytes = files.map { it.bytes.size.toLong() }.sum()

        val archive = createZip.zipIt(files)
        exchange.getIn().setHeader(ASICE_CONTAINER, AsicE(archive.bytes, bytes))
    }

    private fun SDPDokument.toAttachable() = SDPDokumentAsicEWrapper(this, documentReceiver)

    companion object {
        const val ASICE_CONTAINER = "ASiCE_CONTAINER"
    }
}

data class AsicE(val bytes: ByteArray, val uncompressedBytes: Long)

class DocumentReceiver(val documentDirectory: String) {
    fun getBytes(sdpDokument: SDPDokument): ByteArray {
        log.info("Adding zip from {}", sdpDokument.href)
        return Files.readAllBytes(Paths.get(documentDirectory).resolve(sdpDokument.href))
    }
}

class SDPDokumentAsicEWrapper(private val sdpDokument: SDPDokument, private val documentReceiver: DocumentReceiver) : AsicEAttachable {
    override fun getFileName(): String = sdpDokument.href
    override fun getMimeType(): String = sdpDokument.mime
    override fun getBytes(): ByteArray = documentReceiver.getBytes(sdpDokument)
}
