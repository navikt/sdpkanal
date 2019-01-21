package no.nav.kanal.ebms

import kotlinx.coroutines.runBlocking
import no.digipost.api.handlers.EbmsContextAware
import no.nav.kanal.LegalArchiveLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.core.WebServiceMessageExtractor
import org.springframework.ws.soap.SoapMessage
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import toByteArray
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
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
                val node = (soapBody.payloadSource as DOMSource).node
                /*
                This might be a bit hacky way of extracting the conversationId, however it does not require a strict
                XPath or re-parsing of the document, so its a simple but hacky way of extracting it
                 */
                val conversationId = node
                        ?.childNodes?.find("StandardBusinessDocumentHeader")
                        ?.childNodes?.find("BusinessScope")
                        ?.childNodes?.find("Scope")
                        ?.childNodes?.find("InstanceIdentifier")
                        ?.firstChild?.nodeValue
                runBlocking {
                    legalArchiveLogger.archiveDocumentLogOnException(conversationId!!, "NAV", "DIFI meldingsformidler", message.toByteArray(this), EVENT_TYPE)
                }
                Files.newOutputStream(Paths.get("receipt_$conversationId.bin")).use { message.writeTo(it) }
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

    private fun NodeList.find(name: String): Node? {
        for (i in 0.until(length)) {
            if (item(i)?.localName == name)
                return item(i)
        }
        return null
    }
}

