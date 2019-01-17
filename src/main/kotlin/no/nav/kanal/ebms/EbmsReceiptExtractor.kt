package no.nav.kanal.ebms

import no.digipost.api.handlers.EbmsContextAware
import org.springframework.ws.WebServiceMessage
import org.springframework.ws.client.core.WebServiceMessageExtractor
import org.springframework.ws.soap.SoapMessage
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.ByteArrayOutputStream
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.xpath.XPath
import javax.xml.xpath.XPathFactory

private const val NO_MESSAGE_AVAILABLE_FROM_MPC_ERROR_CODE = "EBMS:0006"

private val tf: TransformerFactory = TransformerFactory.newInstance()
class EbmsReceiptExtractor : EbmsContextAware(), WebServiceMessageExtractor<EbmsReceipt?> {
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
        for (i in 0.rangeTo(length)) {
            if (item(i)?.localName == name)
                return item(i)
        }
        return null
    }
}

