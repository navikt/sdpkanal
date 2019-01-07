package no.nav.kanal.camel

import java.net.ConnectException
import java.net.UnknownHostException

import javax.xml.ws.soap.SOAPFaultException

import no.difi.begrep.sdp.schema_v10.SDPDigitalPost
import no.digipost.api.MessageSender
import no.digipost.api.PMode
import no.digipost.api.representations.*
import no.nav.kanal.SdpPayload
import no.nav.kanal.config.*
import no.nav.kanal.ebms.EbmsSender

import org.apache.camel.Exchange
import org.apache.camel.Processor
import org.apache.camel.RuntimeCamelException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import net.logstash.logback.argument.StructuredArguments.keyValue


class EbmsPush(
    private val maxRetries: Long,
    private val retryInterval: Long,
    messageSender: MessageSender,
    private val datahandler: EbmsAktoer,
    private val receiver: EbmsAktoer,
    private val ebmsSender: EbmsSender = EbmsSender.fromMessageSender(messageSender)
) : Processor {
    private val log: Logger = LoggerFactory.getLogger(EbmsPush::class.java)

    override fun process(exchangeIn: Exchange) {
        log.info("EBMS is pushing ${exchangeIn.loggingKeys()}", *exchangeIn.loggingValues())

        for (retryNumber in 0.until(maxRetries)) {
            try {
                val reply = createAndSendMessage(exchangeIn)
                exchangeIn.`in`.body = reply
                log.info("Message pushed ${exchangeIn.loggingKeys()}", *exchangeIn.loggingValues())
                return
            } catch (e: Exception) {
                log.error("Caught exception while trying to push message ${exchangeIn.loggingKeys()}",
                        *exchangeIn.loggingValues(),
                        e)
                throw e
            } catch (e: SOAPFaultException) {
                log.error("Error during transmit ${exchangeIn.loggingKeys()}", *exchangeIn.loggingValues(), e.message)
                // check if we should retry based on connection problems
                if (isConnectionProblem(e)) {
                    if (retryNumber + 1 == maxRetries) {
                        throw RuntimeCamelException("Maximum retries reached and ClientException during push", e)
                    }
                    log.warn("Failed to send message to meldingsformidler, waiting $retryInterval ms ${exchangeIn.loggingKeys()}",
                            *exchangeIn.loggingValues(),
                            e)
                    Thread.sleep(retryInterval)
                } else {
                    log.error("Exception caught is not marked as a temporary problem ${exchangeIn.loggingKeys()}",
                            *exchangeIn.loggingValues(),
                            e)
                    throw RuntimeCamelException("ClientException during push: " + e.message, e)
                }
            }
        }
    }


    private fun createAndSendMessage(exchangeIn: Exchange): TransportKvittering {
        val sbd = exchangeIn.getIn().getHeader(XmlExtractor.SDP_PAYLOAD, SdpPayload::class.java).standardBusinessDocument

        val documentPackage = exchangeIn.getIn().getHeader(DocumentPackageCreator.DOCUMENT_PACKAGE, Dokumentpakke::class.java)

        val sdpMelding = sbd.any as SDPDigitalPost

        val conversationId = exchangeIn.getIn().header<String>(CONVERSATION_ID_HEADER)
        val messageId = exchangeIn.getIn().header<String>(MESSAGE_ID_HEADER)
        val action = if (sdpMelding.digitalPostInfo == null) PMode.Action.FORMIDLE_FYSISK else PMode.Action.FORMIDLE_DIGITAL
        val mpcId = exchangeIn.getIn().header<String>(MPC_ID_HEADER)
        val priority = exchangeIn.getIn().header<EbmsOutgoingMessage.Prioritet>(PRIORITY_HEADER)
        return ebmsSender.send(datahandler, receiver, sbd, documentPackage, priority, mpcId, messageId, conversationId, action)
    }


    private fun isConnectionProblem(e: Exception): Boolean {
        var cause = e.cause
        for (i in 0.until(3)) {
            if (cause == null)
                return false
            if (cause is ConnectException || cause is UnknownHostException) {
                return true
            }
            cause = cause.cause
        }
        return false
    }
}
