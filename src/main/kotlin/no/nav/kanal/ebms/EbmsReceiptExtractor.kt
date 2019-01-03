package no.nav.kanal.ebms

import no.digipost.api.handlers.EbmsContextAware
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.core.WebServiceMessageExtractor
import org.springframework.ws.soap.SoapMessage
import java.io.ByteArrayOutputStream
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult

private const val NO_MESSAGE_AVAILABLE_FROM_MPC_ERROR_CODE = "EBMS:0006"

private val tf: TransformerFactory = TransformerFactory.newInstance()
class EbmsReceiptExtractor : EbmsContextAware(), WebServiceMessageExtractor<EbmsReceipt?> {
    override fun extractData(message: WebServiceMessage?): EbmsReceipt? =
            if (ebmsContext.warning != null && ebmsContext.warning.errorCode == NO_MESSAGE_AVAILABLE_FROM_MPC_ERROR_CODE) {
                null
            } else {
                EbmsReceipt(
                        sbdBytes = ByteArrayOutputStream().use {
                            tf.newTransformer()
                                    .apply {
                                        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                                    }
                                    .transform((message as SoapMessage).soapBody.payloadSource, StreamResult(it))
                            it
                        }.toByteArray(),
                        messageId = ebmsContext.userMessage.messageInfo.messageId,
                        refToMessageId = ebmsContext.userMessage.messageInfo.refToMessageId,
                        references = ebmsContext.incomingReferences.values
                )
            }
}

