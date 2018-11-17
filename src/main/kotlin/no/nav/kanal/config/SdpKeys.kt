package no.nav.kanal.config

import no.difi.sdp.client2.domain.Noekkelpar
import no.nav.kanal.config.model.VaultCredentials
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.io.FileInputStream
import java.security.KeyStore

@Service
class SdpKeys @Autowired constructor(
        @Value("\${no.nav.sdpkanal.keystore}") keystorePath: String,
        @Value("\${no.nav.sdpkanal.truststore}") truststorePath: String,
        credentials: VaultCredentials) {
    val keystore: KeyStore = KeyStore.getInstance("JKS").apply {
        load(FileInputStream(keystorePath), credentials.virksomhetKeystorePassword.toCharArray())
    }
    val truststore: KeyStore = KeyStore.getInstance("JKS").apply {
        load(FileInputStream(truststorePath), credentials.truststorePassword.toCharArray())
    }
    val keypair: Noekkelpar = Noekkelpar.fraKeyStoreUtenTrustStore(keystore, credentials.virksomhetKeystoreAlias, credentials.virksomhetKeystorePassword)
}
