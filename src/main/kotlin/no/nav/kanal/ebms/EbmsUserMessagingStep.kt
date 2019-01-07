package no.nav.kanal.ebms

import java.time.ZonedDateTime
import no.digipost.api.PMode.Action
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsContext
import no.digipost.api.representations.EbmsProcessingStep
import no.digipost.api.representations.Mpc
import no.digipost.api.xml.Constants
import no.digipost.api.xml.Marshalling
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.AgreementRef
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.CollaborationInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.From
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartProperties
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyId
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PartyInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.PayloadInfo
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Property
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Service
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.To
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.UserMessage
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.soap.SoapHeaderElement
import org.springframework.ws.soap.SoapMessage

private const val isoPartyIdType = "urn:oasis:names:tc:ebcore:partyid-type:iso6523:9908"

/*
The application initiating the request correlates the message with sbd.standardBusinessDocumentHeader.businessScope.scopes.first().instanceIdentifier
rather than (new SimpleStandardBusinessDocument(doc)).getInstanceIdentifier() which is hardcoded in the library written
by Posten. Since it would be hard get anything changed in the other application we've reimplemented it here
 */
class EbmsUserMessagingStep(
    private val mpc: Mpc,
    messageId: String,
    conversationId: String,
    action: Action,
    refToMessageId: String?,
    datahandler: EbmsAktoer,
    receiver: EbmsAktoer,
    private val marshaller: Jaxb2Marshaller
) : EbmsProcessingStep {
    private val from = From()
            .withRole(datahandler.rolle.urn)
            .withPartyIds(PartyId(datahandler.orgnr.organisasjonsnummerMedLandkode, isoPartyIdType))
    private val to = To().withRole(receiver.rolle.urn)
            .withPartyIds(PartyId(receiver.orgnr.organisasjonsnummerMedLandkode, isoPartyIdType))
    private val partyInfo = PartyInfo(from, to)
    private val collaborationInfo = CollaborationInfo()
            .withAction(action.value)
            .withAgreementRef(AgreementRef()
                    .withValue(action.agreementRef))
            .withConversationId(conversationId)
            .withService(Service().withValue("SDP"))
    private val messageInfo = MessageInfo()
            .withMessageId(messageId)
            .withRefToMessageId(refToMessageId)
            .withTimestamp(ZonedDateTime.now())

    override fun apply(ebmsContext: EbmsContext, ebmsMessaging: SoapHeaderElement, soapMessage: SoapMessage) {
        val userMessage = UserMessage()
                .withMpc(mpc.toString())
                .withMessageInfo(messageInfo).withCollaborationInfo(collaborationInfo)
                .withPartyInfo(partyInfo)
                .withPayloadInfo(PayloadInfo().withPartInfos(createPartInfo(soapMessage)))
        Marshalling.marshal(marshaller, ebmsMessaging, Constants.USER_MESSAGE_QNAME, userMessage)
    }

    private fun createPartInfo(requestMessage: SoapMessage): List<PartInfo> = mutableListOf<PartInfo>().apply {
        requestMessage.attachments.forEachRemaining { attachment ->
            val cid = "cid:${attachment.contentId.substring(1, attachment.contentId.length - 1)}"
            add(PartInfo()
                    .withHref(cid)
                    .withPartProperties(PartProperties()
                            .withProperties(Property(attachment.contentType, "MimeType"), Property("sdp:Dokumentpakke", "Content"))))
        }
    }
}
