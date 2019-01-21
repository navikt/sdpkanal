package no.nav.kanal.interceptor

import kotlinx.coroutines.runBlocking
import no.nav.kanal.LegalArchiveLogger
import no.nav.kanal.ebms.CONVERSATION_ID_PROPERTY
import no.nav.kanal.ebms.SHOULD_BE_LOGGED_PROPERTY
import org.slf4j.LoggerFactory
import org.springframework.ws.client.support.interceptor.ClientInterceptor
import org.springframework.ws.context.MessageContext
import toByteArray
import java.lang.Exception

class LegalArchiveLoggingInterceptor(val legalArchiveLogger: LegalArchiveLogger) : ClientInterceptor {
    val log = LoggerFactory.getLogger(LegalArchiveLoggingInterceptor::class.java)

    override fun handleFault(messageContext: MessageContext): Boolean {
        return true
    }

    override fun handleRequest(messageContext: MessageContext): Boolean {
        return true
    }

    override fun handleResponse(ctx: MessageContext): Boolean {
        if (ctx.getProperty(SHOULD_BE_LOGGED_PROPERTY) == true) {
            val conversationId = ctx.getProperty(CONVERSATION_ID_PROPERTY) as String
            runBlocking {
                legalArchiveLogger.archiveDocumentLogOnException(conversationId, "NAV", "DIFI meldingsformidler",
                        ctx.request.toByteArray(this), "SDP_OUTGOING")
                legalArchiveLogger.archiveDocumentLogOnException(conversationId, "DIFI meldingsformidler", "NAV",
                        ctx.response.toByteArray(this), "SDP_OUTGOING_RECEIPT")
            }
        }
        return true
    }

    override fun afterCompletion(messageContext: MessageContext, ex: Exception?) {}
}
