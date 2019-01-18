package no.nav.kanal.ebms

import no.digipost.api.handlers.EbmsContextAwareWebServiceTemplate
import no.digipost.api.representations.EbmsAktoer
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.client.core.WebServiceMessageExtractor
import org.springframework.ws.context.MessageContext
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory
import org.springframework.ws.transport.WebServiceConnection

/*
Could not find a good way of hooking into the current code to get an interceptor that can dump the WS content, this way
we can add the conversationId and a boolean telling if it should be logged to an interceptor that will be able to log
specific messages. This is a bit hacky, but it seems like it would require a upstream change or to reimplement more of
the WS code
 */
class EbmsLoggingWebServiceTemplate(factory: SaajSoapMessageFactory?, ebmsAktoerRemoteParty: EbmsAktoer?) : EbmsContextAwareWebServiceTemplate(factory, ebmsAktoerRemoteParty) {
    override fun <T : Any?> doSendAndReceive(messageContext: MessageContext, connection: WebServiceConnection, requestCallback: WebServiceMessageCallback, responseExtractor: WebServiceMessageExtractor<T>): T {
        if (requestCallback is LoggableContext) {
            messageContext.setProperty(CONVERSATION_ID_PROPERTY, requestCallback.conversationId)
            messageContext.setProperty(SHOULD_BE_LOGGED_PROPERTY, requestCallback.shouldBeLogged)
        }
        return super.doSendAndReceive(messageContext, connection, requestCallback, responseExtractor)
    }
}
