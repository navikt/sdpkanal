package no.nav.kanal.ebms

import no.digipost.api.EbmsEndpointUriBuilder
import no.digipost.api.MessageSender
import no.digipost.api.handlers.BekreftelseSender
import no.digipost.api.handlers.EmptyReceiver
import no.digipost.api.handlers.PullRequestSender
import no.digipost.api.representations.EbmsPullRequest
import no.digipost.api.representations.KanBekreftesSomBehandletKvittering
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.client.core.WebServiceTemplate

class EbmsSender(
        private val marshaller: Jaxb2Marshaller,
        private val uri: EbmsEndpointUriBuilder,
        private val messageTemplate: WebServiceTemplate
) {
    fun fetchReceipt(
        ebmsPullRequest: EbmsPullRequest,
        previousConfirmableReceipt: KanBekreftesSomBehandletKvittering? = null
    ): EbmsReceipt? {
        val pullRequest = PullRequestSender(ebmsPullRequest, marshaller, previousConfirmableReceipt)
        return messageTemplate.sendAndReceive(uri.baseUri.toString(), pullRequest, EbmsReceiptExtractor())
    }

    fun confirmReceipt(confirmableReceipt: KanBekreftesSomBehandletKvittering) {
        messageTemplate.sendAndReceive(uri.baseUri.toString(), BekreftelseSender(confirmableReceipt, marshaller), EmptyReceiver())
    }

    companion object {
        fun fromMessageSender(messageSender: MessageSender): EbmsSender {
            // TODO: Hacky workaround, we should probably rather implement the required builder logic ourselves
            val marshallerField = MessageSender.DefaultMessageSender::class.java.getDeclaredField("marshaller").apply {
                isAccessible = true
            }
            val uriField = MessageSender.DefaultMessageSender::class.java.getDeclaredField("uri").apply {
                isAccessible = true
            }
            val meldingsTemplateField = MessageSender.DefaultMessageSender::class.java.getDeclaredField("meldingTemplate").apply {
                isAccessible = true
            }
            return EbmsSender(
                    marshaller = marshallerField.get(messageSender) as Jaxb2Marshaller,
                    uri = uriField.get(messageSender) as EbmsEndpointUriBuilder,
                    messageTemplate = meldingsTemplateField.get(messageSender) as WebServiceTemplate
            )
        }
    }
}

