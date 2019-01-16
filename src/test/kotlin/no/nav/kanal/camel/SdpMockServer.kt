package no.nav.kanal.camel

import no.difi.begrep.sdp.schema_v10.SDPKvittering
import no.difi.begrep.sdp.schema_v10.SDPLevering
import no.difi.begrep.sdp.schema_v10.SDPMelding
import org.apache.cxf.binding.soap.SoapMessage
import org.apache.cxf.binding.soap.interceptor.SoapInterceptor
import org.apache.cxf.endpoint.Server
import org.apache.cxf.ext.logging.LoggingFeature
import org.apache.cxf.headers.Header
import org.apache.cxf.jaxb.JAXBDataBinding
import org.apache.cxf.jaxws.JaxWsServerFactoryBean
import org.apache.cxf.message.Message
import org.apache.cxf.phase.AbstractPhaseInterceptor
import org.apache.cxf.phase.Phase
import org.apache.cxf.ws.security.wss4j.WSS4JInInterceptor
import org.apache.cxf.ws.security.wss4j.WSS4JOutInterceptor
import org.apache.wss4j.common.bsp.BSPEnforcer
import org.apache.wss4j.common.crypto.Merlin
import org.apache.wss4j.common.ext.WSPasswordCallback
import org.apache.wss4j.common.token.X509Security
import org.apache.wss4j.dom.handler.WSHandlerConstants
import org.apache.wss4j.dom.util.WSSecurityUtil
import org.oasis_open.docs.ebxml_bp.ebbp_signals_2.MessagePartNRInformation
import org.oasis_open.docs.ebxml_bp.ebbp_signals_2.NonRepudiationInformation
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.AgreementRef
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Description
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error as EbmsError
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartProperties
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Receipt
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.SignalMessage
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage

import org.unece.cefact.namespaces.standardbusinessdocumentheader.BusinessScope
import org.unece.cefact.namespaces.standardbusinessdocumentheader.DocumentIdentification
import org.unece.cefact.namespaces.standardbusinessdocumentheader.Partner
import org.unece.cefact.namespaces.standardbusinessdocumentheader.PartnerIdentification
import org.unece.cefact.namespaces.standardbusinessdocumentheader.Scope
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocumentHeader
import org.w3.xmldsig.Reference
import org.w3.xmldsig.Signature
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.FileOutputStream
import java.io.StringWriter
import java.net.URI
import java.security.KeyStore
import java.time.ZonedDateTime
import java.util.Properties
import java.util.UUID
import javax.security.auth.callback.CallbackHandler
import javax.xml.bind.JAXBContext
import javax.xml.bind.Marshaller
import javax.xml.bind.Unmarshaller
import javax.xml.crypto.dsig.CanonicalizationMethod
import javax.xml.crypto.dsig.DigestMethod
import javax.xml.crypto.dsig.Transform
import javax.xml.crypto.dsig.XMLSignatureFactory
import javax.xml.crypto.dsig.dom.DOMSignContext
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec
import javax.xml.crypto.dsig.spec.TransformParameterSpec
import javax.xml.namespace.QName
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.soap.AttachmentPart
import javax.xml.soap.MessageFactory
import javax.xml.soap.MimeHeader
import javax.xml.soap.SOAPConstants
import javax.xml.soap.SOAPMessage
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.ws.BindingType
import javax.xml.ws.Provider
import javax.xml.ws.ServiceMode
import javax.xml.ws.WebServiceProvider
import javax.xml.ws.soap.SOAPBinding
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathExpression
import javax.xml.xpath.XPathFactory
import javax.xml.ws.Service as WSService

private const val meldingsformidlerOrgNr = "984661185"
private const val orgNrPartyType = "urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908"

private const val agreementUrl = "http://begrep.difi.no/SikkerDigitalPost/Meldingsutveksling/FormidleDigitalPostForsendelse"
private val agreementRef: AgreementRef = AgreementRef(agreementUrl,  null, "nav-digital-post")
private val collaberatorService: Service = Service("SDP", null)
private val sdpFrom = From(listOf(PartyId(meldingsformidlerOrgNr, orgNrPartyType)), "urn:sdp:meldingsformidler")

val messagingNamespace = QName("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "Messaging")
// Not sure about localName
val wsuNamespace = QName("http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd", "Id")
val ebmsXpath: XPath = XPathFactory.newInstance().newXPath().apply {
    namespaceContext = EbmsNamespaceContext
}
val wsseResourceXPath: XPathExpression = ebmsXpath.compile("./ds:Signature/ds:SignedInfo/ds:Reference")
val serialNumberRegexp = Regex("SERIALNUMBER=([0-9]{9}),.+")

data class EbmsAttachment(val contentId: String, val mimeHeaders: List<MimeHeader>)

interface SbdHandler {
    fun handleUserMessage(sbdIn: StandardBusinessDocument, attachments: List<EbmsAttachment>, senderOrgNumber: String, userMessage: UserMessage): EbmsResponse
    fun handleSignalMessage(attachments: List<EbmsAttachment>, senderOrgNumber: String, signalMessage: SignalMessage): EbmsResponse

    fun defaultReceipt(conversationId: String) = EbmsResponse(
            sbdPayload = SDPKvittering().apply {
                tidspunkt = ZonedDateTime.now()
                levering = SDPLevering()
            },
            addSignalMessage = false,
            userMessageInfo = EbmsUserMessageInfo("kvittering", conversationId),
            errors = listOf())
    fun noNewMessages(): EbmsResponse = EbmsResponse(
            sbdPayload = null,
            emptySbd = true,
            addSignalMessage = true,
            userMessageInfo = null,
            errors = listOf(EbmsError(
                    Description("There is no message available for pulling from this MPC at this moment.", "en"),
                    null,
                    "Communication",
                    "TODO",
                    "EBMS:0006",
                    "ebMS",
                    "warning",
                    "EmptyMessagePartitionChannel"
            ))
    )
}

data class EbmsUserMessageInfo(val action: String, val conversationId: String)
data class EbmsResponse(val sbdPayload: Any?, val userMessageInfo: EbmsUserMessageInfo?, val addSignalMessage: Boolean, val errors: List<EbmsError>, val emptySbd: Boolean = false)

object DefaultSbdHandler : SbdHandler {
    override fun handleUserMessage(sbdIn: StandardBusinessDocument, attachments: List<EbmsAttachment>, senderOrgNumber: String, userMessage: UserMessage): EbmsResponse {
        // Assume its a request to send SDP
        return EbmsResponse(null, null, true, listOf())
    }


    override fun handleSignalMessage(attachments: List<EbmsAttachment>, senderOrgNumber: String, signalMessage: SignalMessage): EbmsResponse {
        return defaultReceipt(UUID.randomUUID().toString())
    }

}

@BindingType(SOAPBinding.SOAP12HTTP_BINDING)
@WebServiceProvider(
        portName = "FormidleDigitalPostPort",
        serviceName = "FormidleDigitalPost",
        targetNamespace = "http://www.unece.org/cefact/namespaces/StandardBusinessDocumentHeader"
)
@ServiceMode(value = WSService.Mode.MESSAGE)
class Soap(private val sbdHandler: SbdHandler, val keystore: KeyStore) : Provider<SOAPMessage> {
    private val sbdContext = JAXBContext.newInstance(StandardBusinessDocument::class.java, SDPMelding::class.java)
    private val messagingContext: JAXBContext = JAXBContext.newInstance(Messaging::class.java, NonRepudiationInformation::class.java)

    private val signatureKey = keystore.getKey("posten", "changeit".toCharArray())

    override fun invoke(request: SOAPMessage): SOAPMessage {
        val referenceUnmarshaller: Unmarshaller = JAXBContext.newInstance(Reference::class.java).createUnmarshaller()
        val messageFactory: MessageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)
        // private val documentBuilder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        // private val sdpMessageMarshaller: Marshaller = JAXBContext.newInstance(SDPMelding::class.java).createMarshaller()
        // private val sdpMessageMarshaller: Marshaller = JAXBContext.newInstance(SDPMelding::class.java).createMarshaller()
        val bodyMarshaller: Marshaller = sbdContext.createMarshaller()
        val sbdUnmarshaller: Unmarshaller = sbdContext.createUnmarshaller()


        val messagingUnmarshaller: Unmarshaller = messagingContext.createUnmarshaller()
        val messagingMarshaller: Marshaller = messagingContext.createMarshaller()

        //val messagingHeader = messagingUnmarshaller.unmarshal(findMessagingHeader(request.soapHeader)!!, Messaging::class.java).value
        val messagingHeader = messagingUnmarshaller.unmarshal(request.soapHeader.getChildElements(messagingNamespace).next() as Node, Messaging::class.java).value
        val wsseHeader = WSSecurityUtil.getSecurityHeader(request.soapPart, "")

        // If its a user message its most likely a distribution request
        val isUserMessage = messagingHeader.userMessages?.isNotEmpty() == true

        val action = when (isUserMessage) {
            true -> "FormidleDigitalPost"
            else -> "KvitteringsForespoersel"
        }
        val clientPublicKey = X509Security(wsseHeader.firstChild as Element, BSPEnforcer(true))

        val certificate = clientPublicKey.getX509Certificate(Merlin())
        val senderOrgNrFromCertificate = serialNumberRegexp.matchEntire(certificate.subjectDN.name)!!.groups[1]!!.value

        val attachments = request.attachments.toList().map { it as AttachmentPart }.map { attachment ->
            EbmsAttachment(
                    contentId = attachment.contentId.replace("<", "cid:").replace(">", ""),
                    mimeHeaders = attachment.allMimeHeaders.toList().map { it as MimeHeader }
            )
        }

        val previousMessageInfo = if (isUserMessage) {
            messagingHeader.userMessages.first().messageInfo
        } else {
            messagingHeader.signalMessages.first().messageInfo
        }

        val newMsgInfo = MessageInfo(ZonedDateTime.now(), UUID.randomUUID().toString(), previousMessageInfo.messageId)
        val soapBodyId = request.soapBody.getAttributeValue(wsuNamespace)

        val ebmsResponse = if (isUserMessage) {
            val sbd = sbdUnmarshaller.unmarshal(request.soapBody, StandardBusinessDocument::class.java).value
            sbdHandler.handleUserMessage(sbd, attachments, senderOrgNrFromCertificate, messagingHeader.userMessages.first())
        } else {
            sbdHandler.handleSignalMessage(attachments, senderOrgNrFromCertificate, messagingHeader.signalMessages.first())
        }

        return messageFactory.createMessage().apply {
            val messaging = Messaging().apply {
                if (ebmsResponse.addSignalMessage) {
                    signalMessages.add(SignalMessage().apply {
                        messageInfo = newMsgInfo
                        receipt = Receipt().apply {
                            val references = wsseResourceXPath.evaluate(wsseHeader, XPathConstants.NODESET) as NodeList
                            val nonRepReferences = mutableListOf<Reference>()
                            val attachmentContentIds = attachments.map { it.contentId }
                            for (i in 0..(references.length - 1)) {
                                val uri = references.item(i).attributes.getNamedItem("URI").textContent
                                if (uri in attachmentContentIds || uri == "#$soapBodyId") {
                                    nonRepReferences.add(referenceUnmarshaller.unmarshal(references.item(i), Reference::class.java).value)
                                }
                            }
                            anies.addAll(listOf(NonRepudiationInformation(
                                    nonRepReferences.map { MessagePartNRInformation(it, null) }
                            )))
                        }
                        errors.addAll(ebmsResponse.errors)
                    })
                }
                if (ebmsResponse.userMessageInfo != null) {
                    userMessages = listOf(UserMessage().apply {
                        messageInfo = newMsgInfo
                        collaborationInfo = CollaborationInfo(agreementRef, collaberatorService, action, ebmsResponse.userMessageInfo.conversationId)

                        partyInfo = PartyInfo(sdpFrom, To(listOf(PartyId(senderOrgNrFromCertificate, orgNrPartyType)), "urn:sdp:avsender"))
                        payloadInfo = PayloadInfo(attachments.map { attachment ->
                            PartInfo(null, null, PartProperties(attachment.mimeHeaders.map { Property(it.value, it.name) }), attachment.contentId)
                        })
                        if (payloadInfo.partInfos.isEmpty()) {
                            payloadInfo.partInfos = listOf(PartInfo())
                        }
                    })
                }
            }
            messagingMarshaller.marshal(messaging, soapHeader)

            if (ebmsResponse.sbdPayload != null) {
                StandardBusinessDocument().apply {
                    standardBusinessDocumentHeader = createSbdHeader(senderOrgNrFromCertificate, "kvittering", ebmsResponse.userMessageInfo!!.conversationId)
                    any = ebmsResponse.sbdPayload
                    if (any is SDPMelding) {
                        signSdpPayload(any as SDPMelding)
                    }
                    bodyMarshaller.marshal(this, soapBody)
                }
            } else if (ebmsResponse.emptySbd) {
                bodyMarshaller.marshal( StandardBusinessDocument(), soapBody)
            }
        }
    }

    private fun createSbdHeader(receiverOrgNumber: String, typeMessage: String, conversationId: String): StandardBusinessDocumentHeader = StandardBusinessDocumentHeader().apply {
        headerVersion = "1.0"
        senders.add(Partner(PartnerIdentification(meldingsformidlerOrgNr, "iso6523-actorid-upis"), mutableListOf()))
        receivers.add(Partner(PartnerIdentification(receiverOrgNumber, "iso6523-actorid-upis"), mutableListOf()))
        documentIdentification = DocumentIdentification("urn:no:difi:sdp:1.0", "1.0", conversationId, typeMessage, null, ZonedDateTime.now())
        businessScope = BusinessScope(listOf(Scope("ConversationId", UUID.randomUUID().toString(), "urn:no:difi:sdp:1.0", listOf())))
    }

    // To be honest, I have no idea what we're supposed to sign here, however it seems like the normal java client accepts it.
    private fun signSdpPayload(sdpPayload: SDPMelding) {
        val signatureFactory: XMLSignatureFactory = XMLSignatureFactory.getInstance("DOM")
        val signatureUnmarshaller: Unmarshaller = JAXBContext.newInstance(Signature::class.java).createUnmarshaller()
        val digestMethod: DigestMethod = signatureFactory.newDigestMethod(DigestMethod.SHA256, null)
        val transforms: List<Transform> = listOf(signatureFactory.newTransform(Transform.ENVELOPED, null as TransformParameterSpec?))

        val keyInfo = keystore.getCertificate("posten").let {
            val keyInfoFactory  = signatureFactory.keyInfoFactory
            keyInfoFactory.newKeyInfo(listOf(keyInfoFactory.newX509Data(listOf(it))))
        }
        //val dom = documentBuilder.newDocument()
        //sdpMessageMarshaller.marshal(sdpPayload, dom)
        val signatureReference = signatureFactory.newReference("", digestMethod, transforms, null, null)

        val signedInfo = signatureFactory.newSignedInfo(signatureFactory.newCanonicalizationMethod(CanonicalizationMethod.EXCLUSIVE, null as C14NMethodParameterSpec?), signatureFactory.newSignatureMethod("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", null), listOf(signatureReference))


        val xmlSignature = signatureFactory.newXMLSignature(signedInfo, keyInfo)
        val signatureDOM = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
        val domSignContext = DOMSignContext(signatureKey, signatureDOM)
        xmlSignature.sign(domSignContext)
        sdpPayload.signature = signatureUnmarshaller.unmarshal(signatureDOM, Signature::class.java).value
    }

}

fun <T: Any?> Iterator<T>.toList(): List<T> = mutableListOf<T>().apply {
    while (hasNext()) { add(next()) }
}

fun main(args: Array<String>) {
    val keyStore = generateKeyStore().apply {
        store(FileOutputStream("build/keystore.p12"), "changeit".toCharArray())
    }
    createSDPMockServer(keyStore = keyStore)
}

data class SdpServer(val server: Server, val sfb: JaxWsServerFactoryBean)

fun createSDPMockServer(sbdHandler: SbdHandler = DefaultSbdHandler, port: Int = 8080, keyStore: KeyStore): SdpServer {
    val sfb = JaxWsServerFactoryBean().apply {
        serviceBean = Soap(sbdHandler, keyStore)
        address = "http://localhost:$port/sdpmock"
        features.add(LoggingFeature())
    }
    return SdpServer(sfb.create().apply {
        endpoint.apply {
            val signatureProperties = Properties().apply {
                this["org.apache.ws.security.crypto.provider"] = Merlin::class.java
                this[Merlin.PREFIX + Merlin.TRUSTSTORE_PASSWORD] = "changeit"
                this[Merlin.PREFIX + Merlin.TRUSTSTORE_FILE] = "build/keystore.p12"
                this[Merlin.PREFIX + Merlin.TRUSTSTORE_TYPE] = "pkcs12"
            }

            val signingProperties = Properties().apply {
                this["org.apache.ws.security.crypto.provider"] = Merlin::class.java
                this[Merlin.PREFIX + Merlin.KEYSTORE_PASSWORD] = "changeit"
                this[Merlin.PREFIX + Merlin.KEYSTORE_FILE] = "build/keystore.p12"
                this[Merlin.PREFIX + Merlin.KEYSTORE_TYPE] = "pkcs12"
            }

            inInterceptors.add(WSS4JInInterceptor(mapOf(
                    WSHandlerConstants.ACTION to WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.TIMESTAMP,
                    "signatureValidatorProperties" to signatureProperties,
                    WSHandlerConstants.SIG_PROP_REF_ID to "signatureValidatorProperties"
            )))
            inInterceptors.add(object : AbstractPhaseInterceptor<SoapMessage>(Phase.PRE_PROTOCOL), SoapInterceptor {
                override fun handleMessage(message: SoapMessage) {

                }

                override fun getUnderstoodHeaders(): MutableSet<QName> = mutableSetOf(
                        QName("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/", "Messaging")
                )

                override fun getRoles(): MutableSet<URI>? = null

            })
            outInterceptors.add(object : AbstractPhaseInterceptor<Message>(Phase.PRE_LOGICAL) {
                override fun handleMessage(message: Message) {
                    if (message.exchange.inMessage.attachments != null) {
                        message.attachments = message.exchange.inMessage.attachments.toMutableList()
                    }
                }
            })
            val wssOutInterceptor = WSS4JOutInterceptor(mapOf(
                    WSHandlerConstants.ACTION to WSHandlerConstants.SIGNATURE + " " + WSHandlerConstants.TIMESTAMP,
                    WSHandlerConstants.USER to "posten",
                    WSHandlerConstants.PW_CALLBACK_REF to CallbackHandler {
                        (it[0] as WSPasswordCallback).password = "changeit"
                    },
                    WSHandlerConstants.SIG_KEY_ID to "DirectReference",
                    WSHandlerConstants.SIG_ALGO to "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256",
                    WSHandlerConstants.SIG_DIGEST_ALGO to "http://www.w3.org/2001/04/xmlenc#sha256",
                    WSHandlerConstants.SIGNATURE_PARTS to "{}{http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd}Timestamp;{}{}Body;{}{http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/}Messaging;{}cid:Attachments;",
                    "signatureCreatorProperties" to signingProperties,
                    WSHandlerConstants.SIG_PROP_REF_ID to "signatureCreatorProperties",
                    WSHandlerConstants.STORE_BYTES_IN_ATTACHMENT to "false"
            ))
            outInterceptors.add(wssOutInterceptor)
        }
        start()
    }, sfb)
}
