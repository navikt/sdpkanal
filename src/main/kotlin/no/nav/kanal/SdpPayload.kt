package no.nav.kanal

import no.difi.begrep.sdp.schema_v10.SDPManifest
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlRootElement

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
open class SdpPayload {
    @field:XmlElement
    lateinit var standardBusinessDocument: StandardBusinessDocument
    @field:XmlElement
    lateinit var manifest: SDPManifest
    @field:XmlElement(name = "sertifikat")
    lateinit var certificate: ByteArray
    @field:XmlElement(name = "erPrioritert")
    var hasPriority: Boolean = false
}
