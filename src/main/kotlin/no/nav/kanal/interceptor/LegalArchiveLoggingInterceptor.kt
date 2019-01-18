package no.nav.kanal.interceptor

import no.nav.kanal.ebms.CONVERSATION_ID_PROPERTY
import no.nav.kanal.ebms.SHOULD_BE_LOGGED_PROPERTY
import org.springframework.ws.client.support.interceptor.ClientInterceptor
import org.springframework.ws.context.MessageContext
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Paths

class LegalArchiveLoggingInterceptor : ClientInterceptor {
    override fun handleFault(messageContext: MessageContext): Boolean {
        return true
    }

    override fun handleRequest(messageContext: MessageContext): Boolean {
        return true
    }

    override fun handleResponse(ctx: MessageContext): Boolean {
        if (ctx.getProperty(SHOULD_BE_LOGGED_PROPERTY) == true) {
            val conversationId = ctx.getProperty(CONVERSATION_ID_PROPERTY)
            Files.newOutputStream(Paths.get("payload_out_$conversationId.bin")).use { ctx.request.writeTo(it) }
            Files.newOutputStream(Paths.get("payload_in_$conversationId.bin")).use { ctx.response.writeTo(it) }
        }
        return true
    }

    override fun afterCompletion(messageContext: MessageContext, ex: Exception?) {}
}
