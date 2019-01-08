package no.nav.kanal.ebms

import no.digipost.api.EbmsEndpointUriBuilder
import no.digipost.api.PMode
import no.digipost.api.SdpMeldingSigner
import no.digipost.api.handlers.BekreftelseSender
import no.digipost.api.handlers.EmptyReceiver
import no.digipost.api.handlers.PullRequestSender
import no.digipost.api.handlers.TransportKvitteringReceiver
import no.digipost.api.representations.Dokumentpakke
import no.digipost.api.representations.EbmsAktoer
import no.digipost.api.representations.EbmsOutgoingMessage
import no.digipost.api.representations.EbmsPullRequest
import no.digipost.api.representations.KanBekreftesSomBehandletKvittering
import no.digipost.api.representations.TransportKvittering
import no.digipost.api.xml.Marshalling
import org.springframework.oxm.jaxb.Jaxb2Marshaller
import org.springframework.ws.client.core.WebServiceTemplate
import org.unece.cefact.namespaces.standardbusinessdocumentheader.StandardBusinessDocument
import no.digipost.api.exceptions.MessageSenderFaultMessageResolver
import no.digipost.api.handlers.EbmsContextAwareWebServiceTemplate
import no.digipost.api.interceptors.EbmsClientInterceptor
import no.digipost.api.interceptors.EbmsReferenceValidatorInterceptor
import no.digipost.api.interceptors.RemoveContentLengthInterceptor
import no.digipost.api.interceptors.TransactionLogClientInterceptor
import no.digipost.api.interceptors.WsSecurityInterceptor
import no.nav.kanal.config.SdpKeys
import org.apache.http.impl.client.HttpClientBuilder
import org.springframework.ws.soap.saaj.SaajSoapMessageFactory
import org.springframework.ws.transport.http.HttpComponentsMessageSender
import javax.xml.soap.MessageFactory
import javax.xml.soap.SOAPConstants

class EbmsSender(
        private val uri: EbmsEndpointUriBuilder,
        sdpKeys: SdpKeys,
        receiver: EbmsAktoer,
        private val jaxb2Marshaller: Jaxb2Marshaller = Marshalling.getMarshallerSingleton(),
        private val signer: SdpMeldingSigner = SdpMeldingSigner(sdpKeys.keypair.keyStoreInfo, jaxb2Marshaller)
) {
    private val clientInterceptors = arrayOf(
            EbmsClientInterceptor(jaxb2Marshaller, receiver),
            WsSecurityInterceptor(sdpKeys.keypair.keyStoreInfo, null).apply {
                afterPropertiesSet()
            },
            EbmsReferenceValidatorInterceptor(jaxb2Marshaller),
            TransactionLogClientInterceptor(jaxb2Marshaller).apply {
                setTransaksjonslogg(EbmsTransactionLogger())
            }
    )

    private val messageSender = HttpComponentsMessageSender(HttpClientBuilder.create().addInterceptorFirst(RemoveContentLengthInterceptor()).build())

    private val saajSoapMessageFactory: SaajSoapMessageFactory = SaajSoapMessageFactory(MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL)).apply {
        afterPropertiesSet()
    }

    private val messageTemplate: WebServiceTemplate = EbmsContextAwareWebServiceTemplate(saajSoapMessageFactory, receiver).apply {
        marshaller = jaxb2Marshaller
        unmarshaller = jaxb2Marshaller
        faultMessageResolver = MessageSenderFaultMessageResolver(jaxb2Marshaller)
        setMessageSender(messageSender)
        interceptors = clientInterceptors
    }

    fun fetchReceipt(
        ebmsPullRequest: EbmsPullRequest,
        previousConfirmableReceipt: KanBekreftesSomBehandletKvittering? = null
    ): EbmsReceipt? {
        val pullRequest = PullRequestSender(ebmsPullRequest, jaxb2Marshaller, previousConfirmableReceipt)
        return messageTemplate.sendAndReceive(uri.baseUri.toString(), pullRequest, EbmsReceiptExtractor())
    }

    fun confirmReceipt(confirmableReceipt: KanBekreftesSomBehandletKvittering) {
        messageTemplate.sendAndReceive(uri.baseUri.toString(), BekreftelseSender(confirmableReceipt, jaxb2Marshaller), EmptyReceiver())
    }

    fun send(
        dataHandler: EbmsAktoer,
        technicalReceiver: EbmsAktoer,
        sbd: StandardBusinessDocument,
        documentPackage: Dokumentpakke,
        priority: EbmsOutgoingMessage.Prioritet,
        mpcId: String,
        messageId: String,
        conversationId: String,
        action: PMode.Action
    ) : TransportKvittering {
        val outgoing = EbmsOutgoingSender(signer, dataHandler, technicalReceiver, sbd, documentPackage, priority, mpcId, messageId, conversationId, action, jaxb2Marshaller)
        return messageTemplate.sendAndReceive(uri.baseUri.toString(), outgoing, TransportKvitteringReceiver())
    }
}
