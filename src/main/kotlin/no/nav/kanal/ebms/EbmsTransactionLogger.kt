package no.nav.kanal.ebms

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.digipost.api.config.TransaksjonsLogg
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Error
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.MessageInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.ws.soap.SoapFault

class EbmsTransactionLogger : TransaksjonsLogg() {
    private val log: Logger = LoggerFactory.getLogger(EbmsTransactionLogger::class.java)

    override fun soapfault(endpoint: String, orgnr: String, soapFault: SoapFault) {
        log.error("SOAP fault occurred {}",
                keyValue("orgNumber", orgnr),
                keyValue("faultCode", soapFault.faultCode),
                keyValue("faultReason", soapFault.faultStringOrReason),
                keyValue("faultDetail", soapFault.faultDetail))
    }

    override fun ebmserror(
        endpoint: String?,
        orgnr: String?,
        retning: Retning?,
        error: Error?,
        messageInfo: MessageInfo?,
        mpc: String?,
        conversationId: String?,
        instanceIdentifier: String?
    ) {
        log.error("An EBMS error occurred {}, {}, {}, {}",
                keyValue("orgNumber", orgnr),
                keyValue("conversationId", conversationId),
                keyValue("messageId", messageInfo?.messageId ?: ""),
                keyValue("mpc", mpc))
    }
}
