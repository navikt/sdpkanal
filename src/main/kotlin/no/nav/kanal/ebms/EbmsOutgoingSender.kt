package no.nav.kanal.ebms

import no.digipost.api.PMode
import no.digipost.api.SdpMeldingSigner
import no.digipost.api.handlers.EbmsContextAware
import no.digipost.api.interceptors.steps.AddUserMessageStep
import no.digipost.api.representations.Dokumentpakke
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsOutgoingMessage
import no.digipost.api.representations.Mpc
import no.digipost.api.xml.Marshalling
import no.digipost.xsd.types.DigitalPostformidling
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.soap.SoapMessage
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument
import org.w3.xmldsig.DigestMethod
import org.w3.xmldsig.Reference
import java.nio.file.Files
import java.nio.file.Paths
import java.util.UUID
import javax.activation.DataHandler

class EbmsOutgoingSender(
    private val signer: SdpMeldingSigner,
    private val dataHandler: EbmsAktoer,
    private val technicalReceiver: EbmsAktoer,
    private val sbd: StandardBusinessDocument,
    private val documentPackage: Dokumentpakke,
    private val priority: EbmsOutgoingMessage.Prioritet,
    private val mpcId: String,
    private val messageId: String,
    override val conversationId: String,
    private val action: PMode.Action,
    private val marshaller: Jaxb2Marshaller
) : EbmsContextAware(), WebServiceMessageCallback, LoggableContext {
    override val shouldBeLogged: Boolean = true
    override fun doWithMessage(message: WebServiceMessage) {
        message as SoapMessage
        val sbdPayload = sbd.any as DigitalPostformidling
        sbdPayload.dokumentpakkefingeravtrykk = Reference()
                .withDigestMethod(DigestMethod().withAlgorithm(javax.xml.crypto.dsig.DigestMethod.SHA256))
                .withDigestValue(documentPackage.shA256)
        message.addAttachment(generateContentId(), DataHandler(documentPackage))

        val mpc = Mpc(priority, mpcId)
        val signedDoc = signer.sign(sbd)
        Marshalling.marshal(signedDoc, message.envelope.body.payloadResult)
        ebmsContext.addRequestStep(EbmsUserMessagingStep(
                mpc = mpc,
                messageId = messageId,
                conversationId = conversationId,
                action = action,
                refToMessageId = null,
                datahandler = dataHandler,
                receiver = technicalReceiver,
                marshaller = marshaller))
    }

    private fun generateContentId(): String {
        return "<${UUID.randomUUID()}@meldingsformidler.sdp.difi.no>"
    }
}
