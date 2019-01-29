package no.nav.kanal.ebms

import kotlinx.coroutines.runBlocking
import no.digipost.api.handlers.EbmsContextAware
import no.nav.kanal.LegalArchiveLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.core.WebServiceMessageExtractor
import org.springframework.ws.soap.SoapMessage
import toByteArray
import java.io.ByteArrayOutputStream
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.StreamResult

private const val NO_MESSAGE_AVAILABLE_FROM_MPC_ERROR_CODE = "EBMS:0006"

private val tf: TransformerFactory = TransformerFactory.newInstance()
const val EVENT_TYPE = "INCOMING_RECEIPT"
class EbmsReceiptExtractor(val legalArchiveLogger:  LegalArchiveLogger) : EbmsContextAware(), WebServiceMessageExtractor<EbmsReceipt?> {

    val log: Logger = LoggerFactory.getLogger(EbmsReceiptExtractor::class.java)
    override fun extractData(message: WebServiceMessage?): EbmsReceipt? =
            if (ebmsContext.warning != null && ebmsContext.warning.errorCode == NO_MESSAGE_AVAILABLE_FROM_MPC_ERROR_CODE) {
                null
            } else {
                val soapBody = (message as SoapMessage).soapBody
                val conversationId = ebmsContext.sbd.underlyingDoc.standardBusinessDocumentHeader
                        .businessScope.scopes.first().instanceIdentifier

                runBlocking {
                    legalArchiveLogger.archiveDocumentLogOnException(conversationId!!, "NAV", "DIFI meldingsformidler", message.toByteArray(this), EVENT_TYPE)
                }
                EbmsReceipt(
                        sbdBytes = ByteArrayOutputStream().use {
                            tf.newTransformer()
                                    .apply {
                                        setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
                                    }
                                    .transform(soapBody.payloadSource, StreamResult(it))
                            it
                        }.toByteArray(),
                        messageId = ebmsContext.userMessage.messageInfo.messageId,
                        conversationId = conversationId,
                        refToMessageId = ebmsContext.userMessage.messageInfo.refToMessageId,
                        references = ebmsContext.incomingReferences.values
                )
            }
}

