package no.nav.kanal.config

import no.difi.sdp.client2.domain.Noekkelpar
import java.io.FileInputStream
import java.security.KeyStore
import java.util.Base64

class SdpKeys constructor(
        keystorePath: String,
        truststorePath: String,
        credentials: VaultCredentials) {
    val keystore: KeyStore = KeyStore.getInstance("JKS").apply {
        load(Base64.getDecoder().wrap(FileInputStream(keystorePath)), credentials.virksomhetKeystorePassword.toCharArray())
    }
    val truststore: KeyStore = KeyStore.getInstance("JKS").apply {
        load(Base64.getDecoder().wrap(FileInputStream(truststorePath)), credentials.truststorePassword.toCharArray())
    }
    val keypair: Noekkelpar = Noekkelpar.fraKeyStoreUtenTrustStore(keystore, credentials.virksomhetKeystoreAlias, credentials.virksomhetKeystorePassword)
}
