package no.nav.kanal.config

import no.digipost.api.EbmsEndpointUriBuilder
import no.digipost.api.MessageSender
import no.digipost.api.interceptors.KeyStoreInfo
import no.digipost.api.representations.EbmsAktoer

import java.net.URI


const val MPC_ID: String = "MPC_ID"

fun createMessageSender(
        databehandler: EbmsAktoer,
        mottaker: EbmsAktoer,
        sdpKeys: SdpKeys,
        credentials: VaultCredentials,
        ebmsEndpoint: String
): MessageSender {
    val keystoreInfo = KeyStoreInfo(sdpKeys.keystore, sdpKeys.truststore, credentials.virksomhetKeystoreAlias, credentials.virksomhetKeystorePassword)
    val endpoint = EbmsEndpointUriBuilder.statiskUri(URI(ebmsEndpoint))
    return MessageSender.create(endpoint, keystoreInfo, databehandler, mottaker).build()
}
