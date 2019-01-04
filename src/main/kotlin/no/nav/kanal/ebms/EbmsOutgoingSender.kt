package no.nav.kanal.ebms

import no.digipost.api.PMode
import no.digipost.api.SdpMeldingSigner
import no.digipost.api.handlers.EbmsContextAware
import no.digipost.api.interceptors.steps.AddUserMessageStep
import no.digipost.api.representations.Dokumentpakke
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsOutgoingMessage
import no.digipost.api.representations.Mpc
import no.digipost.xsd.types.DigitalPostformidling
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.soap.SoapMessage
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument
import org.w3.xmldsig.DigestMethod
import org.w3.xmldsig.Reference
import java.util.UUID
import javax.activation.DataHandler

class EbmsOutgoingSender(
    val signer: SdpMeldingSigner,
    val dataHandler: EbmsAktoer,
    val technicalReceiver: EbmsAktoer,
    val sbd: StandardBusinessDocument,
    val documentPackage: Dokumentpakke,
    val priority: EbmsOutgoingMessage.Prioritet,
    val mpcId: String,
    val messageId: String,
    val action: PMode.Action,
    val marshaller: Jaxb2Marshaller
) : EbmsContextAware(), WebServiceMessageCallback {
    override fun doWithMessage(message: WebServiceMessage?) {
        message as SoapMessage
        val sbdPayload = sbd.any as DigitalPostformidling
        sbdPayload.dokumentpakkefingeravtrykk = Reference()
                .withDigestMethod(DigestMethod().withAlgorithm(javax.xml.crypto.dsig.DigestMethod.SHA256))
                .withDigestValue(documentPackage.shA256)
        message.addAttachment(generateContentId(), DataHandler(documentPackage))

        val mpc = Mpc(priority, mpcId)
        signer.sign(sbd)
        ebmsContext.addRequestStep(AddUserMessageStep(mpc, messageId, action, null, sbd, dataHandler, technicalReceiver, marshaller))

    }

    private fun generateContentId(): String {
        return "<${UUID.randomUUID()}@meldingsformidler.sdp.difi.no>"
    }
}
