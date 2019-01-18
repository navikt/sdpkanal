package no.nav.kanal.ebms

import no.digipost.api.handlers.EbmsContextAwareWebServiceTemplate
import no.digipost.api.representations.EbmsAktoer
import org.springframework.ws.client.WebServiceIOException
import org.springframework.ws.client.WebServiceTransportException
import org.springframework.ws.client.core.WebServiceMessageCallback
import org.springframework.ws.client.core.WebServiceMessageExtractor
import org.springframework.ws.context.DefaultMessageContext
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory
import org.springframework.ws.transport.TransportException
import org.springframework.ws.transport.context.DefaultTransportContext
import org.springframework.ws.transport.context.TransportContextHolder
import java.io.IOException
import java.net.URI

/*
Could not find a good way of hooking into the current code to get an interceptor that can dump the WS content, this way
we can add the conversationId and a boolean telling if it should be logged to an interceptor that will be able to log
specific messages. This is a bit hacky, but it seems like it would require a upstream change or to reimplement more of
the WS code
 */
class EbmsLoggingWebServiceTemplate(factory: SaajSoapMessageFactory?, ebmsAktoerRemoteParty: EbmsAktoer?) : EbmsContextAwareWebServiceTemplate(factory, ebmsAktoerRemoteParty) {
    override fun <T : Any?> sendAndReceive(uriString: String, requestCallback: WebServiceMessageCallback, responseExtractor: WebServiceMessageExtractor<T>): T {
        return try {
            createConnection(URI.create(uriString)).use { connection ->
                TransportContextHolder.setTransportContext(DefaultTransportContext(connection))
                val messageContext = DefaultMessageContext(messageFactory)
                if (requestCallback is LoggableContext) {
                    messageContext.setProperty(CONVERSATION_ID_PROPERTY, requestCallback.conversationId)
                    messageContext.setProperty(SHOULD_BE_LOGGED_PROPERTY, requestCallback.shouldBeLogged)
                }
                doSendAndReceive(messageContext, connection, requestCallback, responseExtractor)
            }
        } catch (ex: TransportException) {
            throw WebServiceTransportException("Could not use transport: ${ex.message}", ex)
        } catch (ex: IOException) {
            throw WebServiceIOException("I/O error: ${ex.message}", ex)
        }
    }
}
